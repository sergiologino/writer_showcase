package io.altacod.publisher.channel.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelOutboundLogEntity;
import io.altacod.publisher.channel.ChannelOutboundLogRepository;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.channel.WorkspaceChannelEntity;
import io.altacod.publisher.channel.WorkspaceChannelRepository;
import io.altacod.publisher.config.PublisherPublicSiteProperties;
import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChannelDispatchService.class);

    private static final int MAX_MESSAGE_LEN = 3800;
    private static final int MAX_ERR_DB = 2000;
    private static final String VK_API = "https://api.vk.com/method/wall.post";

    private final PostRepository postRepository;
    private final WorkspaceChannelRepository channelRepository;
    private final ChannelOutboundLogRepository outboundLogRepository;
    private final PublisherPublicSiteProperties publicSite;
    private final ObjectMapper objectMapper;
    private final RestClient http = RestClient.create();

    public ChannelDispatchService(
            PostRepository postRepository,
            WorkspaceChannelRepository channelRepository,
            ChannelOutboundLogRepository outboundLogRepository,
            PublisherPublicSiteProperties publicSite,
            ObjectMapper objectMapper
    ) {
        this.postRepository = postRepository;
        this.channelRepository = channelRepository;
        this.outboundLogRepository = outboundLogRepository;
        this.publicSite = publicSite;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(long postId) {
        PostEntity post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        if (post.getStatus() != PostStatus.PUBLISHED || post.getVisibility() != PostVisibility.PUBLIC) {
            return;
        }
        Long wsId = post.getWorkspace().getId();
        List<WorkspaceChannelEntity> channels =
                channelRepository.findByWorkspaceIdAndEnabledIsTrueOrderByChannelTypeAsc(wsId);
        Instant now = Instant.now();
        for (WorkspaceChannelEntity ch : channels) {
            try {
                switch (ch.getChannelType()) {
                    case TELEGRAM -> sendTelegram(post, ch, now);
                    case VK -> sendVk(post, ch, now);
                    default -> log.debug("Unsupported channel type: {}", ch.getChannelType());
                }
            } catch (Exception e) {
                log.warn("Channel {} publish failed for post {}", ch.getChannelType(), postId, e);
                upsertFailure(post, ch.getChannelType(), e.getMessage(), now);
            }
        }
    }

    private void sendTelegram(PostEntity post, WorkspaceChannelEntity ch, Instant now) throws Exception {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), ChannelType.TELEGRAM);
        if (existing.isPresent() && existing.get().getStatus() == ChannelDeliveryStatus.SENT) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String token = text(cfg, "botToken");
        String chatId = text(cfg, "chatId");
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            upsertFailure(post, ChannelType.TELEGRAM, "Missing botToken or chatId in channel config", now);
            return;
        }
        String text = buildMessage(post);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("disable_web_page_preview", true);

        String url = "https://api.telegram.org/bot" + token.trim() + "/sendMessage";
        String raw;
        try {
            raw = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Telegram HTTP: " + e.getMessage(), e);
        }
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram API: " + truncateError(raw));
        }
        upsertSuccess(post, ChannelType.TELEGRAM, now);
    }

    private void sendVk(PostEntity post, WorkspaceChannelEntity ch, Instant now) throws Exception {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), ChannelType.VK);
        if (existing.isPresent() && existing.get().getStatus() == ChannelDeliveryStatus.SENT) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String accessToken = text(cfg, "accessToken");
        String groupIdRaw = text(cfg, "groupId");
        if (accessToken == null || accessToken.isBlank() || groupIdRaw == null || groupIdRaw.isBlank()) {
            upsertFailure(post, ChannelType.VK, "Missing accessToken or groupId in channel config", now);
            return;
        }
        long groupId;
        try {
            groupId = Long.parseLong(groupIdRaw.trim());
        } catch (NumberFormatException e) {
            upsertFailure(post, ChannelType.VK, "Invalid groupId (expected numeric)", now);
            return;
        }
        String message = buildMessage(post);
        String uri = UriComponentsBuilder.fromUriString(VK_API)
                .queryParam("v", "5.199")
                .queryParam("access_token", accessToken.trim())
                .queryParam("owner_id", -groupId)
                .queryParam("from_group", 1)
                .queryParam("message", message)
                .build(true)
                .toUriString();

        String raw;
        try {
            raw = http.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("VK HTTP: " + e.getMessage(), e);
        }
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (root.has("error")) {
            throw new IllegalStateException("VK API: " + root.path("error").toString());
        }
        upsertSuccess(post, ChannelType.VK, now);
    }

    private void upsertSuccess(PostEntity post, ChannelType type, Instant now) {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        if (existing.isEmpty()) {
            outboundLogRepository.save(new ChannelOutboundLogEntity(
                    post, type, ChannelDeliveryStatus.SENT, null, now));
        } else {
            ChannelOutboundLogEntity e = existing.get();
            e.markSent(now);
        }
    }

    private void upsertFailure(PostEntity post, ChannelType type, String error, Instant now) {
        String err = truncateError(error);
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        if (existing.isEmpty()) {
            outboundLogRepository.save(new ChannelOutboundLogEntity(
                    post, type, ChannelDeliveryStatus.FAILED, err, now));
        } else {
            existing.get().markFailed(err, now);
        }
    }

    private String buildMessage(PostEntity post) {
        String title = post.getTitle() == null ? "" : post.getTitle().trim();
        String excerpt = stripHtml(post.getExcerpt());
        String link = buildPublicUrl(post);
        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) {
            sb.append(title);
        }
        if (excerpt != null && !excerpt.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(truncatePlain(excerpt, 1200));
        }
        if (link != null && !link.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(link);
        }
        return truncatePlain(sb.toString(), MAX_MESSAGE_LEN);
    }

    private String buildPublicUrl(PostEntity post) {
        if (!publicSite.hasBaseUrl()) {
            return null;
        }
        String base = publicSite.getBaseUrl().replaceAll("/+$", "");
        String ws = post.getWorkspace().getSlug();
        return base + "/blog/" + ws + "/p/" + post.getSlug();
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
    }

    private static String truncatePlain(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    private static String truncateError(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= MAX_ERR_DB) {
            return s;
        }
        return s.substring(0, MAX_ERR_DB - 1) + "…";
    }

    private static String text(JsonNode cfg, String key) {
        if (cfg == null || !cfg.has(key) || cfg.get(key).isNull()) {
            return null;
        }
        return cfg.get(key).asText();
    }
}
