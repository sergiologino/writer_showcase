-- Аватар (ссылка на media_assets) и личная галерея фотографий в профиле
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_media_asset_id BIGINT REFERENCES media_assets (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS user_profile_media (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    media_asset_id  BIGINT NOT NULL REFERENCES media_assets (id) ON DELETE CASCADE,
    sort_order      INT    NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_profile_media_user_asset UNIQUE (user_id, media_asset_id)
);

CREATE INDEX IF NOT EXISTS idx_user_profile_media_user ON user_profile_media (user_id);
