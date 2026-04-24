package io.altacod.publisher.user;

import io.altacod.publisher.media.MediaAssetEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "user_profile_media",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "media_asset_id"})
)
public class UserProfileMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAssetEntity mediaAsset;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserProfileMediaEntity() {
    }

    public UserProfileMediaEntity(UserEntity user, MediaAssetEntity mediaAsset, int sortOrder, Instant now) {
        this.user = user;
        this.mediaAsset = mediaAsset;
        this.sortOrder = sortOrder;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public MediaAssetEntity getMediaAsset() {
        return mediaAsset;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
