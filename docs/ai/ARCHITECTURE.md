# Архитектура

## Целевой подход (из ТЗ)

**Модульный монолит** на backend: единая кодовая база и деплой, границы пакетов по доменам (`io.altacod.publisher`: user, workspace, post, category, tag, security, api, …).

## Стек (факт + цель)

| Слой | Реализовано сейчас | Цель по ТЗ |
|------|-------------------|------------|
| Backend | Spring Boot 3.3, Java 17, PostgreSQL (prod), Flyway, JWT, без Redis/S3 пока | Java 21+, Redis, S3, планировщик job-ов |
| Frontend | React 18, Vite, TS, Tailwind v4, Router, Query, RHF, Zod; TipTap/Recharts — позже | Как в ТЗ |

## Репозиторий

**Монорепозиторий**: корень — `npm run build`; артефакты: `apps/web/dist`, `apps/backend/target/*.jar`. Локальная PostgreSQL — `docker-compose.yml` (`npm run docker:up`).

## Взаимодействие

- SPA → REST `/api/*`, JWT в `Authorization: Bearer`, опционально `X-Workspace-Id`.
- Публичный блог: UI `/blog/...`, данные через **`/api/public/w/{workspaceSlug}/posts`** (только `PUBLIC` + `PUBLISHED`).

## Публичная часть (текущее)

- Список и карточка поста по slug workspace + slug поста (мультитенантность по `workspace.slug`).
