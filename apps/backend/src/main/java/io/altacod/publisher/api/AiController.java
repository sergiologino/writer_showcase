package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.AiInvokeRequest;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.api.dto.AiPromptResponse;
import io.altacod.publisher.api.dto.AiPromptUpsertPayload;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiPromptService aiPromptService;
    private final AiInvokeService aiInvokeService;

    public AiController(AiPromptService aiPromptService, AiInvokeService aiInvokeService) {
        this.aiPromptService = aiPromptService;
        this.aiInvokeService = aiInvokeService;
    }

    @GetMapping("/prompts")
    public List<AiPromptResponse> listPrompts(@ActiveWorkspace Long workspaceId) {
        return aiPromptService.list(workspaceId);
    }

    @PutMapping("/prompts/{promptKey}")
    public AiPromptResponse upsertPrompt(
            @ActiveWorkspace Long workspaceId,
            @PathVariable String promptKey,
            @Valid @RequestBody AiPromptUpsertPayload payload
    ) {
        return aiPromptService.upsert(workspaceId, promptKey, payload);
    }

    @PostMapping("/invoke")
    public AiInvokeResponse invoke(
            @ActiveWorkspace Long workspaceId,
            @Valid @RequestBody AiInvokeRequest request
    ) {
        return aiInvokeService.invoke(workspaceId, request);
    }
}
