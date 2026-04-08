package io.altacod.publisher.workspace;

import io.altacod.publisher.user.UserEntity;
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
        name = "memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
)
public class MembershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MembershipEntity() {
    }

    public MembershipEntity(WorkspaceEntity workspace, UserEntity user, MembershipRole role, Instant now) {
        this.workspace = workspace;
        this.user = user;
        this.role = role;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public UserEntity getUser() {
        return user;
    }

    public MembershipRole getRole() {
        return role;
    }
}
