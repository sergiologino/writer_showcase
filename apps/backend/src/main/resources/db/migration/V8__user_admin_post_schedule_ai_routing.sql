-- Админ, посты с пропущенным расписанием (офлайн → синхронизация), приоритеты нейросетей (глобальные)
ALTER TABLE users
    ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE posts
    ADD COLUMN scheduled_publish_at timestamp with time zone;
ALTER TABLE posts
    ADD COLUMN schedule_missed boolean NOT NULL DEFAULT false;
ALTER TABLE posts
    ADD COLUMN late_schedule_released boolean NOT NULL DEFAULT true;

CREATE TABLE app_ai_routing (
    id         BIGINT       NOT NULL PRIMARY KEY,
    routing_json TEXT       NOT NULL DEFAULT '{}',
    updated_at  timestamp with time zone NOT NULL
);

INSERT INTO app_ai_routing (id, routing_json, updated_at)
VALUES (1, '{}', CURRENT_TIMESTAMP);
