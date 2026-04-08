# Текущее состояние

## Репозиторий

- **Монорепозиторий**: `apps/backend` (Spring Boot), `apps/web` (Vite + React + TS), корневой `package.json` со скриптом **`npm run build`** (фронт: Vitest + tsc + vite; бэкенд: `mvnw verify`). Удалённый репозиторий: [github.com/sergiologino/writer_showcase](https://github.com/sergiologino/writer_showcase).
- **Память проекта**: `docs/ai/*`.
- **Деплой (инструкции)**: [`DEPLOY_BACKEND.md`](../DEPLOY_BACKEND.md), [`DEPLOY_FRONTEND.md`](../DEPLOY_FRONTEND.md) в каталоге `docs/`.
- **Docker**: `docker-compose.yml` — PostgreSQL 16 для разработки. Запуск: **`npm run docker:up`** (или `docker compose up -d`). Остановка: `npm run docker:down`. Параметры БД совпадают с дефолтами backend: хост `localhost`, порт `5432`, БД/логин/пароль `publisher`.

## Backend (`apps/backend`)

- Java **17** (на машине разработки; в ТЗ целевой **21** — см. `DECISIONS.md`).
- Spring Boot **3.3.6**, JPA, Security, JWT (JJWT), Flyway **V1** (`users`, `workspaces`, `memberships`, `categories`, `tags`, `posts`, `post_tags`).
- API: `/api/auth/register`, `/api/auth/login`, `/api/me`, CRUD `/api/posts`, `/api/categories`, POST/GET `/api/tags`, публичное чтение `/api/public/w/{workspaceSlug}/posts` и `.../posts/{postSlug}`.
- Выбор workspace: заголовок **`X-Workspace-Id`** или первый workspace пользователя.
- Тесты: H2 в памяти, уникальный URL с `${random.uuid}`; интеграционные сценарии регистрации/поста/публичной выдачи.
- **БД в разработке**: PostgreSQL из `docker-compose` или свой инстанс; URL/учётка по умолчанию в `application.yml`, при необходимости — env `DATASOURCE_*`.

## Frontend (`apps/web`)

- Маршруты: `/login`, `/register`, `/app/feed`, `/app/posts/new`, `/app/posts/:id`, публично `/blog/:workspaceSlug`, `/blog/:workspaceSlug/p/:postSlug`.
- Стили: Tailwind v4, нейтральная палитра и CSS-переменные, темы **светлая / тёмная / системная**.
- Dev: прокси Vite на `http://localhost:8080` для `/api`.

## Следующие шаги по плану (приоритет)

1. **Инфраструктура репозитория (по желанию)**: добавить `apps/backend/Dockerfile` (как в `DEPLOY_BACKEND.md`) и при необходимости compose-сервис для API, чтобы Coolify собирал без ручного `mvn` на хосте.
2. **Фронт под раздельные домены**: переменная `VITE_API_BASE_URL` + правки `api/client.ts`, если API и SPA на разных origin без общего reverse proxy.
3. **MVP по ТЗ**: медиа (загрузка + S3), интеграции каналов (Telegram/VK), AI и prompt templates, refresh-токены.
4. **Качество API**: стабильная пагинация (DTO вместо сырого `Page`), rate limiting на чувствительных маршрутах.
5. **Фоновые задачи и кэш**: Redis, планировщик (polling метрик, публикации) по спецификации.
6. **Публичный URL автора**: реализация ADR-008 (`/{author_nickname}/...`) вместо `workspace.slug` в путях.

## Бэклог (крупнее)

RSS, расширенная аналитика, миграция на Java 21 при появлении JDK.
