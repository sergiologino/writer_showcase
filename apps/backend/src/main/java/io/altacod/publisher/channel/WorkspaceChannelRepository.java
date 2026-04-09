package io.altacod.publisher.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceChannelRepository extends JpaRepository<WorkspaceChannelEntity, Long> {

    List<WorkspaceChannelEntity> findByWorkspaceIdOrderByChannelTypeAsc(Long workspaceId);

    Optional<WorkspaceChannelEntity> findByWorkspaceIdAndChannelType(Long workspaceId, ChannelType channelType);
}
