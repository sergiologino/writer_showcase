package io.altacod.publisher.channel;

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
        name = "workspace_channels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "channel_type"})
)
public class WorkspaceChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 32)
    private ChannelType channelType;

    @Column(nullable = false)
    private boolean enabled = true;

    private String label;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkspaceChannelEntity() {
    }

    public WorkspaceChannelEntity(
            WorkspaceEntity workspace,
            ChannelType channelType,
            boolean enabled,
            String label,
            String configJson,
            Instant now
    ) {
        this.workspace = workspace;
        this.channelType = channelType;
        this.enabled = enabled;
        this.label = label;
        this.configJson = configJson;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLabel() {
        return label;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(boolean enabled, String label, String configJson, Instant now) {
        this.enabled = enabled;
        this.label = label;
        this.configJson = configJson;
        this.updatedAt = now;
    }
}
