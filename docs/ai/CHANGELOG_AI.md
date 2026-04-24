# История изменений (AI-память)

Краткие записи (1–3 строки) по итогам задач; без логов и отладочного шума.

- **2026-04-08**: Инициализированы `docs/ai/*`; состояние: только память, код отсутствует; ТЗ сведено из `altacod_publisher_spec.md`, зафиксированы ADR-001 (модульный монолит) и ADR-002 (стек по ТЗ).

- **2026-04-09**: Монорепозиторий MVP: backend (auth JWT, workspaces, posts/categories/tags, публичное API), frontend (вход/регистрация, лента, редактор, публичный блог, темы); Flyway V1; тесты бэкенда и фронта; корневая сборка `npm run build`. ADR-003…006 в `DECISIONS.md`.

- **2026-04-09**: Добавлен `docker-compose.yml` (Postgres 16) и npm-скрипты `docker:*`; ADR-007 (compose), ADR-008 (целевой URL `/{author_nickname}/...` после MVP, пока MVP на `workspace.slug`).

- **2026-04-09**: Добавлены `docs/DEPLOY_BACKEND.md` и `docs/DEPLOY_FRONTEND.md` (IDEA, локальный Postgres, сервер, Coolify); в `CURRENT_STATE.md` — ссылки и приоритизированные следующие шаги.

- **2026-04-09**: Репозиторий Git: ветка `main`, remote [sergiologino/writer_showcase](https://github.com/sergiologino/writer_showcase); корневой `.gitignore`, первый push выполнен.

- **2026-04-09**: План инфраструктуры: `apps/backend/Dockerfile`, `.dockerignore`, compose-сервис `api` (profile `backend`), npm `docker:up:all` / `docker:logs:api`; фронт — `VITE_API_BASE_URL`, `resolveApiUrl`, тесты, `apps/web/.env.example`; обновлены DEPLOY_*. ADR-009, ADR-010.

- **2026-04-09**: Refresh-токены (opaque + SHA-256 в БД, ротация при `/api/auth/refresh`), каналы TG/VK (`workspace_channels`, API `/api/channels`, маскирование секретов в ответах), промпты AI (`workspace_ai_prompts`, `/api/ai/prompts`, `/api/ai/invoke` → внешний сервис по `publisher.integration-ai.*`), `PageResponse`, in-memory rate limit по IP; фронт: `refreshToken`, авто-refresh при 401. Flyway V3.

- **2026-04-09**: Интеграция AI переведена на контракт noteapp-ai-integration: `POST /api/ai/process`, только `X-API-Key`, тело как `AiRequestDTO` (userId, networkName, requestType, payload с `messages`, metadata); ответ `AiInvokeResponse` разбирается по `status`/`errorMessage`; env `AI_INTEGRATION_*` (и совместимость `INTEGRATION_AI_*`).

- **2026-04-09**: Фоновая публикация в TG/VK: событие после первого `PUBLISHED`+`PUBLIC`, очередь in-memory или Redis (`publisher.redis.*`), планировщик, `channel_outbound_log` (Flyway V4), ссылки на пост через `publisher.public-site.base-url`; в compose добавлен Redis для profile `backend`.

- **2026-04-09**: Надёжность доставки: Flyway V5 (`attempt_count`, `next_retry_at`, `retryable`), экспоненциальный backoff с jitter и лимитом, отдельный `@Scheduled` для ретраев, терминальные сбои конфигурации без повторов; настройки `publisher.channels.delivery.*` и `retry-poll-ms`.

- **2026-04-22**: Добавлен `docs/ai/TODO.md` (очередь: канал МАКС, углубление AI, прочие пункты из `CURRENT_STATE`/`TZ_BACKLOG`); `CURRENT_STATE.md` обновлён — ссылка на TODO и переставлены приоритеты (MAX и AI впереди).

- **2026-04-22**: Канал **MAX** (мессенджер МАКС): `ChannelType.MAX`, публикация через MAX Bot API (`platform-api.max.ru/messages`, конфиг `accessToken` + `chatId`), UI на `/app/.../channels` (страница «Каналы публикации»), тест `MaxMessengerHttpContractTest`, ADR-011.

- **2026-04-22**: Нейросети: Flyway V8 (`users.is_admin`, план публикации у поста, `app_ai_routing`), `POST /api/ai/studio/invoke`, admin `GET/PUT` routing + список сетей; фронт — AI-студия, расписание, баннер `channelSyndicationBlocked`, блок админа в профиле. Документация env и проверок: [`docs/ai/AI_INTEGRATION.md`](./AI_INTEGRATION.md); в `application.yml` — `AI_INTEGRATION_AVAILABLE_NETWORKS_PATH` (и синоним `INTEGRATION_AI_AVAILABLE_NETWORKS_PATH`), шаблон `apps/backend/.env.example` расширен.

- **2026-04-23**: Совместимость с другими сервисами: `AI_INTEGRATION_URL` как альтернатива `AI_INTEGRATION_BASE_URL` (приоритет у `BASE_URL`); `ADMIN_EMAILS` — глобальные админы по email (`publisher.security.admin-emails`), в т.ч. для `ROLE_ADMIN` и поля `isAdmin` в `/api/me`.
