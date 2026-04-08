package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.TagPayload;
import io.altacod.publisher.api.dto.TagResponse;
import io.altacod.publisher.common.Slugify;
import io.altacod.publisher.tag.TagEntity;
import io.altacod.publisher.tag.TagRepository;
import io.altacod.publisher.workspace.WorkspaceEntity;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final WorkspaceRepository workspaceRepository;

    public TagService(TagRepository tagRepository, WorkspaceRepository workspaceRepository) {
        this.tagRepository = tagRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long workspaceId) {
        return tagRepository.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TagResponse create(Long workspaceId, TagPayload payload) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        String slug = payload.slug() == null || payload.slug().isBlank()
                ? Slugify.uniqueSlug(payload.name(), s -> tagRepository.findByWorkspaceIdAndSlug(workspaceId, s).isPresent())
                : Slugify.uniqueSlug(payload.slug(), s -> tagRepository.findByWorkspaceIdAndSlug(workspaceId, s).isPresent());
        Instant now = Instant.now();
        var tag = new TagEntity(workspace, payload.name().trim(), slug, now);
        tagRepository.save(tag);
        return toResponse(tag);
    }

    private TagResponse toResponse(TagEntity t) {
        return new TagResponse(t.getId(), t.getName(), t.getSlug(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
