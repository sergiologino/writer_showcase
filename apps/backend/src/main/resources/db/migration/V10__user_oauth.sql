-- OAuth-only пользователи: password_hash NULL; (provider, subject) — уникальная привязка
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(32);
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS oauth_subject VARCHAR(255);

-- Без WHERE: H2 (тесты) не поддерживает частичные индексы в этом синтаксисе.
-- В PostgreSQL и H2 в UNIQUE-индексе NULL ≠ NULL, несколько (NULL, NULL) допустимы.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_oauth_provider_subject
    ON users (oauth_provider, oauth_subject);
