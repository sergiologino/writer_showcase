package io.altacod.publisher.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

    Optional<WorkspaceEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
