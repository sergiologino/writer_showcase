# История изменений (AI-память)

Краткие записи (1–3 строки) по итогам задач; без логов и отладочного шума.

- **2026-04-08**: Инициализированы `docs/ai/*`; состояние: только память, код отсутствует; ТЗ сведено из `altacod_publisher_spec.md`, зафиксированы ADR-001 (модульный монолит) и ADR-002 (стек по ТЗ).

- **2026-04-09**: Монорепозиторий MVP: backend (auth JWT, workspaces, posts/categories/tags, публичное API), frontend (вход/регистрация, лента, редактор, публичный блог, темы); Flyway V1; тесты бэкенда и фронта; корневая сборка `npm run build`. ADR-003…006 в `DECISIONS.md`.

- **2026-04-09**: Добавлен `docker-compose.yml` (Postgres 16) и npm-скрипты `docker:*`; ADR-007 (compose), ADR-008 (целевой URL `/{author_nickname}/...` после MVP, пока MVP на `workspace.slug`).

- **2026-04-09**: Добавлены `docs/DEPLOY_BACKEND.md` и `docs/DEPLOY_FRONTEND.md` (IDEA, локальный Postgres, сервер, Coolify); в `CURRENT_STATE.md` — ссылки и приоритизированные следующие шаги.
