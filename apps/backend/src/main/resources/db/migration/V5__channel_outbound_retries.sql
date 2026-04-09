ALTER TABLE channel_outbound_log
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE channel_outbound_log
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE channel_outbound_log
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT TRUE;

-- Повторить ранее упавшие доставки (до лимита попыток в коде)
UPDATE channel_outbound_log
SET next_retry_at = CURRENT_TIMESTAMP,
    attempt_count = GREATEST(attempt_count, 1)
WHERE status = 'FAILED'
  AND retryable = TRUE
  AND next_retry_at IS NULL;

CREATE INDEX idx_channel_outbound_retry ON channel_outbound_log (status, next_retry_at);
