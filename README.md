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

### Backend: обязательные

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `DATASOURCE_URL` | JDBC-строка подключения Spring Boot к PostgreSQL. Flyway применяет миграции в эту БД при старте. | `jdbc:postgresql://postgres:5432/publisher` |
| `DATASOURCE_USERNAME` | Пользователь PostgreSQL. | `publisher` |
| `DATASOURCE_PASSWORD` | Пароль пользователя PostgreSQL. | хранить только в Coolify Secrets/Variables |
| `JWT_SECRET` | Секрет подписи access/refresh JWT. В продакшене должен быть длинным и случайным. | минимум 32 байта, лучше 64+ символа |
| `PUBLIC_SITE_BASE_URL` | Публичный HTTPS URL сайта без хвостового `/`. Используется для ссылок в каналах, `robots.txt` и `sitemap.xml`. | `https://publisher.example.com` |
| `PUBLISHER_CORS_ALLOWED_ORIGINS_0` | Разрешённый origin фронтенда для браузерных запросов к API. Если фронт и API на одном домене, укажите этот домен. | `https://publisher.example.com` |
| `STORAGE_LOCAL_ROOT` | Папка внутри backend-контейнера для загруженных медиа. В Coolify на неё нужен persistent volume. | `/data/storage` |

### Backend: OAuth через Google и Yandex

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `GOOGLE_CLIENT_ID` | Client ID приложения Google OAuth. Если вместе с secret не задан, кнопка Google на фронте скрывается. | значение из Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | Client Secret приложения Google OAuth. | secret из Google Cloud Console |
| `YANDEX_CLIENT_ID` | Client ID приложения Yandex OAuth. Если вместе с secret не задан, кнопка Yandex на фронте скрывается. | значение из Yandex OAuth app |
| `YANDEX_CLIENT_SECRET` | Client Secret приложения Yandex OAuth. | secret из Yandex OAuth app |
| `PUBLISHER_OAUTH2_FRONTEND_URL` | URL фронтенда, куда backend редиректит после успешного OAuth с токенами во fragment (`/auth/callback#...`). Должен совпадать с пользовательским URL SPA. | `https://publisher.example.com` |

Redirect URI в кабинетах провайдеров должен указывать на backend:

```text
Google: https://<backend-public-domain>/login/oauth2/code/google
Yandex: https://<backend-public-domain>/login/oauth2/code/yandex
```

Если backend открыт на том же домене, что и frontend, используйте этот домен. Если backend в Coolify имеет отдельный домен, используйте домен backend.

### Backend: AI-интеграция и соцпубликация

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `AI_INTEGRATION_BASE_URL` | Базовый URL сервиса `noteapp-ai-integration`. Нужен для AI-запросов и публикации в Telegram/Facebook/X через `/api/social/posts`. | `https://ai.example.com` |
| `AI_INTEGRATION_API_KEY` | API key клиента в `noteapp-ai-integration`; отправляется в заголовке `X-API-Key`. | secret из ai-integration |
| `AI_INTEGRATION_API_KEY_HEADER` | Имя заголовка API key, если отличается от стандартного. | обычно `X-API-Key` |
| `AI_INTEGRATION_PROCESS_PATH` | Путь обработки AI-запросов. | `/api/ai/process` |
| `AI_INTEGRATION_AVAILABLE_NETWORKS_PATH` | Путь получения списка доступных нейросетей для админского UI. | `/api/ai/networks/available` |
| `AI_INTEGRATION_SOCIAL_POSTS_PATH` | Путь публикации постов в соцсети через ai-integration. | `/api/social/posts` |
| `AI_INTEGRATION_CONNECT_TIMEOUT_MS` | Timeout подключения к ai-integration. | `5000` |
| `AI_INTEGRATION_READ_TIMEOUT_MS` | Timeout ожидания ответа от ai-integration. Для нейросетей и соцсетей лучше держать больше обычного. | `120000` |
| `AI_INTEGRATION_ENSURE_CLIENT_USER_LINK` | При старте пытается связать клиента ai-integration с техническим пользователем. | `true` |
| `AI_INTEGRATION_ADMIN_USERNAME` | Admin login для bootstrap-связки в ai-integration. | задавать, если включён `ENSURE_CLIENT_USER_LINK` |
| `AI_INTEGRATION_ADMIN_PASSWORD` | Admin password для bootstrap-связки в ai-integration. | secret |
| `AI_INTEGRATION_ASSIGN_USER_EMAIL` | Email технического пользователя в ai-integration. | `publisher-builtin@integration.local` |
| `AI_INTEGRATION_ASSIGN_USER_NAME` | Отображаемое имя технического пользователя. | `Publisher (auto)` |
| `AI_INTEGRATION_CLIENT_ID` | Опциональный UUID клиента ai-integration, если не хотите искать клиента по API key. | можно оставить пустым |

### Backend: Redis и фоновые публикации

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `PUBLISHER_REDIS_ENABLED` | Включает Redis-очередь фоновой публикации. Для продакшена лучше `true`; без Redis используется in-memory очередь одного инстанса. | `true` |
| `REDIS_HOST` | Host Redis внутри сети Coolify. | `redis` или hostname managed Redis |
| `REDIS_PORT` | Порт Redis. | `6379` |
| `REDIS_PASSWORD` | Пароль Redis, если задан. | можно пусто для локального Redis |
| `PUBLISHER_CHANNEL_POLL_MS` | Частота обработки очереди исходящих публикаций. | `2000` |
| `PUBLISHER_CHANNEL_RETRY_POLL_MS` | Частота проверки задач на retry. | `45000` |
| `PUBLISHER_CHANNEL_MAX_ATTEMPTS` | Максимум попыток доставки в канал. | `8` |
| `PUBLISHER_CHANNEL_BACKOFF_BASE_SEC` | Базовая задержка retry. | `45` |
| `PUBLISHER_CHANNEL_BACKOFF_MAX_SEC` | Максимальная задержка retry. | `7200` |
| `PUBLISHER_CHANNEL_BACKOFF_JITTER_PCT` | Jitter для retry, чтобы не бить внешние API пачкой. | `25` |
| `PUBLISHER_CHANNEL_RETRY_BATCH` | Сколько задач брать за один проход retry-планировщика. | `40` |

### Backend: лимиты, загрузки, админы

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `SERVER_PORT` | HTTP-порт Spring Boot внутри контейнера. | `8080` |
| `JWT_ACCESS_TTL_MINUTES` | Время жизни access-токена. | `60` |
| `JWT_REFRESH_TTL_DAYS` | Время жизни refresh-токена. | `30` |
| `RATE_LIMIT_ENABLED` | Включает rate limiting API. | `true` |
| `RATE_LIMIT_AUTH_PER_MINUTE` | Лимит login/register/refresh в минуту на IP. | `40` |
| `RATE_LIMIT_API_PER_MINUTE` | Общий API-лимит в минуту на IP. | `400` |
| `MULTIPART_MAX_FILE_SIZE` | Максимальный размер одного multipart-файла на уровне Spring. | `50MB` |
| `MULTIPART_MAX_REQUEST_SIZE` | Максимальный размер multipart-запроса. | `55MB` |
| `STORAGE_MAX_UPLOAD_BYTES` | Прикладной лимит размера загружаемого файла в байтах. | `52428800` |
| `ADMIN_EMAILS` | Список email, которые получают глобальную админ-роль, через запятую. | `admin@example.com,owner@example.com` |
| `PUBLISHER_HTTP_USE_SYSTEM_PROXIES` | Использовать системные proxy-настройки JVM для исходящих запросов. На Linux-сервере обычно не нужно. | `false` |
| `PUBLISHER_HTTP_PREFER_IPV4` | Предпочитать IPv4 для исходящих запросов, если есть сетевые проблемы с IPv6. | `false` |

### Frontend runtime

Эти переменные задаются в frontend-контейнере `apps/web/Dockerfile` на runtime:

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `API_UPSTREAM` | Внутренний URL backend-сервиса, куда Nginx проксирует `/api/*`, `/robots.txt`, `/sitemap.xml`. | `http://publisher-api:8080` |
| `NGINX_PORT` | Порт Nginx внутри frontend-контейнера. | `80` |

### Frontend build args

Эти значения нужны во время сборки SPA. В Coolify задаются как build variables/args.

| Переменная | Для чего нужна | Пример / комментарий |
|------------|----------------|----------------------|
| `VITE_API_BASE_URL` | Абсолютный URL API, если фронт ходит на отдельный backend-origin без общего `/api` proxy. Если используете один домен и `API_UPSTREAM`, оставьте пустым. | `https://api.example.com` |
| `VITE_OAUTH_BASE_URL` | Абсолютный URL backend для перехода на `/oauth2/authorization/google` и `/oauth2/authorization/yandex`. В продакшене обязательно задайте, если backend не доступен на `http://localhost:8080`. | `https://api.example.com` или `https://publisher.example.com` |

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
