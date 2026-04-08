# Деплой frontend (Vite + React)

Результат сборки: статические файлы в **`apps/web/dist`**.

По умолчанию клиент ходит в API по **относительным** путям `/api/...` (тот же origin, что у страницы, или прокси Vite в dev).

**Раздельные домены:** перед сборкой задайте `VITE_API_BASE_URL` (см. `apps/web/.env.example`), например `https://api.example.com` **без** хвостового слэша. Тогда запросы уйдут на этот хост; в настройках backend укажите реальный origin фронта в `CORS_ORIGINS`.

Рекомендуемый продакшен-вариант при одном домене: `https://app.example.com` — статика + reverse proxy для `/api/*` → Spring Boot.

---

## 1. Локальная разработка

```text
cd apps/web
npm install
npm run dev
```

По умолчанию Vite: **http://localhost:5173**. Прокси в `vite.config.ts` пересылает `/api` на `http://localhost:8080`. Backend должен быть запущен отдельно (см. `docs/DEPLOY_BACKEND.md`).

Сборка без публикации:

```text
npm run build
```

Проверка превью:

```text
npm run preview
```

Превью **не** настраивает прокси на 8080 автоматически; для полной проверки API локально удобнее `dev` или связка nginx (см. ниже).

---

## 2. Локальный «как в проде»: статика + backend

1. Соберите фронт: `npm run build` в `apps/web`.
2. Поднимите backend на `8080`.
3. Настройте Nginx (или Caddy) на одном порту, например 8081:

   - `location /` → root `.../apps/web/dist`, `try_files $uri $uri/ /index.html` (SPA fallback);
   - `location /api/` → `proxy_pass http://127.0.0.1:8080/`.

Так вы проверяете CORS и пути так же, как на сервере.

---

## 3. Деплой на сервере (статика + reverse proxy)

### 3.1. Только статика

Скопируйте содержимое `dist` на хостинг статики (S3+CloudFront, GitHub Pages **не** подойдёт без отдельного API на том же домене из-за относительных `/api`).

### 3.2. Nginx (пример фрагмента)

```nginx
server {
    server_name app.example.com;
    root /var/www/publisher-web/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

После правок: `nginx -t`, перезагрузка Nginx. Backend слушает внутренний адрес (здесь `127.0.0.1:8080`).

---

## 4. Coolify

### 4.1. Один сайт: статика + прокси API

Зависит от возможностей вашей версии Coolify:

1. **Static site** (или аналог):  
   - Корень репозитория или подкаталог **`apps/web`**.  
   - Команда сборки: `npm ci && npm run build`.  
   - Публикуемая папка: **`dist`**.

2. Если Coolify позволяет добавить **маршрут / прокси** для пути `/api` на внутренний сервис backend — включите его и укажите URL контейнера/сервиса API (порт 8080).

3. Если отдельного «объединяющего» ресурса нет: поднимите **один** публичный сервис с образом **Nginx** (или Caddy), где в контейнер кладёте `dist` и конфиг как в п. 3.2, а `proxy_pass` указывает на hostname сервиса backend в docker-сети Coolify (например `http://publisher-api:8080`).

### 4.2. Два ресурса: только фронт и только API

- Фронт: static, домен `app.example.com`.  
- API: домен `api.example.com`.

Без доработки кода фронт будет стучаться в `app.example.com/api`, а не в `api.example.com`. Варианты:

- вынести фронт за тот же домен, что и API, через gateway (предпочтительно);
- или добавить в проект переменную **`VITE_API_BASE_URL`** и использовать её в `fetch` (отдельная задача на код).

Для MVP ориентируйтесь на **один домен и прокси `/api`**.

### 4.3. Переменные сборки

- **`VITE_API_BASE_URL`** — если API на другом origin; иначе оставьте пустым и используйте общий домен с proxy.
- **`CORS_ORIGINS`** на backend должен совпадать с URL, с которого открывается SPA.

Другие `VITE_*` при необходимости добавляются позже (аналитика и т.д.).

### 4.4. Кэш и SPA

Для HTML с `index.html` обычно отключают агрессивный кэш или короткий `Cache-Control`, а для файлов с хэшем в имени (`assets/*.js`) — длинный кэш. Coolify/ Nginx defaults при необходимости подстройте вручную.

---

## 5. Чеклист

- `npm run build` проходит без ошибок.
- Открытие корня сайта отдаёт SPA; прямой заход на `/app/feed` не даёт 404 на сервере (настроен fallback на `index.html`).
- Запросы из браузера к `/api/...` доходят до Spring Boot (вкладка Network).
- В настройках backend указан тот же origin в `CORS_ORIGINS`, что и у пользовательского URL фронта.
