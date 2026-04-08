package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.CategoryPayload;
import io.altacod.publisher.api.dto.CategoryResponse;
import io.altacod.publisher.category.CategoryEntity;
import io.altacod.publisher.category.CategoryRepository;
import io.altacod.publisher.common.Slugify;
import io.altacod.publisher.workspace.WorkspaceEntity;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final WorkspaceRepository workspaceRepository;

    public CategoryService(CategoryRepository categoryRepository, WorkspaceRepository workspaceRepository) {
        this.categoryRepository = categoryRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long workspaceId) {
        return categoryRepository.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long workspaceId, CategoryPayload payload) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        String slug = payload.slug() == null || payload.slug().isBlank()
                ? Slugify.uniqueSlug(payload.name(), s -> categoryRepository.existsByWorkspaceIdAndSlug(workspaceId, s))
                : Slugify.uniqueSlug(payload.slug(), s -> categoryRepository.existsByWorkspaceIdAndSlug(workspaceId, s));
        Instant now = Instant.now();
        var cat = new CategoryEntity(
                workspace,
                payload.name().trim(),
                slug,
                payload.description(),
                payload.color(),
                now
        );
        categoryRepository.save(cat);
        return toResponse(cat);
    }

    @Transactional
    public CategoryResponse update(Long workspaceId, Long id, CategoryPayload payload) {
        CategoryEntity cat = categoryRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        String slug = payload.slug() == null || payload.slug().isBlank()
                ? Slugify.slugify(payload.name())
                : Slugify.slugify(payload.slug());
        if (!slug.equals(cat.getSlug())) {
            slug = Slugify.uniqueSlug(slug, candidate -> categoryRepository
                    .findByWorkspaceIdAndSlug(workspaceId, candidate)
                    .map(other -> !other.getId().equals(cat.getId()))
                    .orElse(false));
        }
        cat.update(payload.name().trim(), slug, payload.description(), payload.color(), Instant.now());
        return toResponse(cat);
    }

    @Transactional
    public void delete(Long workspaceId, Long id) {
        CategoryEntity cat = categoryRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryRepository.delete(cat);
    }

    private CategoryResponse toResponse(CategoryEntity c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                c.getColor(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
