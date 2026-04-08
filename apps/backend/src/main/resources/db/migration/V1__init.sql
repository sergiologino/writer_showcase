CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    locale          VARCHAR(32),
    timezone        VARCHAR(64),
    theme           VARCHAR(32),
    created_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    owner_id    BIGINT NOT NULL REFERENCES users (id),
    created_at  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE memberships (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role          VARCHAR(32) NOT NULL,
    created_at    timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE categories (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    description   TEXT,
    color         VARCHAR(32),
    created_at    timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, slug)
);

CREATE TABLE tags (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    created_at    timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, slug)
);

CREATE TABLE posts (
    id             BIGSERIAL PRIMARY KEY,
    workspace_id   BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    author_id      BIGINT NOT NULL REFERENCES users (id),
    category_id    BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    title          VARCHAR(500) NOT NULL,
    slug           VARCHAR(500) NOT NULL,
    excerpt        TEXT,
    body_source    TEXT,
    body_html      TEXT,
    visibility     VARCHAR(32) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   timestamp with time zone,
    UNIQUE (workspace_id, slug)
);

CREATE TABLE post_tags (
    post_id BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_posts_workspace_created ON posts (workspace_id, created_at DESC);
CREATE INDEX idx_posts_workspace_status ON posts (workspace_id, status);
CREATE INDEX idx_posts_workspace_visibility ON posts (workspace_id, visibility);
CREATE INDEX idx_memberships_user ON memberships (user_id);
CREATE INDEX idx_categories_workspace ON categories (workspace_id);
CREATE INDEX idx_tags_workspace ON tags (workspace_id);
