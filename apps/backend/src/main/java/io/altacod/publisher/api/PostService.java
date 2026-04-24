package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.PostMediaAttachmentDto;
import io.altacod.publisher.api.dto.PostOutboundInfoDto;
import io.altacod.publisher.api.dto.PostPayload;
import io.altacod.publisher.api.dto.PostResponse;
import io.altacod.publisher.api.dto.TagSummaryDto;
import io.altacod.publisher.channel.ChannelErrorSanitizer;
import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelOutboundLogEntity;
import io.altacod.publisher.channel.ChannelOutboundLogRepository;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.channel.PostChannelTargetEntity;
import io.altacod.publisher.channel.PostChannelTargetRepository;
import io.altacod.publisher.channel.WorkspaceChannelEntity;
import io.altacod.publisher.channel.WorkspaceChannelRepository;
import io.altacod.publisher.media.MediaAssetEntity;
import io.altacod.publisher.media.MediaAssetRepository;
import io.altacod.publisher.media.PostMediaEntity;
import io.altacod.publisher.media.PostMediaRepository;
import io.altacod.publisher.category.CategoryEntity;
import io.altacod.publisher.category.CategoryRepository;
import io.altacod.publisher.common.Slugify;
import io.altacod.publisher.channel.publish.PostPublishedEvent;
import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import io.altacod.publisher.tag.TagEntity;
import io.altacod.publisher.tag.TagRepository;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.WorkspaceEntity;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostMediaRepository postMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PostChannelTargetRepository postChannelTargetRepository;
    private final ChannelOutboundLogRepository channelOutboundLogRepository;
    private final WorkspaceChannelRepository workspaceChannelRepository;
    private final ObjectMapper objectMapper;

    public PostService(
            PostRepository postRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            PostMediaRepository postMediaRepository,
            MediaAssetRepository mediaAssetRepository,
            ApplicationEventPublisher eventPublisher,
            PostChannelTargetRepository postChannelTargetRepository,
            ChannelOutboundLogRepository channelOutboundLogRepository,
            WorkspaceChannelRepository workspaceChannelRepository,
            ObjectMapper objectMapper
    ) {
        this.postRepository = postRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.postMediaRepository = postMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.eventPublisher = eventPublisher;
        this.postChannelTargetRepository = postChannelTargetRepository;
        this.channelOutboundLogRepository = channelOutboundLogRepository;
        this.workspaceChannelRepository = workspaceChannelRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PostResponse create(Long workspaceId, Long authorId, PostPayload payload) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String slug = resolveSlug(workspaceId, payload.title(), payload.slug(), null);
        Instant now = Instant.now();
        Instant publishedAt = payload.status() == PostStatus.PUBLISHED ? now : null;

        var post = new PostEntity(
                workspace,
                author,
                payload.title().trim(),
                slug,
                payload.excerpt(),
                payload.bodySource(),
                payload.bodyHtml(),
                payload.visibility(),
                payload.status(),
                Boolean.TRUE.equals(payload.aiGenerated()),
                now
        );
        if (publishedAt != null) {
            post.setPublishedAt(publishedAt);
        }
        if (payload.scheduledPublishAt() != null) {
            post.setScheduledPublishAt(payload.scheduledPublishAt());
        }
        applyScheduleStateOnCreate(post, payload, now);
        applyCategory(post, workspaceId, payload.categoryId());
        applyTags(post, workspaceId, payload.tagIds());

        postRepository.save(post);
        applyPostMedia(post, workspaceId, payload.mediaAssetIds());
        applySocialAndTargets(post, payload, true);
        if (payload.status() == PostStatus.PUBLISHED
                && payload.visibility() == PostVisibility.PUBLIC
                && !post.isChannelSyndicationBlocked()) {
            eventPublisher.publishEvent(new PostPublishedEvent(post.getId()));
        }
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> list(Long workspaceId, PostStatus status, String q, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        String qPattern = query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        Page<PostEntity> page = postRepository.findWorkspaceFeed(workspaceId, status, qPattern, sorted);
        List<Long> ids = page.getContent().stream().map(PostEntity::getId).toList();
        Map<Long, List<ChannelType>> targetsByPost = loadTargetsByPostIds(ids);
        Map<Long, List<ChannelOutboundLogEntity>> logsByPost = loadLogsByPostIds(ids);
        return page.map(p -> toResponse(
                p,
                targetsByPost.getOrDefault(p.getId(), List.of()),
                logsByPost.getOrDefault(p.getId(), List.of())
        ));
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long workspaceId, Long id) {
        PostEntity post = postRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return toResponse(post);
    }

    /**
     * Добавляет к накопительному счётчику токенов по статье (успешный вызов AI).
     */
    @Transactional
    public long addAccumulatedAiTokens(Long workspaceId, Long postId, int deltaTokens) {
        if (deltaTokens <= 0) {
            PostEntity p = postRepository.findByIdAndWorkspaceId(postId, workspaceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
            return p.getAiTokensTotal();
        }
        PostEntity post = postRepository.findByIdAndWorkspaceId(postId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        long next = post.getAiTokensTotal() + (long) deltaTokens;
        if (next < 0) {
            next = 0;
        }
        post.setAiTokensTotal(next);
        post.setUpdatedAt(Instant.now());
        return next;
    }

    @Transactional
    public PostResponse update(Long workspaceId, Long id, PostPayload payload) {
        PostEntity post = postRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        PostStatus previous = post.getStatus();
        String prevTitle = post.getTitle();
        String prevExcerpt = post.getExcerpt();
        String prevSource = post.getBodySource();
        String prevHtml = post.getBodyHtml();
        String slug = resolveSlug(workspaceId, payload.title(), payload.slug(), id);
        Instant now = Instant.now();
        Instant publishedAt = post.getPublishedAt();
        if (payload.status() == PostStatus.PUBLISHED) {
            if (publishedAt == null) {
                publishedAt = now;
            }
        } else if (previous == PostStatus.PUBLISHED && payload.status() != PostStatus.PUBLISHED) {
            publishedAt = null;
        }

        post.updateContent(
                payload.title().trim(),
                slug,
                payload.excerpt(),
                payload.bodySource(),
                payload.bodyHtml(),
                payload.visibility(),
                payload.status(),
                now,
                publishedAt
        );
        if (payload.scheduledPublishAt() != null) {
            post.setScheduledPublishAt(payload.scheduledPublishAt());
        }
        applyScheduleStateOnUpdate(
                post,
                payload,
                now,
                prevTitle,
                prevExcerpt,
                prevSource,
                prevHtml
        );
        post.replaceTags(Set.of());
        applyCategory(post, workspaceId, payload.categoryId());
        applyTags(post, workspaceId, payload.tagIds());
        applyPostMedia(post, workspaceId, payload.mediaAssetIds());
        applySocialAndTargets(post, payload, false);

        boolean nowPublishedPublic =
                payload.status() == PostStatus.PUBLISHED && payload.visibility() == PostVisibility.PUBLIC;
        if (nowPublishedPublic && !post.isChannelSyndicationBlocked()) {
            /*
             * Ставим в очередь диспетчера каналов при любом сохранении публичного опубликованного поста:
             * — первый переход в PUBLISHED+PUBLIC;
             * — повторное сохранение после подключения канала / токенов (раньше событие было только на переходе,
             *   из‑за чего новый Telegram оставался в PENDING без попытки доставки);
             * — ретраи для каналов без успешного лога. Уже доставленные (SENT) в dispatch пропускаются.
             */
            eventPublisher.publishEvent(new PostPublishedEvent(post.getId()));
        }

        return toResponse(post);
    }

    private void applyScheduleStateOnCreate(PostEntity post, PostPayload payload, Instant now) {
        Instant sched = payload.scheduledPublishAt();
        if (sched == null) {
            post.setScheduleMissed(false);
            post.setLateScheduleReleased(true);
            return;
        }
        if (sched.isBefore(now)) {
            post.setScheduleMissed(true);
            post.setLateScheduleReleased(payload.status() == PostStatus.PUBLISHED);
        } else {
            post.setScheduleMissed(false);
            post.setLateScheduleReleased(true);
        }
    }

    private void applyScheduleStateOnUpdate(
            PostEntity post,
            PostPayload payload,
            Instant now,
            String prevTitle,
            String prevExcerpt,
            String prevSource,
            String prevHtml
    ) {
        if (payload.scheduledPublishAt() != null) {
            post.setScheduledPublishAt(payload.scheduledPublishAt());
        }
        Instant sched = post.getScheduledPublishAt();
        if (sched != null && !sched.isBefore(now)) {
            post.setScheduleMissed(false);
            post.setLateScheduleReleased(true);
            return;
        }
        if (sched != null && sched.isBefore(now) && !post.isScheduleMissed()) {
            post.setScheduleMissed(true);
            post.setLateScheduleReleased(false);
        }
        if (post.isScheduleMissed() && !post.isLateScheduleReleased()) {
            if (contentChanged(prevTitle, prevExcerpt, prevSource, prevHtml, payload)) {
                post.setLateScheduleReleased(true);
            }
        }
    }

    private static boolean contentChanged(
            String prevTitle,
            String prevExcerpt,
            String prevSource,
            String prevHtml,
            PostPayload payload
    ) {
        return !strEq(prevTitle, payload.title().trim())
                || !strEq(prevExcerpt, payload.excerpt())
                || !strEq(prevSource, payload.bodySource())
                || !strEq(prevHtml, payload.bodyHtml());
    }

    private static boolean strEq(String a, String b) {
        if (a == null) {
            return b == null || b.isEmpty();
        }
        return a.equals(b == null ? "" : b);
    }

    @Transactional
    public void delete(Long workspaceId, Long id) {
        PostEntity post = postRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        postRepository.delete(post);
    }

    private void applyCategory(PostEntity post, Long workspaceId, Long categoryId) {
        if (categoryId == null) {
            post.setCategory(null);
            return;
        }
        CategoryEntity category = categoryRepository.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        post.setCategory(category);
    }

    private void applyPostMedia(PostEntity post, Long workspaceId, List<Long> mediaAssetIds) {
        if (mediaAssetIds == null) {
            return;
        }
        Long postId = post.getId();
        postMediaRepository.deleteByPostId(postId);
        if (mediaAssetIds.isEmpty()) {
            return;
        }
        List<Long> ordered = new ArrayList<>(new LinkedHashSet<>(mediaAssetIds));
        List<MediaAssetEntity> assets = mediaAssetRepository.findByWorkspaceIdAndIdIn(workspaceId, ordered);
        if (assets.size() != ordered.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media assets");
        }
        Map<Long, MediaAssetEntity> byId = assets.stream().collect(Collectors.toMap(MediaAssetEntity::getId, a -> a));
        for (int i = 0; i < ordered.size(); i++) {
            MediaAssetEntity asset = byId.get(ordered.get(i));
            postMediaRepository.save(new PostMediaEntity(post, asset, i, null));
        }
    }

    private void applyTags(PostEntity post, Long workspaceId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            post.replaceTags(Set.of());
            return;
        }
        List<TagEntity> found = tagRepository.findByWorkspaceIdAndIdIn(workspaceId, tagIds);
        if (found.size() != new HashSet<>(tagIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tags");
        }
        post.replaceTags(new HashSet<>(found));
    }

    private String resolveSlug(Long workspaceId, String title, String requestedSlug, Long postId) {
        String base = requestedSlug == null || requestedSlug.isBlank()
                ? Slugify.slugify(title)
                : Slugify.slugify(requestedSlug);
        String candidate = base;
        for (int i = 2; i < 10_000; i++) {
            var existing = postRepository.findByWorkspaceIdAndSlug(workspaceId, candidate);
            if (existing.isEmpty()) {
                return candidate;
            }
            if (postId != null && existing.get().getId().equals(postId)) {
                return candidate;
            }
            candidate = base + "-" + i;
        }
        return base + "-" + System.currentTimeMillis();
    }

    private void applySocialAndTargets(PostEntity post, PostPayload payload, boolean isCreate) {
        if (payload.socialPublishEnabled() != null) {
            post.setSocialPublishEnabled(payload.socialPublishEnabled());
        } else if (isCreate) {
            post.setSocialPublishEnabled(true);
        }
        boolean socialFieldsTouched =
                isCreate
                        || payload.socialPublishEnabled() != null
                        || payload.publishChannels() != null;
        if (!socialFieldsTouched) {
            return;
        }
        postChannelTargetRepository.deleteByPost_Id(post.getId());
        if (!post.isSocialPublishEnabled()) {
            return;
        }
        List<ChannelType> pc = payload.publishChannels();
        if (pc == null || pc.isEmpty()) {
            return;
        }
        for (ChannelType t : pc) {
            postChannelTargetRepository.save(new PostChannelTargetEntity(post, t));
        }
    }

    private Map<Long, List<ChannelType>> loadTargetsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ChannelType>> m = new HashMap<>();
        for (var row : postChannelTargetRepository.findRowsByPostIdIn(postIds)) {
            m.computeIfAbsent(row.getPostId(), k -> new ArrayList<>()).add(row.getChannelType());
        }
        return m;
    }

    private Map<Long, List<ChannelOutboundLogEntity>> loadLogsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return channelOutboundLogRepository.findByPost_IdIn(postIds).stream()
                .collect(Collectors.groupingBy(l -> l.getPost().getId()));
    }

    private PostResponse toResponse(PostEntity post) {
        List<ChannelType> targets = postChannelTargetRepository.findChannelTypesByPostId(post.getId());
        List<ChannelOutboundLogEntity> logs = channelOutboundLogRepository.findByPost_Id(post.getId());
        return toResponse(post, targets, logs);
    }

    private PostResponse toResponse(
            PostEntity post,
            List<ChannelType> publishChannelTypes,
            List<ChannelOutboundLogEntity> outboundLogs
    ) {
        List<TagSummaryDto> tags = post.getTags().stream()
                .sorted(Comparator.comparing(TagEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new TagSummaryDto(t.getId(), t.getName(), t.getSlug()))
                .toList();
        List<PostMediaAttachmentDto> media = postMediaRepository.findByPostIdOrderBySortOrderAsc(post.getId()).stream()
                .map(pm -> new PostMediaAttachmentDto(
                        pm.getMediaAsset().getId(),
                        pm.getMediaAsset().getMimeType(),
                        pm.getMediaAsset().getAltText(),
                        pm.getSortOrder(),
                        pm.getCaption()
                ))
                .toList();
        Long categoryId = post.getCategory() == null ? null : post.getCategory().getId();
        List<PostOutboundInfoDto> outbound = mergeOutbound(post, outboundLogs);
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getBodySource(),
                post.getBodyHtml(),
                post.getVisibility(),
                post.getStatus(),
                post.isAiGenerated(),
                categoryId,
                tags,
                media,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt(),
                post.getScheduledPublishAt(),
                post.isScheduleMissed(),
                post.isLateScheduleReleased(),
                post.isChannelSyndicationBlocked(),
                post.isSocialPublishEnabled(),
                post.getAiTokensTotal(),
                publishChannelTypes,
                outbound
        );
    }

    /**
     * Каналы, в которые должен уйти материал: явный список в {@code post_channel_targets} или все включённые каналы workspace.
     */
    private List<ChannelType> effectivePublishChannels(PostEntity post) {
        if (!post.isSocialPublishEnabled()) {
            return List.of();
        }
        List<ChannelType> explicit = postChannelTargetRepository.findChannelTypesByPostId(post.getId());
        if (!explicit.isEmpty()) {
            return explicit.stream().distinct().sorted(Comparator.comparing(Enum::name)).toList();
        }
        long wsId = post.getWorkspace().getId();
        return workspaceChannelRepository.findByWorkspaceIdAndEnabledIsTrueOrderByChannelTypeAsc(wsId).stream()
                .map(WorkspaceChannelEntity::getChannelType)
                .toList();
    }

    private List<PostOutboundInfoDto> mergeOutbound(PostEntity post, List<ChannelOutboundLogEntity> outboundLogs) {
        List<ChannelType> effective = effectivePublishChannels(post);
        if (effective.isEmpty()) {
            return List.of();
        }
        Map<ChannelType, ChannelOutboundLogEntity> byType = outboundLogs.stream()
                .collect(Collectors.toMap(ChannelOutboundLogEntity::getChannelType, Function.identity(), (a, b) -> a));
        List<PostOutboundInfoDto> rows = new ArrayList<>();
        for (ChannelType ct : effective) {
            ChannelOutboundLogEntity log = byType.get(ct);
            if (log == null) {
                rows.add(PostOutboundInfoDto.pending(ct));
            } else {
                rows.add(toOutboundInfo(log));
            }
        }
        return rows;
    }

    private PostOutboundInfoDto toOutboundInfo(ChannelOutboundLogEntity log) {
        long likes = 0;
        long reposts = 0;
        long views = 0;
        long comments = 0;
        long shares = 0;
        String mj = log.getMetricsJson();
        if (mj != null && !mj.isBlank()) {
            try {
                JsonNode n = objectMapper.readTree(mj);
                likes = n.path("likes").asLong(0);
                reposts = n.path("reposts").asLong(0);
                views = n.path("views").asLong(0);
                comments = n.path("comments").asLong(0);
                shares = n.path("shares").asLong(0);
            } catch (Exception ignored) {
                // keep zeros
            }
        }
        boolean showErr = log.getStatus() == ChannelDeliveryStatus.FAILED
                || log.getStatus() == ChannelDeliveryStatus.REJECTED;
        String errForUi = showErr && log.getErrorMessage() != null
                ? ChannelErrorSanitizer.stripSecrets(log.getErrorMessage())
                : null;
        return new PostOutboundInfoDto(
                log.getChannelType(),
                log.getStatus(),
                log.getExternalUrl(),
                errForUi,
                log.getMetricsFetchedAt(),
                likes,
                reposts,
                views,
                comments,
                shares
        );
    }
}
