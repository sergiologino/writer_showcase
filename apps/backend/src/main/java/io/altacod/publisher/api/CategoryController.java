package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.CategoryPayload;
import io.altacod.publisher.api.dto.CategoryResponse;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@ActiveWorkspace Long workspaceId) {
        return categoryService.list(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@ActiveWorkspace Long workspaceId, @Valid @RequestBody CategoryPayload payload) {
        return categoryService.create(workspaceId, payload);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @ActiveWorkspace Long workspaceId,
            @PathVariable Long id,
            @Valid @RequestBody CategoryPayload payload
    ) {
        return categoryService.update(workspaceId, id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@ActiveWorkspace Long workspaceId, @PathVariable Long id) {
        categoryService.delete(workspaceId, id);
    }
}
