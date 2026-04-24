package io.altacod.publisher.post;

import io.altacod.publisher.category.CategoryEntity;
import io.altacod.publisher.tag.TagEntity;
import io.altacod.publisher.user.UserEntity;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "posts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "slug"})
)
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "body_source", columnDefinition = "TEXT")
    private String bodySource;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostStatus status;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "scheduled_publish_at")
    private Instant scheduledPublishAt;

    @Column(name = "schedule_missed", nullable = false)
    private boolean scheduleMissed;

    @Column(name = "late_schedule_released", nullable = false)
    private boolean lateScheduleReleased = true;

    @Column(name = "social_publish_enabled", nullable = false)
    private boolean socialPublishEnabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    protected PostEntity() {
    }

    public PostEntity(
            WorkspaceEntity workspace,
            UserEntity author,
            String title,
            String slug,
            String excerpt,
            String bodySource,
            String bodyHtml,
            PostVisibility visibility,
            PostStatus status,
            boolean aiGenerated,
            Instant now
    ) {
        this.workspace = workspace;
        this.author = author;
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.bodySource = bodySource;
        this.bodyHtml = bodyHtml;
        this.visibility = visibility;
        this.status = status;
        this.aiGenerated = aiGenerated;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public WorkspaceEntity getWorkspace() {
        return workspace;
    }

    public UserEntity getAuthor() {
        return author;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public void setCategory(CategoryEntity category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public String getBodySource() {
        return bodySource;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public PostStatus getStatus() {
        return status;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    public void setScheduledPublishAt(Instant scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }

    public boolean isScheduleMissed() {
        return scheduleMissed;
    }

    public void setScheduleMissed(boolean scheduleMissed) {
        this.scheduleMissed = scheduleMissed;
    }

    public boolean isLateScheduleReleased() {
        return lateScheduleReleased;
    }

    public void setLateScheduleReleased(boolean lateScheduleReleased) {
        this.lateScheduleReleased = lateScheduleReleased;
    }

    /**
     * Синхронизация наступила после плановой публикации; в каналы не уходит, пока автор не внесёт правки.
     */
    public boolean isChannelSyndicationBlocked() {
        return scheduleMissed && !lateScheduleReleased;
    }

    public boolean isSocialPublishEnabled() {
        return socialPublishEnabled;
    }

    public void setSocialPublishEnabled(boolean socialPublishEnabled) {
        this.socialPublishEnabled = socialPublishEnabled;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void replaceTags(Set<TagEntity> next) {
        this.tags.clear();
        this.tags.addAll(next);
    }

    public void updateContent(
            String title,
            String slug,
            String excerpt,
            String bodySource,
            String bodyHtml,
            PostVisibility visibility,
            PostStatus status,
            Instant now,
            Instant publishedAt
    ) {
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.bodySource = bodySource;
        this.bodyHtml = bodyHtml;
        this.visibility = visibility;
        this.status = status;
        this.updatedAt = now;
        this.publishedAt = publishedAt;
    }
}
