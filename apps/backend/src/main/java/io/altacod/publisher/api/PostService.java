package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PostMediaAttachmentDto;
import io.altacod.publisher.api.dto.PostPayload;
import io.altacod.publisher.api.dto.PostResponse;
import io.altacod.publisher.api.dto.TagSummaryDto;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public PostService(
            PostRepository postRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            PostMediaRepository postMediaRepository,
            MediaAssetRepository mediaAssetRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.postRepository = postRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.postMediaRepository = postMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.eventPublisher = eventPublisher;
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
        applyCategory(post, workspaceId, payload.categoryId());
        applyTags(post, workspaceId, payload.tagIds());

        postRepository.save(post);
        applyPostMedia(post, workspaceId, payload.mediaAssetIds());
        if (payload.status() == PostStatus.PUBLISHED && payload.visibility() == PostVisibility.PUBLIC) {
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
        return postRepository.findWorkspaceFeed(workspaceId, status, qPattern, sorted).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long workspaceId, Long id) {
        PostEntity post = postRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return toResponse(post);
    }

    @Transactional
    public PostResponse update(Long workspaceId, Long id, PostPayload payload) {
        PostEntity post = postRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        PostStatus previous = post.getStatus();
        PostVisibility previousVisibility = post.getVisibility();
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
        post.replaceTags(Set.of());
        applyCategory(post, workspaceId, payload.categoryId());
        applyTags(post, workspaceId, payload.tagIds());
        applyPostMedia(post, workspaceId, payload.mediaAssetIds());

        boolean wasPublishedPublic =
                previous == PostStatus.PUBLISHED && previousVisibility == PostVisibility.PUBLIC;
        boolean nowPublishedPublic =
                payload.status() == PostStatus.PUBLISHED && payload.visibility() == PostVisibility.PUBLIC;
        if (nowPublishedPublic && !wasPublishedPublic) {
            eventPublisher.publishEvent(new PostPublishedEvent(post.getId()));
        }

        return toResponse(post);
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

    private PostResponse toResponse(PostEntity post) {
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
                post.getPublishedAt()
        );
    }
}
