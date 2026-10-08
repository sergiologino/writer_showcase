# История изменений (AI-память)

Краткие записи (1–3 строки) по итогам задач; без логов и отладочного шума.

- **2026-10-08**: У полей поста добавлены кнопки AI-помощника; студия получила понятные подсказки, редактируемые промпты по умолчанию, референс с инструкцией и действие «Готово». Ответ изображения переносится через Publisher backend, сохраняется локально перед загрузкой в медиа; для референса используется `image_edit`. Добавлены тесты UI, локального порядка сохранения и контракта изображения.

- **2026-10-08**: Из AI-студии убрано техническое пояснение, режим «Части (2.2)» переименован в «Разбить на посты». В медиатеке добавлены превью изображений и видео через авторизованный файловый endpoint; добавлены UI-тесты.

- **2026-10-08**: Автозапуск PostgreSQL закреплён в Git интеграции (`3fb2a95`) через Compose override и применён на NKTelecom без пересоздания сервисов. Из действующего API-контейнера Publisher получен список из 5 доступных нейросетей.

- **2026-10-08**: По отдельному разрешению владельца включён автозапуск PostgreSQL интеграции (`unless-stopped`) в работающем контейнере и production compose. Compose валиден, оба контейнера healthy, внешний health `200 UP`; прочие сервисы не перезапускались.

- **2026-10-08**: С разрешения владельца запущен остановленный PostgreSQL интеграции на NKTelecom. AI backend восстановился автоматически; контейнеры healthy, публичный health `200 UP`, запрос списка сетей с ключом Publisher `200 OK`. Автозапуск БД после reboot остаётся отдельной задачей.

- **2026-10-08**: Через SSH на NKTelecom локализован `502`: Caddy исправно направляет на `127.0.0.1:8091`, но PostgreSQL интеграции остановлен после перезагрузки сервера 5 октября (`Exited 255`, без автозапуска), а AI backend циклически падает на отсутствии hostname `postgres`. Данные БД находятся в Docker volume; изменений на сервере не выполнялось.

- **2026-10-08**: Уточнён маршрут AI-интеграции: домен `aintegration.altacod.com` ведёт на отдельный сервер NKTelecom (`108.165.189.60`), не на Timeweb (`89.223.66.64`); Caddy Timeweb не содержит этого домена. Публичный Caddy NKTelecom отдаёт `502` для корня, health и API; для точной диагностики нужен read-only доступ к его Caddy и backend.

- **2026-10-08**: Проверено из домашнего сервера Publisher: API-контейнер настроен на HTTPS-домен интеграции, тот отвечает `502`, а прямой TCP на NKTelecom:8091 недоступен. В production-документации интеграции порт опубликован только на loopback; без отдельного сетевого маршрута Publisher не может обращаться к нему напрямую.

- **2026-10-08**: Исправлена диагностика списка сетей: Publisher больше не маскирует сбой интеграции пустым массивом, админский экран показывает ошибку. Добавлены тесты контракта и ошибки; внешний Caddy по-прежнему отдаёт `502` на всех проверенных маршрутах, требуется проверка его upstream на отдельном сервере.

- **2026-10-08**: В production `publisher-compose` исправлен ошибочный домен `AI_INTEGRATION_BASE_URL` на `https://aintegration.altacod.com`; деплой успешен, значение подтверждено в новом API-контейнере. Внешний маршрут интеграции пока возвращает `502`, поэтому список сетей остаётся недоступен; облачный сервис не менялся.

- **2026-10-07**: В админском разделе «Нейросети» ручной ввод имён заменён выбором из доступных приложению сетей по `networkType`. Админ задаёт порядок кнопками; сохранённые сети, исчезнувшие из справочника, остаются видимыми до явного удаления. Добавлены UI-тесты выбора и сохранения приоритетов.

- **2026-10-07**: Локальный архив переведён на `publisher/<localId>/{content.md,media/,parameters.json}`: выбранные медиа сохраняются до загрузки на сервер, серверные вложения копируются при сохранении; в профиле добавлен просмотр папки. Backend хранит и отдаёт `sentAt` для параметров отправки в каналы (Flyway V12). Восстановление после переустановки отложено.

- **2026-10-07**: Первый срез локального первоисточника: редактор пишет Markdown в выбранную папку и материал в IndexedDB до отправки на backend, автосохраняет текст, показывает локальные материалы и восстанавливает базу из папки. Добавлены тесты локального формата и порядка сохранения; вложения и фоновая синхронизация остаются в TODO.

- **2026-10-07**: Локальный первоисточник поста поставлен первым приоритетом: пользовательская папка с читаемыми файлами и локальная база, затем синхронизация с сервером и устройствами. Зафиксированы ADR-012, этапы и критерии приёмки; реализация ещё не начата.

- **2026-10-07**: Уточнён деплой Publisher в Coolify: `myposts.pro` получает frontend из `publisher-compose`, а отдельный ресурс `publisher-front` его не обновляет. После деплоя compose на коммите `25132db` публичная JS-сборка содержит новый текст и инструкции Facebook/X.

- **2026-10-07**: На странице каналов добавлены пошаговые инструкции Facebook Page и X с пояснением нужных токенов; пользовательское описание доставки Telegram/Facebook/X заменено на общее описание промежуточного сервиса. Добавлен UI-тест инструкций и текста.

- **2026-10-07**: Исправлен старт Docker-фронта при недоступном hostname API: Nginx разрешает `API_UPSTREAM` при запросе, поэтому статика и `/health` доступны независимо от состояния backend. Для proxy в Coolify по-прежнему требуется задать реальный hostname API в общей Docker-сети.

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

- **2026-04-26**: Telegram-кросспост полностью переведён на **noteapp-ai-integration** `/api/social/posts`: Publisher передаёт текст/caption и все вложения поста в `attachments[]` (Base64), прямой fallback на Telegram Bot API для текста/альбомов убран. Добавлен контрактный тест `HttpIntegrationSocialClientTest`.

- **2026-04-27**: Для `apps/web` добавлен production Dockerfile: сборка Vite в Node 22 и runtime на Nginx с SPA fallback, `/health`, кэшем assets и proxy `/api` на `API_UPSTREAM`. Обновлён `DEPLOY_FRONTEND.md`.

- **2026-04-27**: Добавлен публичный лендинг `/` для блогеров/писателей, SEO-компонент для страниц SPA, backend endpoints `/robots.txt` и `/sitemap.xml` с публичными блогами/статьями; Nginx фронта проксирует SEO-файлы на API. В корень добавлен `README.md` с инструкцией Coolify и env.

- **2026-04-27**: Расширена деплойная документация env для Coolify: подробно описаны backend/frontend переменные, Google/Yandex OAuth (`GOOGLE_CLIENT_*`, `YANDEX_CLIENT_*`, `PUBLISHER_OAUTH2_FRONTEND_URL`, `VITE_OAUTH_BASE_URL`) и redirect URI провайдеров.
