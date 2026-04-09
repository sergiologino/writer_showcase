# Текущее состояние

## Репозиторий

- **Монорепозиторий**: `apps/backend` (Spring Boot), `apps/web` (Vite + React + TS), корневой `package.json` со скриптом **`npm run build`** (фронт: Vitest + tsc + vite; бэкенд: `mvnw verify`). Удалённый репозиторий: [github.com/sergiologino/writer_showcase](https://github.com/sergiologino/writer_showcase).
- **Память проекта**: `docs/ai/*`.
- **Деплой (инструкции)**: [`DEPLOY_BACKEND.md`](../DEPLOY_BACKEND.md), [`DEPLOY_FRONTEND.md`](../DEPLOY_FRONTEND.md) в каталоге `docs/`.
- **Docker**: `docker-compose.yml` — PostgreSQL 16 и Redis 7; опционально сервис **`api`** (профиль `backend`): **`npm run docker:up:all`**. Образ собирается из **`apps/backend/Dockerfile`** (Maven внутри стадии build). Только БД + Redis без API: **`npm run docker:up`** (Redis нужен API с `PUBLISHER_REDIS_ENABLED=true`).

## Backend (`apps/backend`)

- Java **17** (на машине разработки; в ТЗ целевой **21** — см. `DECISIONS.md`).
- Spring Boot **3.3.6**, JPA, Security, JWT (JJWT), Flyway (**V1** схема данных, **V2** медиа, **V3** refresh-токены, каналы TG/VK, промпты AI).
- API: `/api/auth/register`, `/api/auth/login`, **`/api/auth/refresh`**, `/api/me`, CRUD `/api/posts`, `/api/categories`, POST/GET `/api/tags`, **`/api/media`**, **`/api/channels`** (TELEGRAM/VK), **`/api/ai/prompts`**, **`/api/ai/invoke`** (noteapp-ai-integration при `publisher.integration-ai.base-url` и `api-key`), публичное чтение `/api/public/...`. Пагинация: стабильный DTO **`PageResponse`** (не сырой `Page`). **Фоновая публикация в TG/VK** при первом переходе поста в `PUBLISHED` + `PUBLIC` (очередь Redis при `publisher.redis.enabled=true`, иначе in-memory; учёт в `channel_outbound_log`).
- Rate limiting: **`ApiRateLimitFilter`** (окно 1 мин / IP; для логина/регистрации/refresh — отдельный более жёсткий лимит; отключение `publisher.rate-limit.enabled=false`).
- Выбор workspace: заголовок **`X-Workspace-Id`** или первый workspace пользователя.
- Тесты: H2 в памяти, уникальный URL с `${random.uuid}`; интеграционные сценарии регистрации/поста/публичной выдачи.
- **БД в разработке**: PostgreSQL из `docker-compose` или свой инстанс; URL/учётка по умолчанию в `application.yml`, при необходимости — env `DATASOURCE_*`.

## Frontend (`apps/web`)

- Маршруты: `/login`, `/register`, `/app/feed`, `/app/posts/new`, `/app/posts/:id`, публично `/blog/:workspaceSlug`, `/blog/:workspaceSlug/p/:postSlug`.
- Стили: Tailwind v4, нейтральная палитра и CSS-переменные, темы **светлая / тёмная / системная**.
- Dev: прокси Vite на `http://localhost:8080` для `/api`. Опционально **`VITE_API_BASE_URL`** для отдельного origin API (см. `apps/web/.env.example`).

## Следующие шаги по плану (приоритет)

1. **Публикация в TG/VK (углубление)**: ретраи с backoff, обновление поста в канале при редактировании, шифрование секретов каналов at rest, расширенный формат сообщений (медиа, превью ссылок).
2. **Фоновые задачи и кэш (углубление)**: метрики/квоты, отдельные job-типы по спецификации ТЗ; при необходимости вынести воркер или добавить DLQ.
3. **Публичный URL автора**: реализация ADR-008 (`/{author_nickname}/...`) вместо `workspace.slug` в путях.
4. **Интеграционный AI**: уже подключён к noteapp-ai-integration (`POST /api/ai/process`, `publisher.integration-ai.*`); доработки — по продукту (UI, типы запросов, не `chat`).

## Бэклог (крупнее)

RSS, расширенная аналитика, миграция на Java 21 при появлении JDK.
