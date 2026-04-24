package io.altacod.publisher.channel.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelErrorSanitizer;
import io.altacod.publisher.channel.ChannelOutboundLogEntity;
import io.altacod.publisher.channel.ChannelOutboundLogRepository;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.channel.PostChannelTargetRepository;
import io.altacod.publisher.channel.WorkspaceChannelEntity;
import io.altacod.publisher.channel.WorkspaceChannelRepository;
import io.altacod.publisher.api.MediaService;
import io.altacod.publisher.config.OutboundRestClientConfig;
import io.altacod.publisher.config.PublisherChannelDeliveryProperties;
import io.altacod.publisher.config.PublisherPublicSiteProperties;
import io.altacod.publisher.media.MediaAssetEntity;
import io.altacod.publisher.media.PostMediaEntity;
import io.altacod.publisher.media.PostMediaRepository;
import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import io.altacod.publisher.text.HtmlPlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.net.http.HttpTimeoutException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChannelDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChannelDispatchService.class);

    private static final int MAX_MESSAGE_LEN = 3800;
    /** Текст сообщения в MAX Bot API (см. dev.max.ru). */
    private static final int MAX_MESSENGER_TEXT_LEN = 4000;
    /** Лимит подписи к фото/альбому в Telegram Bot API. */
    private static final int TELEGRAM_CAPTION_MAX = 1024;
    /** Превью тела статьи для ВК/ОК (символов plain text). */
    private static final int CHANNEL_BODY_PREVIEW_CHARS = 3200;
    private static final int MAX_ERR_DB = 2000;
    private static final String VK_API = "https://api.vk.com/method/wall.post";
    private static final String VK_GET = "https://api.vk.com/method/wall.getById";
    private static final String OK_API = "https://api.ok.ru/fb.do";
    private static final String MAX_MESSAGES_API = "https://platform-api.max.ru/messages";

    private final PostRepository postRepository;
    private final WorkspaceChannelRepository channelRepository;
    private final ChannelOutboundLogRepository outboundLogRepository;
    private final PostChannelTargetRepository postChannelTargetRepository;
    private final PublisherPublicSiteProperties publicSite;
    private final PublisherChannelDeliveryProperties deliveryProps;
    private final ObjectMapper objectMapper;
    private final RestClient http;
    private final PostMediaRepository postMediaRepository;
    private final MediaService mediaService;

    public ChannelDispatchService(
            PostRepository postRepository,
            WorkspaceChannelRepository channelRepository,
            ChannelOutboundLogRepository outboundLogRepository,
            PostChannelTargetRepository postChannelTargetRepository,
            PublisherPublicSiteProperties publicSite,
            PublisherChannelDeliveryProperties deliveryProps,
            ObjectMapper objectMapper,
            PostMediaRepository postMediaRepository,
            MediaService mediaService,
            @Qualifier(OutboundRestClientConfig.OUTBOUND_REST_CLIENT) RestClient outboundRestClient
    ) {
        this.postRepository = postRepository;
        this.channelRepository = channelRepository;
        this.outboundLogRepository = outboundLogRepository;
        this.postChannelTargetRepository = postChannelTargetRepository;
        this.publicSite = publicSite;
        this.deliveryProps = deliveryProps;
        this.objectMapper = objectMapper;
        this.postMediaRepository = postMediaRepository;
        this.mediaService = mediaService;
        this.http = outboundRestClient;
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
        if (!post.isSocialPublishEnabled()) {
            return;
        }
        if (post.isChannelSyndicationBlocked()) {
            return;
        }
        Long wsId = post.getWorkspace().getId();
        List<WorkspaceChannelEntity> channels =
                channelRepository.findByWorkspaceIdAndEnabledIsTrueOrderByChannelTypeAsc(wsId);
        Instant now = Instant.now();
        for (WorkspaceChannelEntity ch : channels) {
            try {
                if (!shouldDeliverToChannel(post, ch.getChannelType())) {
                    continue;
                }
                switch (ch.getChannelType()) {
                    case TELEGRAM -> sendTelegram(post, ch, now);
                    case VK -> sendVk(post, ch, now);
                    case ODNOKLASSNIKI -> sendOdnoklassniki(post, ch, now);
                    case MAX -> sendMax(post, ch, now);
                    default -> log.debug("Unsupported channel type: {}", ch.getChannelType());
                }
            } catch (Exception e) {
                log.warn(
                        "Channel {} publish failed for post {}: {}",
                        ch.getChannelType(),
                        postId,
                        ChannelErrorSanitizer.sanitizeThrowableChainForLog(e)
                );
                String errMsg = userFacingErrorMessage(e);
                if (isModerationRejection(errMsg)) {
                    recordModerationRejected(post, ch.getChannelType(), errMsg, now);
                } else {
                    recordFailureRetryable(post, ch.getChannelType(), errMsg, now);
                }
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
        if (!post.isSocialPublishEnabled()) {
            recordFailureTerminal(post, type, "Social publishing disabled for post; retries stopped", now);
            return;
        }
        if (post.isChannelSyndicationBlocked()) {
            return;
        }
        Long wsId = post.getWorkspace().getId();
        WorkspaceChannelEntity ch = channelRepository.findByWorkspaceIdAndChannelType(wsId, type).orElse(null);
        if (ch == null || !ch.isEnabled()) {
            recordFailureTerminal(post, type, "Channel disabled or not configured", now);
            return;
        }
        if (!shouldDeliverToChannel(post, type)) {
            recordFailureTerminal(post, type, "Channel not selected for this post; retries stopped", now);
            return;
        }
        try {
            switch (type) {
                case TELEGRAM -> sendTelegram(post, ch, now);
                case VK -> sendVk(post, ch, now);
                case ODNOKLASSNIKI -> sendOdnoklassniki(post, ch, now);
                case MAX -> sendMax(post, ch, now);
                default -> log.debug("Unsupported channel type: {}", type);
            }
        } catch (Exception e) {
            log.warn(
                    "Channel {} retry failed for post {}: {}",
                    type,
                    postId,
                    ChannelErrorSanitizer.sanitizeThrowableChainForLog(e)
            );
            String errMsg = userFacingErrorMessage(e);
            if (isModerationRejection(errMsg)) {
                recordModerationRejected(post, type, errMsg, now);
            } else {
                recordFailureRetryable(post, type, errMsg, now);
            }
        }
    }

    private boolean shouldDeliverToChannel(PostEntity post, ChannelType type) {
        List<io.altacod.publisher.channel.ChannelType> targets =
                postChannelTargetRepository.findChannelTypesByPostId(post.getId());
        if (targets.isEmpty()) {
            return true;
        }
        return targets.contains(type);
    }

    /**
     * Текст для UI/БД: приоритет у {@code getMessage()}, иначе эвристика по сетевым причинам.
     */
    private static String userFacingErrorMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return ChannelErrorSanitizer.stripSecrets(e.getMessage());
        }
        String hinted = describeAnyNetworkFailure(e);
        if (hinted != null) {
            return hinted;
        }
        return e.getClass().getSimpleName();
    }

    private static String describeAnyNetworkFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof ClosedChannelException) {
                return "Не удалось подключиться к внешнему API (исходящий HTTPS/443: фаервол, прокси, блокировка или нет маршрута)";
            }
            if (t instanceof UnknownHostException) {
                return "Не удаётся разрешить имя хоста (DNS). Проверьте сеть";
            }
            if (t instanceof HttpTimeoutException) {
                return "Таймаут сетевого запроса к внешнему API";
            }
        }
        return null;
    }

    private static String describeTelegramNetworkFailure(RestClientException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof ClosedChannelException) {
                return "не удалось установить соединение с api.telegram.org:443. "
                        + "С машины, где запущен backend, нужен исходящий HTTPS; проверьте фаервол, антивирус, "
                        + "корпоративный прокси (для Java: -Dhttps.proxyHost / -Dhttps.proxyPort) или доступность Telegram";
            }
            if (t instanceof UnknownHostException) {
                return "не удаётся разрешить имя api.telegram.org (DNS)";
            }
            if (t instanceof HttpTimeoutException) {
                return "таймаут при обращении к api.telegram.org";
            }
        }
        String m = e.getMessage();
        if (m == null || m.isBlank() || "null".equalsIgnoreCase(m.trim())) {
            return e.getClass().getSimpleName()
                    + " (сеть или api.telegram.org недоступен с сервера приложения)";
        }
        return ChannelErrorSanitizer.stripSecrets(m);
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
        List<TelegramImage> images = loadTelegramImages(post);
        String caption = buildTelegramCaption(post);
        if (images.isEmpty()) {
            String text = buildChannelMessage(post);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("disable_web_page_preview", true);

            String sendUrl = "https://api.telegram.org/bot" + token.trim() + "/sendMessage";
            String raw;
            try {
                raw = http.post()
                        .uri(sendUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .body(String.class);
            } catch (RestClientException e) {
                String detail = describeTelegramNetworkFailure(e);
                throw new IllegalStateException("Telegram HTTP: " + detail, e);
            }
            JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
            if (!root.path("ok").asBoolean(false)) {
                throw new IllegalStateException("Telegram API: " + truncateError(raw));
            }
        } else {
            sendTelegramImagesWithCaption(token, chatId, images, caption);
        }
        upsertSuccess(post, ChannelType.TELEGRAM, now, null, null);
    }

    /**
     * Текст подписи: заголовок + сокращённое тело + ссылка «Читать дальше» (в пределах лимита Telegram).
     */
    private String buildTelegramCaption(PostEntity post) {
        String title = post.getTitle() == null ? "" : post.getTitle().trim();
        String url = buildPublicUrl(post);
        String linkBlock = (url != null && !url.isBlank()) ? "\n\nЧитать дальше:\n" + url : "";
        int linkLen = linkBlock.length();
        int maxTotal = TELEGRAM_CAPTION_MAX;
        int headBudget = maxTotal - linkLen;
        if (headBudget < 40) {
            linkBlock = "";
            headBudget = maxTotal;
        }
        String bodyPlain = HtmlPlainText.toPlain(post.getBodyHtml(), Math.max(120, headBudget));
        StringBuilder head = new StringBuilder();
        if (!title.isEmpty()) {
            head.append(title);
        }
        if (bodyPlain != null && !bodyPlain.isBlank()) {
            if (head.length() > 0) {
                head.append("\n\n");
            }
            head.append(bodyPlain);
        }
        String core = head.toString();
        if (core.length() > headBudget) {
            core = HtmlPlainText.truncatePlain(core, headBudget);
        }
        String full = core + linkBlock;
        if (full.length() > maxTotal) {
            full = HtmlPlainText.truncatePlain(core, maxTotal - linkLen) + linkBlock;
            if (full.length() > maxTotal) {
                full = HtmlPlainText.truncatePlain(full, maxTotal);
            }
        }
        return full;
    }

    /**
     * Одно сообщение в канале: альбом (до 10 фото) с подписью на первом снимке; при &gt;10 — следующие партии без подписи.
     */
    private void sendTelegramImagesWithCaption(
            String token,
            String chatId,
            List<TelegramImage> images,
            String caption
    ) throws Exception {
        int i = 0;
        boolean firstBatch = true;
        while (i < images.size()) {
            int remaining = images.size() - i;
            String cap = firstBatch ? caption : null;
            firstBatch = false;
            if (remaining == 1) {
                sendTelegramPhoto(token, chatId, images.get(i), cap);
                i++;
            } else {
                int chunk = Math.min(10, remaining);
                sendTelegramMediaGroup(token, chatId, images.subList(i, i + chunk), cap);
                i += chunk;
            }
        }
    }

    private record TelegramImage(byte[] data, String mimeType) {
    }

    private List<TelegramImage> loadTelegramImages(PostEntity post) throws IOException {
        Long wsId = post.getWorkspace().getId();
        List<PostMediaEntity> rows = postMediaRepository.findByPostIdOrderBySortOrderAsc(post.getId());
        List<TelegramImage> out = new ArrayList<>();
        for (PostMediaEntity pm : rows) {
            MediaAssetEntity a = pm.getMediaAsset();
            String mime = a.getMimeType();
            if (mime == null || !mime.toLowerCase(Locale.ROOT).startsWith("image/")) {
                continue;
            }
            MediaService.MediaFileView v = mediaService.fileForWorkspace(wsId, a.getId());
            try (InputStream in = v.resource().getInputStream()) {
                out.add(new TelegramImage(in.readAllBytes(), mime));
            }
        }
        return out;
    }

    private void sendTelegramPhoto(String token, String chatId, TelegramImage img, String caption) throws Exception {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("chat_id", chatId);
        parts.add("photo", telegramPhotoResource(img));
        if (caption != null && !caption.isBlank()) {
            parts.add("caption", caption);
        }
        telegramPostMultipart(token, "sendPhoto", parts);
    }

    private void sendTelegramMediaGroup(String token, String chatId, List<TelegramImage> slice, String caption)
            throws Exception {
        if (slice.size() < 2) {
            throw new IllegalArgumentException("sendMediaGroup requires 2–10 photos");
        }
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("chat_id", chatId);
        ArrayNode media = objectMapper.createArrayNode();
        for (int i = 0; i < slice.size(); i++) {
            TelegramImage img = slice.get(i);
            ObjectNode item = objectMapper.createObjectNode();
            item.put("type", "photo");
            item.put("media", "attach://f" + i);
            if (i == 0 && caption != null && !caption.isBlank()) {
                item.put("caption", caption.length() > TELEGRAM_CAPTION_MAX
                        ? caption.substring(0, TELEGRAM_CAPTION_MAX - 1) + "…"
                        : caption);
            }
            media.add(item);
            parts.add("f" + i, telegramPhotoResource(img));
        }
        parts.add("media", objectMapper.writeValueAsString(media));
        telegramPostMultipart(token, "sendMediaGroup", parts);
    }

    private ByteArrayResource telegramPhotoResource(TelegramImage img) {
        String name = telegramImageFilename(img.mimeType());
        return new ByteArrayResource(img.data()) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    private static String telegramImageFilename(String mime) {
        if (mime == null) {
            return "photo.jpg";
        }
        String m = mime.toLowerCase(Locale.ROOT);
        if (m.contains("png")) {
            return "photo.png";
        }
        if (m.contains("gif")) {
            return "photo.gif";
        }
        if (m.contains("webp")) {
            return "photo.webp";
        }
        return "photo.jpg";
    }

    private void telegramPostMultipart(String token, String method, MultiValueMap<String, Object> parts)
            throws Exception {
        String url = "https://api.telegram.org/bot" + token.trim() + "/" + method;
        String raw;
        try {
            raw = http.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            String detail = describeTelegramNetworkFailure(e);
            throw new IllegalStateException("Telegram HTTP: " + detail, e);
        }
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram API: " + truncateError(raw));
        }
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
        String message = buildChannelMessage(post);
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
        int wallPostId = root.path("response").path("post_id").asInt(0);
        if (wallPostId <= 0) {
            throw new IllegalStateException("VK API: missing post_id in response");
        }
        String vkWallKey = "-" + groupId + "_" + wallPostId;
        String vkUrl = "https://vk.com/wall" + vkWallKey;
        upsertSuccess(post, ChannelType.VK, now, vkWallKey, vkUrl);
    }

    private void sendOdnoklassniki(PostEntity post, WorkspaceChannelEntity ch, Instant now) throws Exception {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), ChannelType.ODNOKLASSNIKI);
        if (shouldSkipDelivery(existing, now)) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String applicationKey = text(cfg, "applicationKey");
        String applicationSecretKey = text(cfg, "applicationSecretKey");
        String accessToken = text(cfg, "accessToken");
        String groupId = text(cfg, "groupId");
        if (applicationKey == null || applicationKey.isBlank()
                || applicationSecretKey == null || applicationSecretKey.isBlank()
                || accessToken == null || accessToken.isBlank()
                || groupId == null || groupId.isBlank()) {
            recordFailureTerminal(
                    post,
                    ChannelType.ODNOKLASSNIKI,
                    "Missing applicationKey, applicationSecretKey, accessToken or groupId in channel config",
                    now
            );
            return;
        }
        String message = buildChannelMessage(post);
        String attachmentJson = buildOkAttachment(message, buildPublicUrl(post));
        String sessionSecret = md5Hex(accessToken.trim() + applicationSecretKey.trim());

        TreeMap<String, String> params = new TreeMap<>();
        params.put("application_key", applicationKey.trim());
        params.put("format", "json");
        params.put("gid", groupId.trim());
        params.put("method", "mediatopic.post");
        params.put("attachment", attachmentJson);
        params.put("access_token", accessToken.trim());
        String sig = okSignature(params, sessionSecret);
        params.put("sig", sig);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        String raw;
        try {
            raw = http.post()
                    .uri(OK_API)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("OK HTTP: " + e.getMessage(), e);
        }
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (root.has("error_code")) {
            throw new IllegalStateException("OK API: " + root.toString());
        }
        String topicId = root.path("topic_id").asText(null);
        if (topicId == null || topicId.isBlank()) {
            topicId = root.path("id").asText(null);
        }
        if (topicId == null || topicId.isBlank()) {
            throw new IllegalStateException("OK API: missing topic id in response: " + truncateError(raw));
        }
        String topicUrl = "https://ok.ru/group/" + groupId.trim() + "/topic/" + topicId;
        upsertSuccess(post, ChannelType.ODNOKLASSNIKI, now, topicId, topicUrl);
    }

    private void sendMax(PostEntity post, WorkspaceChannelEntity ch, Instant now) throws Exception {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), ChannelType.MAX);
        if (shouldSkipDelivery(existing, now)) {
            return;
        }
        JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
        String accessToken = text(cfg, "accessToken");
        String chatIdRaw = text(cfg, "chatId");
        if (accessToken == null || accessToken.isBlank() || chatIdRaw == null || chatIdRaw.isBlank()) {
            recordFailureTerminal(
                    post,
                    ChannelType.MAX,
                    "Missing accessToken or chatId in channel config",
                    now
            );
            return;
        }
        long chatId;
        try {
            chatId = Long.parseLong(chatIdRaw.trim());
        } catch (NumberFormatException e) {
            recordFailureTerminal(post, ChannelType.MAX, "Invalid chatId (expected integer)", now);
            return;
        }
        String message = truncatePlain(buildChannelMessage(post), MAX_MESSENGER_TEXT_LEN);
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("text", message);
        // См. dev.max.ru: при false не генерируется превью ссылок (аналог «без сниппета» у Telegram).
        requestBody.put("disable_link_preview", false);

        String uri = UriComponentsBuilder.fromUriString(MAX_MESSAGES_API)
                .queryParam("chat_id", chatId)
                .build(true)
                .toUriString();
        String authHeader = authorizationHeaderForMax(accessToken);
        String raw;
        try {
            raw = http.post()
                    .uri(uri)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new IllegalStateException(
                    "MAX API: HTTP " + e.getStatusCode().value() + " " + truncateError(body), e
            );
        } catch (RestClientException e) {
            String detail = describeMaxNetworkFailure(e);
            throw new IllegalStateException("MAX HTTP: " + detail, e);
        }
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (root.has("error")) {
            throw new IllegalStateException("MAX API: " + truncateError(root.path("error").toString()));
        }
        if (!root.path("message").isObject()) {
            throw new IllegalStateException("MAX API: missing message in response: " + truncateError(raw));
        }
        long mid = root.path("message").path("id").asLong(0L);
        String extId = mid > 0 ? Long.toString(mid) : null;
        upsertSuccess(post, ChannelType.MAX, now, extId, null);
    }

    /**
     * Официальная документация: заголовок {@code Authorization} с токеном бота; в примерах часто используется префикс
     * {@code Bearer } (как в OpenAPI-совместимых клиентах).
     */
    private static String authorizationHeaderForMax(String accessToken) {
        String t = accessToken.trim();
        if (t.regionMatches(true, 0, "bearer ", 0, 7)) {
            return t;
        }
        return "Bearer " + t;
    }

    private static String describeMaxNetworkFailure(RestClientException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof ClosedChannelException) {
                return "не удалось установить соединение с platform-api.max.ru:443. Проверьте исходящий HTTPS";
            }
            if (t instanceof UnknownHostException) {
                return "не удаётся разрешить имя platform-api.max.ru (DNS)";
            }
            if (t instanceof HttpTimeoutException) {
                return "таймаут при обращении к platform-api.max.ru";
            }
        }
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName()
                : ChannelErrorSanitizer.stripSecrets(e.getMessage());
    }

    private String buildOkAttachment(String message, String link) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode media = objectMapper.createArrayNode();
            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", message);
            media.add(text);
            if (link != null && !link.isBlank()) {
                ObjectNode l = objectMapper.createObjectNode();
                l.put("type", "link");
                l.put("url", link);
                media.add(l);
            }
            root.set("media", media);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String okSignature(TreeMap<String, String> paramsIncludingSigCandidate, String sessionSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(paramsIncludingSigCandidate);
        sorted.remove("sig");
        StringBuilder concat = new StringBuilder();
        for (var e : sorted.entrySet()) {
            concat.append(e.getKey()).append("=").append(e.getValue());
        }
        concat.append(sessionSecret);
        return md5Hex(concat.toString());
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        if (row.getStatus() == ChannelDeliveryStatus.REJECTED) {
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

    /**
     * Эвристика по тексту ошибки API (ВК и др.): модерация, спам-фильтр, запрет публикации.
     */
    private static boolean isModerationRejection(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("moderat") || m.contains("модерац")) {
            return true;
        }
        if (m.contains("\"error_code\":214") || m.contains("error_code\": 214") || m.contains("error_code\":214")) {
            return true;
        }
        if (m.contains("spam") && (m.contains("vk") || m.contains("wall"))) {
            return true;
        }
        if (m.contains("content rejected") || m.contains("заблокирован") || m.contains("blocked")) {
            return true;
        }
        return false;
    }

    private void recordModerationRejected(PostEntity post, ChannelType type, String error, Instant now) {
        String err = truncateError(error);
        Optional<ChannelOutboundLogEntity> opt =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        ChannelOutboundLogEntity row = opt.orElseGet(() ->
                new ChannelOutboundLogEntity(post, type, ChannelDeliveryStatus.REJECTED, err, now));
        row.markRejected(err, now);
        outboundLogRepository.save(row);
    }

    private void upsertSuccess(
            PostEntity post,
            ChannelType type,
            Instant now,
            String externalId,
            String externalUrl
    ) {
        Optional<ChannelOutboundLogEntity> existing =
                outboundLogRepository.findByPost_IdAndChannelType(post.getId(), type);
        if (existing.isEmpty()) {
            var row = new ChannelOutboundLogEntity(post, type, ChannelDeliveryStatus.SENT, null, now);
            row.markSent(now, externalId, externalUrl);
            outboundLogRepository.save(row);
        } else {
            ChannelOutboundLogEntity e = existing.get();
            e.markSent(now, externalId, externalUrl);
            outboundLogRepository.save(e);
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

    private long computeBackoffSeconds(int failedAttemptNumber) {
        int base = Math.max(5, deliveryProps.getBaseDelaySeconds());
        int cap = Math.max(base, deliveryProps.getMaxDelaySeconds());
        int jitterPct = Math.min(50, Math.max(0, deliveryProps.getJitterPercent()));
        double exp = Math.min(cap, base * Math.pow(2, Math.max(0, failedAttemptNumber - 1)));
        double jitter = exp * (ThreadLocalRandom.current().nextDouble(-jitterPct, jitterPct) / 100.0);
        long total = Math.round(exp + jitter);
        return Math.min(cap, Math.max(1, total));
    }

    /** Текст для соцканалов: без краткого описания — заголовок, сокращённая версия статьи, ссылка. */
    private String buildChannelMessage(PostEntity post) {
        String title = post.getTitle() == null ? "" : post.getTitle().trim();
        String preview = HtmlPlainText.toPlain(post.getBodyHtml(), CHANNEL_BODY_PREVIEW_CHARS);
        String link = buildPublicUrl(post);
        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) {
            sb.append(title);
        }
        if (preview != null && !preview.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(preview);
        }
        if (link != null && !link.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Читать дальше: ").append(link);
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
        String cleaned = ChannelErrorSanitizer.stripSecrets(s);
        if (cleaned.length() <= MAX_ERR_DB) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_ERR_DB - 1) + "…";
    }

    private static String text(JsonNode cfg, String key) {
        if (cfg == null || !cfg.has(key) || cfg.get(key).isNull()) {
            return null;
        }
        return cfg.get(key).asText();
    }

    /**
     * Обновление метрик ВКонтакте по сохранённому идентификатору поста на стене ({@code externalId} = {@code -groupId_postId}).
     */
    public void refreshVkMetrics(ChannelOutboundLogEntity log, String accessToken) throws Exception {
        String postsKey = log.getExternalId();
        if (postsKey == null || postsKey.isBlank() || accessToken == null || accessToken.isBlank()) {
            return;
        }
        String uri = UriComponentsBuilder.fromUriString(VK_GET)
                .queryParam("v", "5.199")
                .queryParam("access_token", accessToken.trim())
                .queryParam("posts", postsKey)
                .build(true)
                .toUriString();
        String raw = http.get().uri(uri).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
        if (root.has("error")) {
            throw new IllegalStateException("VK metrics: " + root.path("error").toString());
        }
        JsonNode items = root.path("response");
        if (!items.isArray() || items.isEmpty()) {
            return;
        }
        JsonNode w = items.get(0);
        long likes = w.path("likes").path("count").asLong(0);
        long reposts = w.path("reposts").path("count").asLong(0);
        long views = w.path("views").path("count").asLong(0);
        if (views == 0) {
            views = w.path("views").asLong(0);
        }
        long comments = w.path("comments").path("count").asLong(0);
        ObjectNode m = objectMapper.createObjectNode();
        m.put("likes", likes);
        m.put("reposts", reposts);
        m.put("views", views);
        m.put("comments", comments);
        m.put("shares", reposts);
        Instant now = Instant.now();
        log.setMetricsSnapshot(objectMapper.writeValueAsString(m), now);
        outboundLogRepository.save(log);
    }
}
