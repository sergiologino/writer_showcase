CREATE TABLE channel_outbound_log (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    channel_type    VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    error_message   TEXT,
    created_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, channel_type)
);

CREATE INDEX idx_channel_outbound_post ON channel_outbound_log (post_id);
