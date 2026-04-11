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

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "retryable", nullable = false)
    private boolean retryable = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "external_id", length = 256)
    private String externalId;

    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "metrics_fetched_at")
    private Instant metricsFetchedAt;

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
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.retryable = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public PostEntity getPost() {
        return post;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public ChannelDeliveryStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public Instant getMetricsFetchedAt() {
        return metricsFetchedAt;
    }

    public void markSent(Instant now) {
        markSent(now, null, null);
    }

    public void markSent(Instant now, String externalId, String externalUrl) {
        this.status = ChannelDeliveryStatus.SENT;
        this.errorMessage = null;
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.retryable = true;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.metricsJson = null;
        this.metricsFetchedAt = null;
        this.updatedAt = now;
    }

    public void setMetricsSnapshot(String metricsJson, Instant metricsFetchedAt) {
        this.metricsJson = metricsJson;
        this.metricsFetchedAt = metricsFetchedAt;
        this.updatedAt = metricsFetchedAt;
    }

    /**
     * Ошибка без повторных попыток (невалидный конфиг, исчерпан лимит, пост снят с публикации).
     */
    public void markTerminalFailure(String error, Instant now) {
        this.status = ChannelDeliveryStatus.FAILED;
        this.errorMessage = error;
        this.retryable = false;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }

    /**
     * Отклонение модерацией площадки — без ретраев.
     */
    public void markRejected(String error, Instant now) {
        this.status = ChannelDeliveryStatus.REJECTED;
        this.errorMessage = error;
        this.retryable = false;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }

    /**
     * Очередная неудачная попытка доставки; следующая — не раньше {@code nextRetryAt}.
     */
    public void markRetryableFailure(String error, Instant now, int newAttemptCount, Instant nextRetryAt) {
        this.status = ChannelDeliveryStatus.FAILED;
        this.errorMessage = error;
        this.attemptCount = newAttemptCount;
        this.nextRetryAt = nextRetryAt;
        this.retryable = true;
        this.updatedAt = now;
    }
}
