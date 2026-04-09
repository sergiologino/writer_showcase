package io.altacod.publisher.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, Long> {

    Optional<MediaAssetEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    List<MediaAssetEntity> findByWorkspaceIdAndIdIn(Long workspaceId, Collection<Long> ids);

    Page<MediaAssetEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);
}
