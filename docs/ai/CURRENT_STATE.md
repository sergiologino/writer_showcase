# Текущее состояние

## Репозиторий

- **Монорепозиторий**: `apps/backend` (Spring Boot), `apps/web` (Vite + React + TS), корневой `package.json` со скриптом **`npm run build`** (фронт: Vitest + tsc + vite; бэкенд: `mvnw verify`). Удалённый репозиторий: [github.com/sergiologino/writer_showcase](https://github.com/sergiologino/writer_showcase).
- **Память проекта**: `docs/ai/*`. Сверка с исходным ТЗ и план работ: [`TZ_BACKLOG.md`](../TZ_BACKLOG.md). Рабочий бэклог и приоритеты очереди: [`TODO.md`](./TODO.md).
- **Деплой (инструкции)**: [`DEPLOY_BACKEND.md`](../DEPLOY_BACKEND.md), [`DEPLOY_FRONTEND.md`](../DEPLOY_FRONTEND.md) в каталоге `docs/`.
- **Docker**: `docker-compose.yml` — PostgreSQL 16 и Redis 7; опционально сервис **`api`** (профиль `backend`): **`npm run docker:up:all`**. Образ собирается из **`apps/backend/Dockerfile`** (Maven внутри стадии build). Только БД + Redis без API: **`npm run docker:up`** (Redis нужен API с `PUBLISHER_REDIS_ENABLED=true`).

## Backend (`apps/backend`)

- Java **17** (на машине разработки; в ТЗ целевой **21** — см. `DECISIONS.md`).
- Spring Boot **3.3.6**, JPA, Security, JWT (JJWT), Flyway (**V1** схема данных, **V2** медиа, **V3** refresh-токены, каналы, промпты AI, **V4–V5** лог исходящих каналов + ретраи).
- API: `/api/auth/register`, `/api/auth/login`, **`/api/auth/refresh`**, `/api/me`, CRUD `/api/posts`, `/api/categories`, POST/GET `/api/tags`, **`/api/media`**, **`/api/channels`** (TELEGRAM, VK, ODNOKLASSNIKI, **MAX**), **`/api/ai/prompts`**, **`/api/ai/invoke`** (noteapp-ai-integration при `publisher.integration-ai.base-url` и `api-key`), публичное чтение `/api/public/...`. Пагинация: стабильный DTO **`PageResponse`** (не сырой `Page`). **Фоновая публикация в подключённые каналы** (в т.ч. TG/VK/OK/MAX) при первом переходе поста в `PUBLISHED` + `PUBLIC` (очередь Redis при `publisher.redis.enabled=true`, иначе in-memory; учёт в `channel_outbound_log` с **экспоненциальным backoff + jitter**, отдельный планировщик ретраев по `next_retry_at`; терминальные ошибки конфигурации без повторов, лимит попыток `publisher.channels.delivery.*`). MAX: `POST https://platform-api.max.ru/messages?chat_id=…`, токен и `chatId` в JSON канала (см. `DECISIONS.md` ADR-011).
- Rate limiting: **`ApiRateLimitFilter`** (окно 1 мин / IP; для логина/регистрации/refresh — отдельный более жёсткий лимит; отключение `publisher.rate-limit.enabled=false`).
- Выбор workspace: заголовок **`X-Workspace-Id`** или первый workspace пользователя.
- Тесты: H2 в памяти, уникальный URL с `${random.uuid}`; интеграционные сценарии регистрации/поста/публичной выдачи.
- **БД в разработке**: PostgreSQL из `docker-compose` или свой инстанс; URL/учётка по умолчанию в `application.yml`, при необходимости — env `DATASOURCE_*`.

## Frontend (`apps/web`)

- Маршруты: `/login`, `/register`, `/app/feed`, `/app/posts/new`, `/app/posts/:id`, публично `/blog/:workspaceSlug`, `/blog/:workspaceSlug/p/:postSlug`.
- Стили: Tailwind v4, нейтральная палитра и CSS-переменные, темы **светлая / тёмная / системная**.
- Dev: прокси Vite на `http://localhost:8080` для `/api`. Опционально **`VITE_API_BASE_URL`** для отдельного origin API (см. `apps/web/.env.example`).

## Следующие шаги по плану (приоритет)

Детализация и актуальная очередь: [`docs/ai/TODO.md`](./TODO.md).

1. **Интеграция с нейросетями (углубление)**: сценарии в продукте поверх существующего `noteapp-ai-integration` (UI, `requestType`, не только `chat`); в перспективе — варианты контента и `PostVariant` (см. `TZ_BACKLOG.md`).
2. **Публикация в TG/VK/MAX/каналах (углубление)**: обновление поста в канале при редактировании, шифрование секретов каналов at rest, расширенный формат сообщений (медиа, превью ссылок); при нескольких инстансах API — блокировки строк лога (например `SKIP LOCKED`) или вынос воркера.
3. **Фоновые задачи и кэш (углубление)**: метрики/квоты, отдельные job-типы по спецификации ТЗ; при необходимости вынести воркер или добавить DLQ.
4. **Публичный URL автора**: реализация ADR-008 (`/{author_nickname}/...`) вместо `workspace.slug` в путях.

## Бэклог (крупнее)

См. также [`TODO.md`](./TODO.md) и `TZ_BACKLOG.md`: чекбоксы каналов у поста, расписание, RSS, расширенная аналитика, миграция на Java 21 при появлении JDK.
