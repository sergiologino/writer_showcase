package io.altacod.publisher.channel;

import io.altacod.publisher.post.PostEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "channel_outbound_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "channel_type"})
)
public class ChannelOutboundLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 32)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChannelDeliveryStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChannelOutboundLogEntity() {
    }

    public ChannelOutboundLogEntity(
            PostEntity post,
            ChannelType channelType,
            ChannelDeliveryStatus status,
            String errorMessage,
            Instant now
    ) {
        this.post = post;
        this.channelType = channelType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public ChannelDeliveryStatus getStatus() {
        return status;
    }

    public void markFailed(String error, Instant now) {
        this.status = ChannelDeliveryStatus.FAILED;
        this.errorMessage = error;
        this.updatedAt = now;
    }

    public void markSent(Instant now) {
        this.status = ChannelDeliveryStatus.SENT;
        this.errorMessage = null;
        this.updatedAt = now;
    }
}
