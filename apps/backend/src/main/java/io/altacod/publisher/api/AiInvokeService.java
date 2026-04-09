package io.altacod.publisher.api;

import io.altacod.publisher.ai.WorkspaceAiPromptRepository;
import io.altacod.publisher.api.dto.AiInvokeRequest;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.integration.AiPromptFormatting;
import io.altacod.publisher.integration.IntegrationAiClient;
import io.altacod.publisher.integration.NoteappAiProcessRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiInvokeService {

    private final WorkspaceAiPromptRepository promptRepository;
    private final IntegrationAiClient integrationAiClient;

    public AiInvokeService(WorkspaceAiPromptRepository promptRepository, IntegrationAiClient integrationAiClient) {
        this.promptRepository = promptRepository;
        this.integrationAiClient = integrationAiClient;
    }

    @Transactional(readOnly = true)
    public AiInvokeResponse invoke(Long workspaceId, AiInvokeRequest request) {
        var prompt = promptRepository.findByWorkspaceIdAndPromptKey(workspaceId, request.promptKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt not found"));
        Map<String, String> vars = request.variables() == null ? Map.of() : request.variables();
        String userId = request.externalUserId() == null || request.externalUserId().isBlank()
                ? "publisher-workspace-" + workspaceId
                : request.externalUserId().trim();
        String networkName = request.networkName() == null || request.networkName().isBlank()
                ? null
                : request.networkName().trim();
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
        NoteappAiProcessRequest processRequest = new NoteappAiProcessRequest(
                userId,
                networkName,
                requestType,
                payload,
                metadata.isEmpty() ? null : metadata
        );
        AiInvokeResponse res = integrationAiClient.send(processRequest);
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
