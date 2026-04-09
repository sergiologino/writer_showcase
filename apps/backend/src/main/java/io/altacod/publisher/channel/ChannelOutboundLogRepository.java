package io.altacod.publisher.channel;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChannelOutboundLogRepository extends JpaRepository<ChannelOutboundLogEntity, Long> {

    Optional<ChannelOutboundLogEntity> findByPost_IdAndChannelType(Long postId, ChannelType channelType);

    List<ChannelOutboundLogEntity> findByPost_Id(Long postId);

    List<ChannelOutboundLogEntity> findByPost_IdIn(Collection<Long> postIds);

    @Query("""
            select l from ChannelOutboundLogEntity l
            where l.status = :sent
            and l.externalId is not null
            and l.channelType in :types
            and (l.metricsFetchedAt is null or l.metricsFetchedAt < :staleBefore)
            """)
    List<ChannelOutboundLogEntity> findDueForMetrics(
            @Param("sent") ChannelDeliveryStatus sent,
            @Param("types") List<ChannelType> types,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable
    );

    @Query("""
            select l.post.id as postId, l.channelType as channelType
            from ChannelOutboundLogEntity l
            where l.status = :failed
            and l.retryable = true
            and l.nextRetryAt is not null
            and l.nextRetryAt <= :now
            order by l.nextRetryAt asc
            """)
    List<ChannelRetryJobRow> findDueRetries(
            @Param("failed") ChannelDeliveryStatus failed,
            @Param("now") Instant now,
            Pageable pageable
    );
}
