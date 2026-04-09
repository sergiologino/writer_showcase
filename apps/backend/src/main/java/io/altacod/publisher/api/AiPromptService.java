package io.altacod.publisher.api;

import io.altacod.publisher.ai.WorkspaceAiPromptEntity;
import io.altacod.publisher.ai.WorkspaceAiPromptRepository;
import io.altacod.publisher.api.dto.AiPromptResponse;
import io.altacod.publisher.api.dto.AiPromptUpsertPayload;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiPromptService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private final WorkspaceAiPromptRepository promptRepository;
    private final WorkspaceRepository workspaceRepository;

    public AiPromptService(WorkspaceAiPromptRepository promptRepository, WorkspaceRepository workspaceRepository) {
        this.promptRepository = promptRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<AiPromptResponse> list(Long workspaceId) {
        return promptRepository.findByWorkspaceIdOrderByPromptKeyAsc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AiPromptResponse upsert(Long workspaceId, String promptKey, AiPromptUpsertPayload payload) {
        if (!KEY_PATTERN.matcher(promptKey).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid prompt key");
        }
        var ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        Instant now = Instant.now();
        var existing = promptRepository.findByWorkspaceIdAndPromptKey(workspaceId, promptKey);
        if (existing.isEmpty()) {
            var e = new WorkspaceAiPromptEntity(
                    ws,
                    promptKey,
                    blankToNull(payload.title()),
                    payload.systemPrompt(),
                    payload.userPromptTemplate(),
                    now
            );
            promptRepository.save(e);
            return toResponse(e);
        }
        WorkspaceAiPromptEntity e = existing.get();
        e.update(blankToNull(payload.title()), payload.systemPrompt(), payload.userPromptTemplate(), now);
        promptRepository.save(e);
        return toResponse(e);
    }

    private AiPromptResponse toResponse(WorkspaceAiPromptEntity e) {
        return new AiPromptResponse(
                e.getId(),
                e.getPromptKey(),
                e.getTitle(),
                e.getSystemPrompt(),
                e.getUserPromptTemplate(),
                e.getUpdatedAt()
        );
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
