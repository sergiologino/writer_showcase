package io.altacod.publisher.ai;

import io.altacod.publisher.workspace.WorkspaceEntity;
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
        name = "workspace_ai_prompts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "prompt_key"})
)
public class WorkspaceAiPromptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Column(name = "prompt_key", nullable = false, length = 64)
    private String promptKey;

    private String title;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "TEXT")
    private String userPromptTemplate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkspaceAiPromptEntity() {
    }

    public WorkspaceAiPromptEntity(
            WorkspaceEntity workspace,
            String promptKey,
            String title,
            String systemPrompt,
            String userPromptTemplate,
            Instant now
    ) {
        this.workspace = workspace;
        this.promptKey = promptKey;
        this.title = title;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getTitle() {
        return title;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPromptTemplate() {
        return userPromptTemplate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String title, String systemPrompt, String userPromptTemplate, Instant now) {
        this.title = title;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.updatedAt = now;
    }
}
