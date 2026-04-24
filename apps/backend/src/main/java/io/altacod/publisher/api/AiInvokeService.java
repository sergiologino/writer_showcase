package io.altacod.publisher.api;

import io.altacod.publisher.ai.AiNetworkRoutingService;
import io.altacod.publisher.ai.WorkspaceAiPromptRepository;
import io.altacod.publisher.api.dto.AiInvokeRequest;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.api.dto.StudioAiRequest;
import io.altacod.publisher.integration.AiPromptFormatting;
import io.altacod.publisher.integration.IntegrationAiClient;
import io.altacod.publisher.integration.NoteappAiProcessRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiInvokeService {

    private final WorkspaceAiPromptRepository promptRepository;
    private final IntegrationAiClient integrationAiClient;
    private final AiNetworkRoutingService routingService;

    public AiInvokeService(
            WorkspaceAiPromptRepository promptRepository,
            IntegrationAiClient integrationAiClient,
            AiNetworkRoutingService routingService
    ) {
        this.promptRepository = promptRepository;
        this.integrationAiClient = integrationAiClient;
        this.routingService = routingService;
    }

    @Transactional(readOnly = true)
    public AiInvokeResponse invoke(Long workspaceId, AiInvokeRequest request) {
        var prompt = promptRepository.findByWorkspaceIdAndPromptKey(workspaceId, request.promptKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt not found"));
        Map<String, String> vars = request.variables() == null ? Map.of() : request.variables();
        String userId = request.externalUserId() == null || request.externalUserId().isBlank()
                ? "publisher-workspace-" + workspaceId
                : request.externalUserId().trim();
        String requestType = request.requestType() == null || request.requestType().isBlank()
                ? "chat"
                : request.requestType().trim();
        Map<String, Object> payload = AiPromptFormatting.chatPayload(
                prompt.getSystemPrompt(),
                prompt.getUserPromptTemplate(),
                vars,
                request.contentSnippet()
        );
        Map<String, String> metadata = enrichMetadata(request.metadata(), workspaceId, request.promptKey());
        NoteappAiProcessRequest base = new NoteappAiProcessRequest(
                userId,
                null,
                requestType,
                payload,
                metadata.isEmpty() ? null : metadata
        );
        return sendWithNetworkFallback(
                request.networkName() == null || request.networkName().isBlank() ? null : request.networkName().trim(),
                requestType,
                base
        );
    }

    @Transactional(readOnly = true)
    public AiInvokeResponse studioInvoke(Long workspaceId, Long userId, StudioAiRequest request) {
        if (request.payload() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload is required");
        }
        String ext = request.externalUserId() == null || request.externalUserId().isBlank()
                ? "publisher-user-" + userId
                : request.externalUserId().trim();
        String requestType = request.requestType() == null || request.requestType().isBlank()
                ? "chat"
                : request.requestType().trim();
        Map<String, String> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.putIfAbsent("publisher.source", "publisher-studio");
        metadata.put("publisher.workspaceId", String.valueOf(workspaceId));
        Map<String, Object> p = new HashMap<>(request.payload());
        NoteappAiProcessRequest base = new NoteappAiProcessRequest(
                ext,
                null,
                requestType,
                p,
                metadata.isEmpty() ? null : metadata
        );
        String explicit = request.networkName() == null || request.networkName().isBlank()
                ? null
                : request.networkName().trim();
        return sendWithNetworkFallback(explicit, requestType, base);
    }

    private AiInvokeResponse sendWithNetworkFallback(
            String explicitNetworkName,
            String requestType,
            NoteappAiProcessRequest base
    ) {
        if (explicitNetworkName != null && !explicitNetworkName.isEmpty()) {
            return mapNotConfigured(
                    integrationAiClient.send(cloneWithNetwork(base, explicitNetworkName))
            );
        }
        List<String> order = routingService.orderedNamesForRequestType(requestType);
        if (order.isEmpty()) {
            return mapNotConfigured(integrationAiClient.send(cloneWithNetwork(base, null)));
        }
        AiInvokeResponse last = null;
        for (String name : order) {
            last = integrationAiClient.send(cloneWithNetwork(base, name));
            if (last != null && last.ok()) {
                return last;
            }
        }
        return mapNotConfigured(last);
    }

    private static NoteappAiProcessRequest cloneWithNetwork(NoteappAiProcessRequest b, String networkName) {
        return new NoteappAiProcessRequest(
                b.userId(),
                networkName,
                b.requestType(),
                b.payload(),
                b.metadata()
        );
    }

    private AiInvokeResponse mapNotConfigured(AiInvokeResponse res) {
        if (!res.ok() && "NOT_CONFIGURED".equals(res.errorCode())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI integration is not configured (set publisher.integration-ai.base-url and api-key)"
            );
        }
        return res;
    }

    private static Map<String, String> enrichMetadata(Map<String, String> fromRequest, Long workspaceId, String promptKey) {
        Map<String, String> m = new HashMap<>();
        if (fromRequest != null) {
            m.putAll(fromRequest);
        }
        m.putIfAbsent("publisher.source", "publisher-api");
        m.put("publisher.workspaceId", String.valueOf(workspaceId));
        m.put("publisher.promptKey", promptKey);
        return m;
    }
}
