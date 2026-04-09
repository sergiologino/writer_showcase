# Деплой backend (Spring Boot + PostgreSQL)

Артефакт: исполняемый JAR (`publisher-api-0.0.1-SNAPSHOT.jar`). БД: **PostgreSQL** (Flyway применяет миграции при старте). Переменные окружения см. ниже.

## Переменные окружения

| Переменная | Назначение | Пример |
|------------|------------|--------|
| `DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/publisher` |
| `DATASOURCE_USERNAME` | пользователь БД | `publisher` |
| `DATASOURCE_PASSWORD` | пароль | (секрет) |
| `JWT_SECRET` | секрет подписи JWT, **длинная строка** | минимум 32+ байт для HS256 |
| `JWT_ACCESS_TTL_MINUTES` | TTL access-токена | `60` |
| `SERVER_PORT` | порт HTTP | `8080` |
| `publisher.cors.*` (в `application.yml` / профиле) | CORS: по умолчанию паттерны `http://localhost:*` и `http://127.0.0.1:*`. Для продакшена задайте `publisher.cors.allowed-origins` (список точных URL) и при необходимости очистите `allowed-origin-patterns` | см. `application.yml` |

Продакшен: задайте сильный `JWT_SECRET` и уникальные учётные данные БД.

---

## 1. Локально: PostgreSQL + запуск из IntelliJ IDEA

### 1.1. База данных

**Вариант A — Docker (рекомендуется для единообразия):**

```text
npm run docker:up
```

из корня репозитория (см. `docker-compose.yml`).

**Вариант A2 — Postgres и API в Docker (образ из репозитория):**

В корне репозитория есть `apps/backend/Dockerfile` (сборка через Maven внутри образа). Поднять БД и контейнер API:

```text
npm run docker:up:all
```

(эквивалентно `docker compose --profile backend up -d --build`). API будет на **http://localhost:8080**, Postgres по-прежнему на **localhost:5432**. Для продакшена задайте `JWT_SECRET` и настройте **`publisher.cors`** в профиле Spring (см. таблицу выше).

**Вариант B — установленный PostgreSQL на машине:**

Создайте пользователя и БД (имена могут быть любыми, но тогда поправьте env):

```sql
CREATE USER publisher WITH PASSWORD 'publisher';
CREATE DATABASE publisher OWNER publisher;
```

### 1.2. Сборка JAR (при необходимости)

```text
cd apps/backend
mvnw.cmd package -DskipTests
```

JAR: `apps/backend/target/publisher-api-0.0.1-SNAPSHOT.jar`.

### 1.3. IntelliJ IDEA

1. **Open** каталог `writer_showcase` или `apps/backend` как Maven-проект.
2. Найдите класс `io.altacod.publisher.PublisherApplication`.
3. **Run → Edit Configurations → + → Spring Boot** (или запуск по зелёной стрелке у `main`).
4. **Main class:** `io.altacod.publisher.PublisherApplication`.
5. **Working directory:** `apps/backend` (если открыт корень монорепо).
6. Вкладка **Environment variables**, пример:

   ```text
   DATASOURCE_URL=jdbc:postgresql://localhost:5432/publisher;DATASOURCE_USERNAME=publisher;DATASOURCE_PASSWORD=publisher;JWT_SECRET=change-me-to-a-long-random-secret-string-min-32-chars
   ```

7. Запуск: убедитесь, что Postgres слушает тот же хост/порт, что в `DATASOURCE_URL`.

Проверка: откройте в браузере или через curl любой публичный endpoint, например регистрация недоступна без POST, но приложение должно стартовать без ошибок Flyway в логах.

### 1.4. Запуск JAR из терминала (без IDE)

```text
cd apps/backend
java -jar target/publisher-api-0.0.1-SNAPSHOT.jar
```

Переменные задайте в системе или перед командой (синтаксис зависит от ОС).

---

## 2. Деплой на сервере (VPS / домашний сервер)

Общая схема: на машине установлены **JRE 17+** (или JDK), доступен **PostgreSQL** (локально или сеть), JAR запускается как служба (**systemd**, **Windows Service**, или контейнер).

### 2.1. PostgreSQL на сервере

- Создайте БД и пользователя (как в п. 1.1).
- В `DATASOURCE_URL` укажите реальный хост (часто `127.0.0.1`, если приложение и Postgres на одной ВМ).
- Откройте firewall только при необходимости; **не** выставляйте Postgres в интернет без TLS и ACL.

### 2.2. systemd (пример)

Файл `/etc/systemd/system/publisher-api.service` (пути и пользователя подставьте свои):

```ini
[Unit]
Description=Publisher API
After=network.target postgresql.service

[Service]
User=app
WorkingDirectory=/opt/publisher-api
Environment=DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/publisher
Environment=DATASOURCE_USERNAME=publisher
Environment=DATASOURCE_PASSWORD=your-secret
Environment=JWT_SECRET=your-long-jwt-secret
# CORS для продакшена — через publisher.cors в application.yml / профиле (см. таблицу переменных)
ExecStart=/usr/bin/java -jar /opt/publisher-api/publisher-api.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Переименуйте JAR в `publisher-api.jar` или поправьте путь. Затем:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now publisher-api
```

---

## 3. Coolify (домашний сервер или VPS)

Coolify разворачивает приложение из Git и/или Dockerfile. Ниже — два типичных подхода.

### 3.1. Подход A: Dockerfile в репозитории

В проекте уже есть **`apps/backend/Dockerfile`**: многостадийная сборка (`maven:3.9-eclipse-temurin-17-alpine` → `mvn package`, затем JRE с непривилегированным пользователем).

В Coolify укажите:

- **Контекст сборки (Base Directory / Context):** `apps/backend` (или корень репозитория с **Dockerfile path** = `apps/backend/Dockerfile` — в зависимости от UI).
- **Dockerfile path:** `apps/backend/Dockerfile`.

Отдельная команда `mvn package` на хосте не нужна: Maven выполняется внутри образа.

### 3.2. Подход B: только JAR + минимальный образ

При желании соберите JAR в CI, скопируйте в образ только `COPY ...jar` (как в старых минимальных Dockerfile); текущий файл репозитория этому не обязан — он самодостаточен.

### 3.3. База данных в Coolify

1. Создайте **PostgreSQL** как managed service (или отдельный контейнер) в Coolify.
2. Задайте переменные приложения — см. таблицу выше. В Coolify часто есть готовые поля host/port/user/password; соберите из них строку:

   `jdbc:postgresql://HOST:5432/DBNAME`

3. Имя переменных должно совпадать с тем, что читает Spring (в проекте: `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`). Если Coolify отдаёт только `DATABASE_URL` в формате `postgresql://...`, либо добавьте маппинг в настройках, либо вынесите в скрипт запуска преобразование в JDBC URL (отдельная задача при необходимости).

### 3.4. Порт и домен

- Укажите порт контейнера **8080** (или задайте `SERVER_PORT` и проброс в Coolify).
- Включите HTTPS через встроенный reverse proxy Coolify.
- В **`publisher.cors.allowed-origins`** укажите **точный** URL фронтенда (схема + хост + при необходимости порт; без лишнего слэша в конце) и при необходимости отключите шаблоны `localhost` в `allowed-origin-patterns`.

### 3.5. Обновление версии

Новый деплой: push в ветку, которую следит Coolify, либо ручной redeploy после сборки нового JAR/образа. Flyway применит новые миграции при старте (при ошибке валидации миграций приложение не поднимется — смотрите логи).

---

## 4. Чеклист после деплоя

- Логи без ошибок Flyway и подключения к БД.
- `POST /api/auth/register` с хоста фронта или через curl (учитывая CORS для браузера).
- Секреты не в Git; для Coolify — только в UI Secrets / Variables.
