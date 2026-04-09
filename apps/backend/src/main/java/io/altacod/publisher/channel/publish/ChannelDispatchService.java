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
import io.altacod.publisher.config.PublisherChannelDeliveryProperties;
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
import java.util.concurrent.ThreadLocalRandom;

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
    private final PublisherChannelDeliveryProperties deliveryProps;
    private final ObjectMapper objectMapper;
    private final RestClient http = RestClient.create();

    public ChannelDispatchService(
            PostRepository postRepository,
            WorkspaceChannelRepository channelRepository,
            ChannelOutboundLogRepository outboundLogRepository,
            PublisherPublicSiteProperties publicSite,
            PublisherChannelDeliveryProperties deliveryProps,
            ObjectMapper objectMapper
    ) {
        this.postRepository = postRepository;
        this.channelRepository = channelRepository;
        this.outboundLogRepository = outboundLogRepository;
        this.publicSite = publicSite;
        this.deliveryProps = deliveryProps;
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
                recordFailureRetryable(post, ch.getChannelType(), e.getMessage(), now);
            }
        }
    }

    /**
     * Повторная доставка одного канала (из планировщика по {@code next_retry_at}).
     */
    @Transactional
    public void retryOne(long postId, ChannelType type) {
        PostEntity post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        Instant now = Instant.now();
        if (post.getStatus() != PostStatus.PUBLISHED || post.getVisibility() != PostVisibility.PUBLIC) {
            recordFailureTerminal(post, type, "Post no longer published as PUBLIC; retries stopped", now);
            return;
        }
        Long wsId = post.getWorkspace().getId();
        WorkspaceChannelEntity ch = channelRepository.findByWorkspaceIdAndChannelType(wsId, type).orElse(null);
        if (ch == null || !ch.isEnabled()) {
            recordFailureTerminal(post, type, "Channel disabled or not configured", now);
            return;
        }
        try {
            switch (type) {
                case TELEGRAM -> sendTelegram(post, ch, now);
                case VK -> sendVk(post, ch, now);
                default -> log.debug("Unsupported channel type: {}", type);
            }
        } catch (Exception e) {
            log.warn("Channel {} retry failed for post {}", type, postId, e);
            recordFailureRetryable(post, type, e.getMessage(), now);
        }
    }

    private void sendTelegram(PostEntity post, WorkspaceChannelEntity ch, Instant now) throws Exception {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), ChannelType.TELEGRAM);
        if (shouldSkipDelivery(existing, now)) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String token = text(cfg, "botToken");
        String chatId = text(cfg, "chatId");
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            recordFailureTerminal(post, ChannelType.TELEGRAM, "Missing botToken or chatId in channel config", now);
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
        if (shouldSkipDelivery(existing, now)) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String accessToken = text(cfg, "accessToken");
        String groupIdRaw = text(cfg, "groupId");
        if (accessToken == null || accessToken.isBlank() || groupIdRaw == null || groupIdRaw.isBlank()) {
            recordFailureTerminal(post, ChannelType.VK, "Missing accessToken or groupId in channel config", now);
            return;
        }
        long groupId;
        try {
            groupId = Long.parseLong(groupIdRaw.trim());
        } catch (NumberFormatException e) {
            recordFailureTerminal(post, ChannelType.VK, "Invalid groupId (expected numeric)", now);
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

    /**
     * @return {@code true}, если уже доставлено, окончательно упало или ждём следующего слота ретрая.
     */
    private boolean shouldSkipDelivery(Optional<ChannelOutboundLogEntity> existing, Instant now) {
        if (existing.isEmpty()) {
            return false;
        }
        ChannelOutboundLogEntity row = existing.get();
        if (row.getStatus() == ChannelDeliveryStatus.SENT) {
            return true;
        }
        if (row.getStatus() == ChannelDeliveryStatus.FAILED && !row.isRetryable()) {
            return true;
        }
        return row.getStatus() == ChannelDeliveryStatus.FAILED
                && row.isRetryable()
                && row.getNextRetryAt() != null
                && row.getNextRetryAt().isAfter(now);
    }

    private void upsertSuccess(PostEntity post, ChannelType type, Instant now) {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        if (existing.isEmpty()) {
            outboundLogRepository.save(
                    new ChannelOutboundLogEntity(post, type, ChannelDeliveryStatus.SENT, null, now));
        } else {
            ChannelOutboundLogEntity e = existing.get();
            e.markSent(now);
        }
    }

    private void recordFailureRetryable(PostEntity post, ChannelType type, String error, Instant now) {
        String err = truncateError(error);
        Optional<ChannelOutboundLogEntity> opt =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        int prev = opt.map(ChannelOutboundLogEntity::getAttemptCount).orElse(0);
        int nextAttempt = prev + 1;
        int max = Math.max(1, deliveryProps.getMaxAttempts());
        ChannelOutboundLogEntity row = findOrCreateFailedRow(post, type, err, now, opt);
        if (nextAttempt >= max) {
            row.markTerminalFailure(err + " (max attempts " + max + ")", now);
        } else {
            long delaySec = computeBackoffSeconds(nextAttempt);
            Instant nextAt = now.plusSeconds(delaySec);
            row.markRetryableFailure(err, now, nextAttempt, nextAt);
        }
        outboundLogRepository.save(row);
    }

    private void recordFailureTerminal(PostEntity post, ChannelType type, String error, Instant now) {
        String err = truncateError(error);
        Optional<ChannelOutboundLogEntity> opt =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        ChannelOutboundLogEntity row = findOrCreateFailedRow(post, type, err, now, opt);
        row.markTerminalFailure(err, now);
        outboundLogRepository.save(row);
    }

    private static ChannelOutboundLogEntity findOrCreateFailedRow(
            PostEntity post,
            ChannelType type,
            String err,
            Instant now,
            Optional<ChannelOutboundLogEntity> opt
    ) {
        return opt.orElseGet(() -> new ChannelOutboundLogEntity(
                post, type, ChannelDeliveryStatus.FAILED, err, now));
    }

    /**
     * Экспоненциальная задержка от номера неудачной попытки (1 = первая ошибка), с джиттером и потолком.
     */
    private long computeBackoffSeconds(int failedAttemptNumber) {
        int base = Math.max(5, deliveryProps.getBaseDelaySeconds());
        int cap = Math.max(base, deliveryProps.getMaxDelaySeconds());
        int jitterPct = Math.min(50, Math.max(0, deliveryProps.getJitterPercent()));
        double exp = Math.min(cap, base * Math.pow(2, Math.max(0, failedAttemptNumber - 1)));
        double jitter = exp * (ThreadLocalRandom.current().nextDouble(-jitterPct, jitterPct) / 100.0);
        long total = Math.round(exp + jitter);
        return Math.min(cap, Math.max(1, total));
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
