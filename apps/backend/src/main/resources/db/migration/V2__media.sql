CREATE TABLE media_assets (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    type            VARCHAR(32) NOT NULL,
    source_type     VARCHAR(32) NOT NULL,
    storage_key     VARCHAR(1024) NOT NULL,
    original_url    TEXT,
    mime_type       VARCHAR(255),
    size_bytes      BIGINT,
    width           INT,
    height          INT,
    duration_seconds INT,
    alt_text        VARCHAR(2000),
    metadata_json   TEXT,
    created_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, storage_key)
);

CREATE INDEX idx_media_workspace_created ON media_assets (workspace_id, created_at DESC);

CREATE TABLE post_media (
    id               BIGSERIAL PRIMARY KEY,
    post_id          BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    media_asset_id   BIGINT NOT NULL REFERENCES media_assets (id) ON DELETE CASCADE,
    sort_order       INT NOT NULL DEFAULT 0,
    caption          TEXT,
    UNIQUE (post_id, media_asset_id)
);

CREATE INDEX idx_post_media_post ON post_media (post_id);
CREATE INDEX idx_post_media_asset ON post_media (media_asset_id);
