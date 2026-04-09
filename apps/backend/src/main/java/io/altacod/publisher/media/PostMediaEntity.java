package io.altacod.publisher.media;

import io.altacod.publisher.post.PostEntity;
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

@Entity
@Table(
        name = "post_media",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "media_asset_id"})
)
public class PostMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAssetEntity mediaAsset;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Optional caption for this attachment on the post. */
    @Column(columnDefinition = "TEXT")
    private String caption;

    protected PostMediaEntity() {
    }

    public PostMediaEntity(PostEntity post, MediaAssetEntity mediaAsset, int sortOrder, String caption) {
        this.post = post;
        this.mediaAsset = mediaAsset;
        this.sortOrder = sortOrder;
        this.caption = caption;
    }

    public Long getId() {
        return id;
    }

    public PostEntity getPost() {
        return post;
    }

    public MediaAssetEntity getMediaAsset() {
        return mediaAsset;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getCaption() {
        return caption;
    }
}
