package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.AiInvokeRequest;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.api.dto.AiPromptResponse;
import io.altacod.publisher.api.dto.AiPromptUpsertPayload;
import io.altacod.publisher.api.dto.AiRoutingPayload;
import io.altacod.publisher.api.dto.StudioAiRequest;
import io.altacod.publisher.ai.AiNetworkRoutingService;
import io.altacod.publisher.integration.IntegrationAiClient;
import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiPromptService aiPromptService;
    private final AiInvokeService aiInvokeService;
    private final IntegrationAiClient integrationAiClient;
    private final AiNetworkRoutingService aiNetworkRoutingService;

    public AiController(
            AiPromptService aiPromptService,
            AiInvokeService aiInvokeService,
            IntegrationAiClient integrationAiClient,
            AiNetworkRoutingService aiNetworkRoutingService
    ) {
        this.aiPromptService = aiPromptService;
        this.aiInvokeService = aiInvokeService;
        this.integrationAiClient = integrationAiClient;
        this.aiNetworkRoutingService = aiNetworkRoutingService;
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

    @PostMapping("/studio/invoke")
    public AiInvokeResponse studioInvoke(
            @ActiveWorkspace Long workspaceId,
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody StudioAiRequest request
    ) {
        return aiInvokeService.studioInvoke(workspaceId, principal.getId(), request);
    }

    @GetMapping(value = "/admin/available-networks", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAvailableNetworks() {
        return integrationAiClient.fetchAvailableNetworksJson();
    }

    @GetMapping("/admin/routing")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, List<String>> adminGetRouting() {
        return aiNetworkRoutingService.getRouting();
    }

    @PutMapping("/admin/routing")
    @PreAuthorize("hasRole('ADMIN')")
    public void adminPutRouting(@RequestBody(required = false) AiRoutingPayload payload) {
        if (payload == null || payload.routing() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "routing required"
            );
        }
        aiNetworkRoutingService.saveRouting(payload.routing());
    }
}
