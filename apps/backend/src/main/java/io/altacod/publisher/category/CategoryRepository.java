package io.altacod.publisher.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    Optional<CategoryEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    boolean existsByWorkspaceIdAndSlug(Long workspaceId, String slug);

    Optional<CategoryEntity> findByWorkspaceIdAndSlug(Long workspaceId, String slug);
}
