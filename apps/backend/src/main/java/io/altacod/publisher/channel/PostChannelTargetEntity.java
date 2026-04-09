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

@Entity
@Table(
        name = "post_channel_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "channel_type"})
)
public class PostChannelTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 32)
    private ChannelType channelType;

    protected PostChannelTargetEntity() {
    }

    public PostChannelTargetEntity(PostEntity post, ChannelType channelType) {
        this.post = post;
        this.channelType = channelType;
    }

    public ChannelType getChannelType() {
        return channelType;
    }
}
