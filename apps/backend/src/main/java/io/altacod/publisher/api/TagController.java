package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.TagPayload;
import io.altacod.publisher.api.dto.TagResponse;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> list(@ActiveWorkspace Long workspaceId) {
        return tagService.list(workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse create(@ActiveWorkspace Long workspaceId, @Valid @RequestBody TagPayload payload) {
        return tagService.create(workspaceId, payload);
    }
}
