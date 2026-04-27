import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchWorkspaceChannels, upsertWorkspaceChannel } from '../api/channels'
import type { ChannelType } from '../api/types'
import { Seo } from '../components/Seo'

function displayValue(v: unknown): string {
  if (v === null || v === undefined) {
    return ''
  }
  const s = String(v)
  return s === '***' ? '' : s
}

function parseConfig(configMasked: string): Record<string, string> {
  try {
    const o = JSON.parse(configMasked) as Record<string, unknown>
    const out: Record<string, string> = {}
    for (const [k, v] of Object.entries(o)) {
      out[k] = displayValue(v)
    }
    return out
  } catch {
    return {}
  }
}

export function PublishingChannelsPage() {
  const qc = useQueryClient()
  const channelsQ = useQuery({
    queryKey: ['channels'],
    queryFn: fetchWorkspaceChannels,
  })

  const [tgEnabled, setTgEnabled] = useState(false)
  const [tgLabel, setTgLabel] = useState('')
  const [tgBotToken, setTgBotToken] = useState('')
  const [tgChatId, setTgChatId] = useState('')

  const [vkEnabled, setVkEnabled] = useState(false)
  const [vkLabel, setVkLabel] = useState('')
  const [vkToken, setVkToken] = useState('')
  const [vkGroupId, setVkGroupId] = useState('')

  const [okEnabled, setOkEnabled] = useState(false)
  const [okLabel, setOkLabel] = useState('')
  const [okAppKey, setOkAppKey] = useState('')
  const [okAppSecret, setOkAppSecret] = useState('')
  const [okToken, setOkToken] = useState('')
  const [okGroupId, setOkGroupId] = useState('')

  const [maxEnabled, setMaxEnabled] = useState(false)
  const [maxLabel, setMaxLabel] = useState('')
  const [maxAccessToken, setMaxAccessToken] = useState('')
  const [maxChatId, setMaxChatId] = useState('')

  const [fbEnabled, setFbEnabled] = useState(false)
  const [fbLabel, setFbLabel] = useState('')
  const [fbAccessToken, setFbAccessToken] = useState('')
  const [fbPageId, setFbPageId] = useState('')

  const [xEnabled, setXEnabled] = useState(false)
  const [xLabel, setXLabel] = useState('')
  const [xBearer, setXBearer] = useState('')

  useEffect(() => {
    if (!channelsQ.data) {
      return
    }
    const apply = (type: ChannelType) => {
      const row = channelsQ.data!.find((c) => c.channelType === type)
      const cfg = row ? parseConfig(row.configMasked) : {}
      return { row, cfg }
    }
    {
      const { row, cfg } = apply('TELEGRAM')
      setTgEnabled(row?.enabled ?? false)
      setTgLabel(row?.label ?? '')
      setTgBotToken(cfg.botToken ?? '')
      setTgChatId(cfg.chatId ?? '')
    }
    {
      const { row, cfg } = apply('VK')
      setVkEnabled(row?.enabled ?? false)
      setVkLabel(row?.label ?? '')
      setVkToken(cfg.accessToken ?? '')
      setVkGroupId(cfg.groupId ?? '')
    }
    {
      const { row, cfg } = apply('ODNOKLASSNIKI')
      setOkEnabled(row?.enabled ?? false)
      setOkLabel(row?.label ?? '')
      setOkAppKey(cfg.applicationKey ?? '')
      setOkAppSecret(cfg.applicationSecretKey ?? '')
      setOkToken(cfg.accessToken ?? '')
      setOkGroupId(cfg.groupId ?? '')
    }
    {
      const { row, cfg } = apply('MAX')
      setMaxEnabled(row?.enabled ?? false)
      setMaxLabel(row?.label ?? '')
      setMaxAccessToken(cfg.accessToken ?? '')
      setMaxChatId(cfg.chatId ?? '')
    }
    {
      const { row, cfg } = apply('FACEBOOK')
      setFbEnabled(row?.enabled ?? false)
      setFbLabel(row?.label ?? '')
      setFbAccessToken(cfg.accessToken ?? '')
      setFbPageId(cfg.pageId ?? '')
    }
    {
      const { row, cfg } = apply('X')
      setXEnabled(row?.enabled ?? false)
      setXLabel(row?.label ?? '')
      setXBearer(cfg.bearerToken ?? '')
    }
  }, [channelsQ.data])

  const save = useMutation({
    mutationFn: (args: {
      type: ChannelType
      enabled: boolean
      label: string
      configJson: string
    }) => upsertWorkspaceChannel(args.type, args.enabled, args.label, args.configJson),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channels'] }),
  })

  const err = save.error instanceof ApiError ? save.error.message : save.error ? 'Ошибка сохранения' : null

  return (
    <div className="space-y-10">
      <Seo
        title="Каналы публикации"
        description="Настройки каналов публикации Altacod Publisher: Telegram, VK, Одноклассники, MAX, Facebook и X."
        keywords="каналы публикации, публикация в Telegram, публикация во ВКонтакте, кросспостинг, POSSE"
        canonicalPath="/app/channels"
        noIndex
      />
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Каналы публикации</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Здесь настраивается автоматическая отправка материалов в соцсети при публикации поста (статус «Опубликован» и
          публичная видимость). Поля ниже — всё, что нужно ввести; технический JSON собирается сам. Секреты (токены),
          которые уже сохранены, можно не вводить повторно — оставьте поле пустым. Посты в{' '}
          <strong>Telegram</strong>, <strong>Facebook</strong> и <strong>X</strong> уходят через сервис{' '}
          <strong>noteapp-ai-integration</strong> (те же <code className="rounded bg-[var(--bg)] px-1">AI_INTEGRATION_*</code>, что
          и для нейросетей); Telegram-изображения отправляются через него же.
        </p>
        <p className="mt-2 text-sm">
          <Link className="text-[var(--accent)] hover:underline" to="/app/profile">
            ← Назад в профиль
          </Link>
        </p>
      </div>

      {err ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          {err}
        </p>
      ) : null}

      {channelsQ.isLoading ? <p className="text-sm text-[var(--muted)]">Загрузка…</p> : null}

      {/* Telegram */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Telegram</h2>
        <details className="mt-3 text-sm text-[var(--muted)]">
          <summary className="cursor-pointer font-medium text-[var(--text)]">Как подключить (пошагово)</summary>
          <ol className="mt-3 list-decimal space-y-2 pl-5">
            <li>Откройте Telegram и найдите бота @BotFather.</li>
            <li>
              Отправьте команду <code className="rounded bg-[var(--bg)] px-1">/newbot</code> и следуйте вопросам — в
              конце BotFather пришлёт <strong>токен бота</strong> (длинная строка). Скопируйте её в поле «Токен бота»
              ниже.
            </li>
            <li>
              Создайте канал или группу, куда будут уходить посты. Добавьте своего бота в этот канал/группу{' '}
              <strong>администратором</strong> (права на отправку сообщений).
            </li>
            <li>
              Узнать <strong>chat id</strong>: проще всего написать боту @userinfobot или переслать сообщение из канала
              боту @getidsbot — в ответе будет числовой id (для каналов часто начинается с{' '}
              <code className="rounded bg-[var(--bg)] px-1">-100</code>). Вставьте его в поле «Chat id».
            </li>
            <li>Сохраните настройки здесь и включите канал переключателем.</li>
          </ol>
        </details>

        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={tgEnabled} onChange={(e) => setTgEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (для себя, необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={tgLabel}
              onChange={(e) => setTgLabel(e.target.value)}
              placeholder="Например: мой канал"
            />
          </label>
          <label className="block text-sm font-medium">
            Токен бота
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={tgBotToken}
              onChange={(e) => setTgBotToken(e.target.value)}
              placeholder="Оставьте пустым, если токен уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            Chat id
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={tgChatId}
              onChange={(e) => setTgChatId(e.target.value)}
              placeholder="-100xxxxxxxxxx"
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'TELEGRAM',
                enabled: tgEnabled,
                label: tgLabel.trim() || '',
                configJson: JSON.stringify({
                  botToken: tgBotToken.trim(),
                  chatId: tgChatId.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить Telegram'}
          </button>
        </div>
      </section>

      {/* VK */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">ВКонтакте</h2>
        <details className="mt-3 text-sm text-[var(--muted)]">
          <summary className="cursor-pointer font-medium text-[var(--text)]">Как подключить (пошагово)</summary>
          <ol className="mt-3 list-decimal space-y-2 pl-5">
            <li>
              Создайте сообщество ВК или откройте существующее, от имени которого будут идти посты (вы — редактор или
              администратор).
            </li>
            <li>
              Нужен <strong>числовой id группы</strong>: откройте сообщество → «Управление» → «Адрес страницы» или
              посмотрите ссылку вида <code className="rounded bg-[var(--bg)] px-1">club123456789</code> — число после{' '}
              <code className="rounded bg-[var(--bg)] px-1">club</code> и есть id (только цифры, без минуса).
            </li>
            <li>
              Нужен <strong>ключ доступа пользователя</strong> (access token) с правами на публикацию записей от имени
              сообщества. Обычно это делается через создание приложения ВК (тип «Веб-сайт» или «Standalone») и
              авторизацию пользователя с нужными scope (в т.ч. доступ к стене группы). Подробные шаги зависят от
              интерфейса ВК; при необходимости воспользуйтесь официальной справкой «Работа с API» в настройках
              сообщества.
            </li>
            <li>Вставьте токен и id группы в поля ниже и сохраните.</li>
          </ol>
        </details>

        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={vkEnabled} onChange={(e) => setVkEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={vkLabel}
              onChange={(e) => setVkLabel(e.target.value)}
            />
          </label>
          <label className="block text-sm font-medium">
            Ключ доступа (access token)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={vkToken}
              onChange={(e) => setVkToken(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            ID группы (только цифры)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={vkGroupId}
              onChange={(e) => setVkGroupId(e.target.value.replace(/\D/g, ''))}
              placeholder="123456789"
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'VK',
                enabled: vkEnabled,
                label: vkLabel.trim() || '',
                configJson: JSON.stringify({
                  accessToken: vkToken.trim(),
                  groupId: vkGroupId.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить ВКонтакте'}
          </button>
        </div>
      </section>

      {/* OK */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Одноклассники</h2>
        <details className="mt-3 text-sm text-[var(--muted)]">
          <summary className="cursor-pointer font-medium text-[var(--text)]">Как подключить (пошагово)</summary>
          <ol className="mt-3 list-decimal space-y-2 pl-5">
            <li>
              Зарегистрируйте приложение в{' '}
              <a className="text-[var(--accent)] hover:underline" href="https://ok.ru/devaccess" target="_blank" rel="noreferrer">
                разделе для разработчиков OK
              </a>
              . После модерации вам будут доступны публичный ключ приложения и секретный ключ.
            </li>
            <li>
              Получите <strong>токен доступа</strong> (access token) для пользователя, который может публиковать от
              имени нужной группы (OAuth; набор прав зависит от типа приложения).
            </li>
            <li>
              <strong>ID группы</strong> — из адреса группы в Одноклассниках (числовой идентификатор в URL или в
              настройках группы).
            </li>
            <li>Введите четыре значения ниже и сохраните. Публикация возможна только если приложение OK одобрено для
              нужных методов API.
            </li>
          </ol>
        </details>

        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={okEnabled} onChange={(e) => setOkEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={okLabel}
              onChange={(e) => setOkLabel(e.target.value)}
            />
          </label>
          <label className="block text-sm font-medium">
            Публичный ключ приложения (application key)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={okAppKey}
              onChange={(e) => setOkAppKey(e.target.value)}
            />
          </label>
          <label className="block text-sm font-medium">
            Секретный ключ приложения (application secret)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={okAppSecret}
              onChange={(e) => setOkAppSecret(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            Токен доступа (access token)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={okToken}
              onChange={(e) => setOkToken(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            ID группы
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={okGroupId}
              onChange={(e) => setOkGroupId(e.target.value.trim())}
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'ODNOKLASSNIKI',
                enabled: okEnabled,
                label: okLabel.trim() || '',
                configJson: JSON.stringify({
                  applicationKey: okAppKey.trim(),
                  applicationSecretKey: okAppSecret.trim(),
                  accessToken: okToken.trim(),
                  groupId: okGroupId.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить Одноклассники'}
          </button>
        </div>
      </section>

      {/* Facebook */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Facebook (страница)</h2>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Публикация идёт через <strong>noteapp-ai-integration</strong> (Graph API на стороне интеграции). Нужны page access
          token с правом публикации на стене и числовой id страницы.
        </p>
        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={fbEnabled} onChange={(e) => setFbEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={fbLabel}
              onChange={(e) => setFbLabel(e.target.value)}
            />
          </label>
          <label className="block text-sm font-medium">
            Access token (страницы)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={fbAccessToken}
              onChange={(e) => setFbAccessToken(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            ID страницы (page id)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={fbPageId}
              onChange={(e) => setFbPageId(e.target.value.trim())}
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'FACEBOOK',
                enabled: fbEnabled,
                label: fbLabel.trim() || '',
                configJson: JSON.stringify({
                  accessToken: fbAccessToken.trim(),
                  pageId: fbPageId.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить Facebook'}
          </button>
        </div>
      </section>

      {/* X (Twitter) */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">X (Twitter)</h2>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Публикация через <strong>noteapp-ai-integration</strong> (Twitter API v2, заголовок Bearer). Токен с правом
          <code className="mx-1 rounded bg-[var(--bg)] px-1">tweets.write</code> (или аналог для вашего типа приложения).
        </p>
        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={xEnabled} onChange={(e) => setXEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={xLabel}
              onChange={(e) => setXLabel(e.target.value)}
            />
          </label>
          <label className="block text-sm font-medium">
            Bearer token (OAuth 2.0)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={xBearer}
              onChange={(e) => setXBearer(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'X',
                enabled: xEnabled,
                label: xLabel.trim() || '',
                configJson: JSON.stringify({
                  bearerToken: xBearer.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить X'}
          </button>
        </div>
      </section>

      {/* МАКС (MAX) */}
      <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Мессенджер МАКС</h2>
        <details className="mt-3 text-sm text-[var(--muted)]">
          <summary className="cursor-pointer font-medium text-[var(--text)]">Как подключить (пошагово)</summary>
          <ol className="mt-3 list-decimal space-y-2 pl-5">
            <li>
              В{' '}
              <a
                className="text-[var(--accent)] hover:underline"
                href="https://business.max.ru/self"
                target="_blank"
                rel="noreferrer"
              >
                кабинете MAX для бизнеса
              </a>{' '}
              создайте чат-бота и получите токен: раздел <strong>Чат-боты → Интеграция</strong> (см.{' '}
              <a
                className="text-[var(--accent)] hover:underline"
                href="https://dev.max.ru/docs-api"
                target="_blank"
                rel="noreferrer"
              >
                документацию API
              </a>
              ).
            </li>
            <li>
              Убедитесь, что бот может писать в нужный <strong>чат</strong> (группа, канал или чат) — бот обычно
              должен быть участником с правом отправки сообщений.
            </li>
            <li>
              Узнайте <strong>числовой id чата</strong> (chat_id), в который бот публикует объявления. Его передают в
              Bot API в параметре <code className="rounded bg-[var(--bg)] px-1">chat_id</code> (потребуется для поля
              ниже; часто id виден в событиях webhook при первом сообщении).
            </li>
            <li>Сохраните токен и id чата здесь, включите канал — при публикации поста в блог текст уйдёт в этот чат.</li>
          </ol>
        </details>

        <div className="mt-4 space-y-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={maxEnabled} onChange={(e) => setMaxEnabled(e.target.checked)} />
            Канал включён
          </label>
          <label className="block text-sm font-medium">
            Подпись (необязательно)
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
              value={maxLabel}
              onChange={(e) => setMaxLabel(e.target.value)}
              placeholder="Например: анонс в МАКС"
            />
          </label>
          <label className="block text-sm font-medium">
            Токен бота (access token)
            <input
              type="password"
              autoComplete="off"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={maxAccessToken}
              onChange={(e) => setMaxAccessToken(e.target.value)}
              placeholder="Оставьте пустым, если уже сохранён"
            />
          </label>
          <label className="block text-sm font-medium">
            Chat id
            <input
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              value={maxChatId}
              onChange={(e) => setMaxChatId(e.target.value.replace(/[^\d-]/g, ''))}
              placeholder="Число из MAX Bot API"
            />
          </label>
          <button
            type="button"
            disabled={save.isPending}
            className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            onClick={() =>
              save.mutate({
                type: 'MAX',
                enabled: maxEnabled,
                label: maxLabel.trim() || '',
                configJson: JSON.stringify({
                  accessToken: maxAccessToken.trim(),
                  chatId: maxChatId.trim(),
                }),
              })
            }
          >
            {save.isPending ? 'Сохранение…' : 'Сохранить МАКС'}
          </button>
        </div>
      </section>

    </div>
  )
}
