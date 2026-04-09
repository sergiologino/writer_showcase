package io.altacod.publisher.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceAiPromptRepository extends JpaRepository<WorkspaceAiPromptEntity, Long> {

    List<WorkspaceAiPromptEntity> findByWorkspaceIdOrderByPromptKeyAsc(Long workspaceId);

    Optional<WorkspaceAiPromptEntity> findByWorkspaceIdAndPromptKey(Long workspaceId, String promptKey);
}
