CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    expires_at      timestamp with time zone NOT NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      timestamp with time zone
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE workspace_channels (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    channel_type    VARCHAR(32) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    label           VARCHAR(255),
    config_json     TEXT NOT NULL DEFAULT '{}',
    created_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, channel_type)
);

CREATE INDEX idx_workspace_channels_ws ON workspace_channels (workspace_id);

CREATE TABLE workspace_ai_prompts (
    id                  BIGSERIAL PRIMARY KEY,
    workspace_id        BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    prompt_key          VARCHAR(64) NOT NULL,
    title               VARCHAR(255),
    system_prompt       TEXT,
    user_prompt_template TEXT,
    created_at          timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, prompt_key)
);

CREATE INDEX idx_workspace_ai_prompts_ws ON workspace_ai_prompts (workspace_id);
