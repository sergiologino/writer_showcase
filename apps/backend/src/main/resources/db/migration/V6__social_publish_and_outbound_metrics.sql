ALTER TABLE posts ADD COLUMN social_publish_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE post_channel_targets (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    channel_type    VARCHAR(32) NOT NULL,
    UNIQUE (post_id, channel_type)
);

CREATE INDEX idx_post_channel_targets_post ON post_channel_targets (post_id);

ALTER TABLE channel_outbound_log ADD COLUMN external_id VARCHAR(256);
ALTER TABLE channel_outbound_log ADD COLUMN external_url VARCHAR(2048);
ALTER TABLE channel_outbound_log ADD COLUMN metrics_json TEXT;
ALTER TABLE channel_outbound_log ADD COLUMN metrics_fetched_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_channel_outbound_metrics ON channel_outbound_log (status, channel_type);
