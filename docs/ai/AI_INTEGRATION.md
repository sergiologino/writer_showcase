# Интеграция с noteapp-ai-integration

Сервис **writer_showcase** (Spring Boot) обращается к внешнему **noteapp-ai-integration** за обработкой запросов к нейросетям. Контракт: `POST` с JSON как у `AiRequestDTO`, ответы с полем `status` (успех — `success`), заголовок API-ключа (по умолчанию `X-API-Key`).

Подробности протокола: репозиторий интеграции, файл `docs/ai/EXTERNAL_SERVICES_INTEGRATION.md` (в проекте `noteapp-ai-integration`).

## Переменные окружения (бэкенд Publisher)

Все настройки также задаются в `publisher.integration-ai` в `application.yml`; ниже — **имена переменных окружения** (предпочтительный префикс `AI_INTEGRATION_*`, дубли `INTEGRATION_AI_*` для совместимости).

| Переменная | Назначение | По умолчанию / примечание |
|------------|------------|----------------------------|
| `AI_INTEGRATION_BASE_URL` | Базовый URL сервиса (без завершающего `/`) | Пусто — интеграция **не** настроена (`NOT_CONFIGURED`, список сетей = `[]`). Имеет приоритет над `AI_INTEGRATION_URL`, если заданы оба. |
| `AI_INTEGRATION_URL` | То же, что базовый URL (как в других ваших сервисах) | Пример прод: `https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net` |
| `AI_INTEGRATION_API_KEY` | Секретный ключ клиента (как выдаёт интеграция) | Обязателен вместе с URL для рабочих вызовов. |
| `AI_INTEGRATION_API_KEY_HEADER` | Имя заголовка с ключом | `X-API-Key` |
| `AI_INTEGRATION_PROCESS_PATH` | Путь метода обработки | `/api/ai/process` |
| `AI_INTEGRATION_AVAILABLE_NETWORKS_PATH` | Путь `GET` списка сетей (тот же ключ) | `/api/ai/networks/available` |
| `AI_INTEGRATION_CONNECT_TIMEOUT_MS` | Таймаут установки соединения, мс | `5000` |
| `AI_INTEGRATION_READ_TIMEOUT_MS` | Таймаут чтения ответа, мс | `120000` (долгие генерации) |
| `INTEGRATION_AI_BASE_URL` | Синоним `AI_INTEGRATION_BASE_URL` | Опционально |
| `INTEGRATION_AI_API_KEY` | Синоним `AI_INTEGRATION_API_KEY` | Опционально |
| `INTEGRATION_AI_API_KEY_HEADER` | Синоним для заголовка | Опционально |
| `INTEGRATION_AI_PROCESS_PATH` | Синоним пути process | Опционально |
| `INTEGRATION_AI_CONNECT_TIMEOUT_MS` / `INTEGRATION_AI_READ_TIMEOUT_MS` | Синонимы таймаутов | Опционально |
| `INTEGRATION_AI_AVAILABLE_NETWORKS_PATH` | Синоним пути списка сетей | Опционально |
| `AI_INTEGRATION_SOCIAL_POSTS_PATH` | Путь `POST` кросс-поста в соцсети (тот же URL и `X-API-Key`, что и для нейросетей) | `/api/social/posts` (контракт: `noteapp-ai-integration` → Telegram, Facebook, X) |
| `INTEGRATION_AI_SOCIAL_POSTS_PATH` | Синоним пути social posts | Опционально |

**Публикация в Telegram / Facebook / X (кросс-пост):** при заданных `AI_INTEGRATION_BASE_URL` и `AI_INTEGRATION_API_KEY` Publisher отправляет эти каналы в **noteapp-ai-integration** (`POST` на путь выше), а не напрямую в API платформ. Для Telegram через тот же запрос уходят текст/caption и вложения поста (`attachments[]`: изображения, видео, документы/файлы в Base64). **ВК, ОК, МАКС** — как прежде напрямую с Publisher.

**Админ в Publisher (роль `ADMIN`, API нейросетей):** право даёт либо поле **`users.is_admin`** в БД, либо вхождение email в **`ADMIN_EMAILS`** (как в других сервисах). Без настроенной интеграции **Профиль → Нейросети** покажет пустой справочник; после настройки URL + `api-key` подтягивается список сетей и сохраняется **глобальный** приоритет маршрутизации (таблица `app_ai_routing`).

| Переменная | Назначение | Пример |
|------------|------------|--------|
| `ADMIN_EMAILS` | Список email с `ROLE_ADMIN`, через **запятую** | `admin@domain.com,ops@domain.com` |

**Опциональный bootstrap админа** (только dev / первая настройка, см. `PublisherAdminBootstrap` — создаёт пользователя и/или выставляет `is_admin` в БД):

| Переменная | Назначение | По умолчанию |
|------------|------------|----------------|
| `PUBLISHER_ADMIN_BOOTSTRAP_ENABLED` | Создать/пометить пользователя админом при старте | `false` |
| `PUBLISHER_ADMIN_BOOTSTRAP_EMAIL` | Email учётки | `admin@local.test` |
| `PUBLISHER_ADMIN_BOOTSTRAP_PASSWORD` | Пароль | `admin` |
| `PUBLISHER_ADMIN_BOOTSTRAP_DISPLAY_NAME` | Имя в UI | `Admin` |

## API в Publisher, связанные с ИИ

- `GET/PUT /api/ai/prompts/...` — шаблоны по workspace.
- `POST /api/ai/invoke` — вызов по `promptKey` (нужен `X-Workspace-Id`).
- `POST /api/ai/studio/invoke` — произвольное тело `StudioAiRequest` (нужен workspace; ИИ-студия в редакторе поста).
- `GET /api/ai/admin/available-networks`, `GET/PUT /api/ai/admin/routing` — **только `ROLE_ADMIN`**; workspace для этих путей обязателен так же, как для остального API (заголовок `X-Workspace-Id`).

## Быстрая проверка

1. Поднять **noteapp-ai-integration** с выданным API-ключом; в `.env` Publisher (или Run Configuration) задать `AI_INTEGRATION_BASE_URL` и `AI_INTEGRATION_API_KEY`.
2. Запустить Publisher, залогиниться, выбрать workspace (чтобы `X-Workspace-Id` проставлялся фронтом или вручную).
3. **Список сетей (админ):** `GET /api/ai/admin/available-networks` с Bearer + `X-Workspace-Id` — ожидается JSON-массив (или обёртка, которую фронт разворачивает).
4. **Студия:** в UI открыть пост → **AI-студия** → короткий промпт; при ошибке интеграции в ответе будет код/тело от upstream.

Корневой **`npm run build`** в репозитории: сценарий Vitest + `tsc` + Vite на фронте и **`mvnw verify`** на бэкенде — используйте после изменений конфигурации.
