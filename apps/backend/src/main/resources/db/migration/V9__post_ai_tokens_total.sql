-- IF NOT EXISTS: колонку могли добавить вручную до прогона Flyway
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS ai_tokens_total BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN posts.ai_tokens_total IS 'Накопительно: сумма tokensUsed по вызовам AI в контексте этой статьи';
