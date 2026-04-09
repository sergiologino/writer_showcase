package io.altacod.publisher.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelOutboundLogRepository extends JpaRepository<ChannelOutboundLogEntity, Long> {

    Optional<ChannelOutboundLogEntity> findByPost_IdAndChannelType(Long postId, ChannelType channelType);
}
