package io.altacod.publisher.media;

import io.altacod.publisher.workspace.WorkspaceEntity;
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
        name = "media_assets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "storage_key"})
)
public class MediaAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MediaType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private MediaSourceType sourceType;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "alt_text", length = 2000)
    private String altText;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAssetEntity() {
    }

    public MediaAssetEntity(
            WorkspaceEntity workspace,
            MediaType type,
            MediaSourceType sourceType,
            String storageKey,
            String mimeType,
            Long sizeBytes,
            String altText,
            Instant createdAt
    ) {
        this.workspace = workspace;
        this.type = type;
        this.sourceType = sourceType;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.altText = altText;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public MediaType getType() {
        return type;
    }

    public MediaSourceType getSourceType() {
        return sourceType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
