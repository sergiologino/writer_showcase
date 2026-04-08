package io.altacod.publisher.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<TagEntity, Long> {

    List<TagEntity> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    Optional<TagEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    List<TagEntity> findByWorkspaceIdAndIdIn(Long workspaceId, Collection<Long> ids);

    Optional<TagEntity> findByWorkspaceIdAndSlug(Long workspaceId, String slug);
}
