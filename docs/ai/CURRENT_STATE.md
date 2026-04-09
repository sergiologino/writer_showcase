# Текущее состояние

## Репозиторий

- **Монорепозиторий**: `apps/backend` (Spring Boot), `apps/web` (Vite + React + TS), корневой `package.json` со скриптом **`npm run build`** (фронт: Vitest + tsc + vite; бэкенд: `mvnw verify`). Удалённый репозиторий: [github.com/sergiologino/writer_showcase](https://github.com/sergiologino/writer_showcase).
- **Память проекта**: `docs/ai/*`.
- **Деплой (инструкции)**: [`DEPLOY_BACKEND.md`](../DEPLOY_BACKEND.md), [`DEPLOY_FRONTEND.md`](../DEPLOY_FRONTEND.md) в каталоге `docs/`.
- **Docker**: `docker-compose.yml` — PostgreSQL 16; опционально сервис **`api`** (профиль `backend`): **`npm run docker:up:all`**. Образ собирается из **`apps/backend/Dockerfile`** (Maven внутри стадии build). Только БД: **`npm run docker:up`**.

## Backend (`apps/backend`)

- Java **17** (на машине разработки; в ТЗ целевой **21** — см. `DECISIONS.md`).
- Spring Boot **3.3.6**, JPA, Security, JWT (JJWT), Flyway (**V1** схема данных, **V2** медиа, **V3** refresh-токены, каналы TG/VK, промпты AI).
- API: `/api/auth/register`, `/api/auth/login`, **`/api/auth/refresh`**, `/api/me`, CRUD `/api/posts`, `/api/categories`, POST/GET `/api/tags`, **`/api/media`**, **`/api/channels`** (TELEGRAM/VK), **`/api/ai/prompts`**, **`/api/ai/invoke`** (noteapp-ai-integration при `publisher.integration-ai.base-url` и `api-key`), публичное чтение `/api/public/...`. Пагинация: стабильный DTO **`PageResponse`** (не сырой `Page`).
- Rate limiting: **`ApiRateLimitFilter`** (окно 1 мин / IP; для логина/регистрации/refresh — отдельный более жёсткий лимит; отключение `publisher.rate-limit.enabled=false`).
- Выбор workspace: заголовок **`X-Workspace-Id`** или первый workspace пользователя.
- Тесты: H2 в памяти, уникальный URL с `${random.uuid}`; интеграционные сценарии регистрации/поста/публичной выдачи.
- **БД в разработке**: PostgreSQL из `docker-compose` или свой инстанс; URL/учётка по умолчанию в `application.yml`, при необходимости — env `DATASOURCE_*`.

## Frontend (`apps/web`)

- Маршруты: `/login`, `/register`, `/app/feed`, `/app/posts/new`, `/app/posts/:id`, публично `/blog/:workspaceSlug`, `/blog/:workspaceSlug/p/:postSlug`.
- Стили: Tailwind v4, нейтральная палитра и CSS-переменные, темы **светлая / тёмная / системная**.
- Dev: прокси Vite на `http://localhost:8080` для `/api`. Опционально **`VITE_API_BASE_URL`** для отдельного origin API (см. `apps/web/.env.example`).

## Следующие шаги по плану (приоритет)

1. **Публикация в TG/VK**: фоновые отправки по событиям (очередь/scheduler), шифрование секретов каналов at rest.
2. **Интеграционный AI**: вызов `POST /api/ai/process` noteapp-ai-integration (`AiRequestDTO` / `AiResponseDTO`), конфиг `publisher.integration-ai.*`; см. `noteapp-ai-integration/docs/ai/EXTERNAL_SERVICES_INTEGRATION.md`.
3. **Фоновые задачи и кэш**: Redis, планировщик (polling метрик, публикации) по спецификации.
4. **Публичный URL автора**: реализация ADR-008 (`/{author_nickname}/...`) вместо `workspace.slug` в путях.

## Бэклог (крупнее)

RSS, расширенная аналитика, миграция на Java 21 при появлении JDK.
