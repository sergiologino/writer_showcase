# Altacod Publisher

Платформа для блогеров, писателей и небольших редакций: авторский блог как первоисточник, редактор материалов, медиатека, AI-помощник и публикация в социальные каналы по модели POSSE.

## Состав

- `apps/backend` — Spring Boot API, PostgreSQL, Flyway, JWT, публичное API блога, интеграции с AI и каналами.
- `apps/web` — Vite + React SPA, публичный лендинг, кабинет автора, публичные блоги и статьи.
- `docker-compose.yml` — локальные PostgreSQL, Redis и опционально backend.

## Локальная проверка

```text
npm run docker:up
npm run build
```

Фронт отдельно:

```text
cd apps/web
npm install
npm run dev
```

Backend отдельно:

```text
cd apps/backend
mvnw.cmd spring-boot:run
```

## Деплой в Coolify

Рекомендуемая схема — два приложения и managed services в одной сети Coolify:

1. PostgreSQL 16.
2. Redis 7, если нужна надёжная очередь фоновой публикации.
3. Backend из `apps/backend/Dockerfile`, порт контейнера `8080`.
4. Frontend из `apps/web/Dockerfile`, порт контейнера `80`; `API_UPSTREAM` указывает на внутренний URL backend.

Для публичного сайта удобнее один домен на frontend-контейнере: он отдаёт SPA, `/health`, проксирует `/api/*`, `/robots.txt` и `/sitemap.xml` в backend.

## Переменные окружения Coolify

Backend:

```text
DATASOURCE_URL=jdbc:postgresql://<postgres-host>:5432/<db>
DATASOURCE_USERNAME=<db-user>
DATASOURCE_PASSWORD=<db-password>
JWT_SECRET=<long-random-secret-32-bytes-or-more>
PUBLIC_SITE_BASE_URL=https://<your-domain>
PUBLISHER_CORS_ALLOWED_ORIGINS_0=https://<your-domain>
STORAGE_LOCAL_ROOT=/data/storage
PUBLISHER_REDIS_ENABLED=true
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password-if-any>
AI_INTEGRATION_BASE_URL=https://<ai-integration-domain>
AI_INTEGRATION_API_KEY=<client-api-key>
ADMIN_EMAILS=<admin@example.com>
```

Опционально для backend:

```text
SERVER_PORT=8080
JWT_ACCESS_TTL_MINUTES=60
JWT_REFRESH_TTL_DAYS=30
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_PER_MINUTE=40
RATE_LIMIT_API_PER_MINUTE=400
MULTIPART_MAX_FILE_SIZE=50MB
MULTIPART_MAX_REQUEST_SIZE=55MB
STORAGE_MAX_UPLOAD_BYTES=52428800
AI_INTEGRATION_SOCIAL_POSTS_PATH=/api/social/posts
AI_INTEGRATION_PROCESS_PATH=/api/ai/process
AI_INTEGRATION_AVAILABLE_NETWORKS_PATH=/api/ai/networks/available
AI_INTEGRATION_CONNECT_TIMEOUT_MS=5000
AI_INTEGRATION_READ_TIMEOUT_MS=120000
PUBLISHER_OAUTH2_FRONTEND_URL=https://<your-domain>
PUBLISHER_CHANNEL_MAX_ATTEMPTS=8
PUBLISHER_CHANNEL_BACKOFF_BASE_SEC=45
PUBLISHER_CHANNEL_BACKOFF_MAX_SEC=7200
```

Frontend runtime:

```text
API_UPSTREAM=http://<backend-service-name>:8080
NGINX_PORT=80
```

Frontend build arg, только если API на отдельном публичном origin без общего `/api` proxy:

```text
VITE_API_BASE_URL=https://<api-domain>
```

## SEO после деплоя

Проверьте:

- `https://<your-domain>/` — лендинг с `h1`, title, description, keywords и OpenGraph.
- `https://<your-domain>/robots.txt` — разрешает публичные страницы и указывает sitemap.
- `https://<your-domain>/sitemap.xml` — содержит лендинг, публичные блоги `/blog/{workspaceSlug}` и статьи `/blog/{workspaceSlug}/p/{postSlug}`.
- В `PUBLIC_SITE_BASE_URL` указан тот же публичный HTTPS-домен без хвостового слэша.

## Подробная документация

- Backend: `docs/DEPLOY_BACKEND.md`
- Frontend: `docs/DEPLOY_FRONTEND.md`
- Текущее состояние и решения для ассистентов: `docs/ai/*`
