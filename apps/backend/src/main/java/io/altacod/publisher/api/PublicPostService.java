package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.CategoryResponse;
import io.altacod.publisher.api.dto.PublicMediaDto;
import io.altacod.publisher.api.dto.PublicPostDetailDto;
import io.altacod.publisher.api.dto.PublicPostSummaryDto;
import io.altacod.publisher.api.dto.TagSummaryDto;
import io.altacod.publisher.media.PostMediaRepository;
import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import io.altacod.publisher.tag.TagEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class PublicPostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;

    public PublicPostService(PostRepository postRepository, PostMediaRepository postMediaRepository) {
        this.postRepository = postRepository;
        this.postMediaRepository = postMediaRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicPostSummaryDto> listPublished(String workspaceSlug, Pageable pageable) {
        return postRepository.findPublicByWorkspaceSlug(workspaceSlug, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public PublicPostDetailDto getPublished(String workspaceSlug, String postSlug) {
        PostEntity post = postRepository.findPublishedPublic(postSlug, workspaceSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return toDetail(post);
    }

    private PublicPostSummaryDto toSummary(PostEntity post) {
        return new PublicPostSummaryDto(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getPublishedAt()
        );
    }

    private PublicPostDetailDto toDetail(PostEntity post) {
        CategoryResponse category = null;
        if (post.getCategory() != null) {
            var c = post.getCategory();
            category = new CategoryResponse(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getDescription(),
                    c.getColor(),
                    c.getCreatedAt(),
                    c.getUpdatedAt()
            );
        }
        List<TagSummaryDto> tags = post.getTags().stream()
                .sorted(Comparator.comparing(TagEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new TagSummaryDto(t.getId(), t.getName(), t.getSlug()))
                .toList();
        String slug = post.getWorkspace().getSlug();
        List<PublicMediaDto> media = postMediaRepository.findByPostIdOrderBySortOrderAsc(post.getId()).stream()
                .map(pm -> new PublicMediaDto(
                        pm.getMediaAsset().getId(),
                        "/api/public/w/" + slug + "/media/" + pm.getMediaAsset().getId() + "/file",
                        pm.getMediaAsset().getMimeType(),
                        pm.getMediaAsset().getAltText(),
                        pm.getSortOrder(),
                        pm.getCaption()
                ))
                .toList();
        return new PublicPostDetailDto(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getBodyHtml(),
                category,
                tags,
                media,
                post.getPublishedAt(),
                post.getUpdatedAt()
        );
    }
}
