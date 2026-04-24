import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { updateProfile } from '../api/account'
import { ApiError, apiFetch } from '../api/client'
import type { MeResponse, UpdateProfilePayload } from '../api/types'
import { AiAdminSection } from '../components/AiAdminSection'
import { applyTheme, type Theme } from '../lib/theme'

function isTheme(v: string | null | undefined): v is Theme {
  return v === 'light' || v === 'dark' || v === 'system'
}

export function ProfilePage() {
  const queryClient = useQueryClient()
  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<MeResponse>('/api/me'),
  })

  const [displayName, setDisplayName] = useState('')
  const [locale, setLocale] = useState('')
  const [timezone, setTimezone] = useState('')
  const [theme, setTheme] = useState<'light' | 'dark' | 'system' | ''>('')

  useEffect(() => {
    if (!me.data) {
      return
    }
    const u = me.data.user
    setDisplayName(u.displayName)
    setLocale(u.locale ?? '')
    setTimezone(u.timezone ?? '')
    setTheme(isTheme(u.theme) ? u.theme : '')
  }, [me.data])

  const mutation = useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateProfile(payload),
    onSuccess: (data) => {
      queryClient.setQueryData(['me'], data)
      const t = data.user.theme
      if (isTheme(t)) {
        applyTheme(t)
      }
    },
  })

  const error =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error ? 'Не удалось сохранить' : null

  if (me.isLoading) {
    return <p className="text-sm text-[var(--muted)]">Загрузка профиля…</p>
  }
  if (me.isError) {
    return (
      <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
        Не удалось загрузить профиль
      </p>
    )
  }

  const profile = me.data
  if (!profile) {
    return null
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Профиль</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">Имя, локаль и отображение. Email меняется отдельно (пока недоступно).</p>
        <p className="mt-3">
          <Link
            className="rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm font-medium text-[var(--text)] hover:border-[var(--accent)]"
            to="/app/channels"
          >
            Каналы публикации (ВК, ОК, Telegram) — инструкции и поля
          </Link>
        </p>
      </div>

      <form
        className="max-w-lg space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm"
        onSubmit={(ev) => {
          ev.preventDefault()
          const payload: UpdateProfilePayload = {
            displayName: displayName.trim(),
            locale: locale.trim() || null,
            timezone: timezone.trim() || null,
            theme: theme === '' ? null : theme,
          }
          if (!payload.displayName) {
            return
          }
          mutation.mutate(payload)
        }}
      >
        {error ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
            {error}
          </p>
        ) : null}
        {mutation.isSuccess ? (
          <p className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-100">
            Сохранено
          </p>
        ) : null}

        <label className="block text-sm font-medium">
          Email
          <input
            type="email"
            readOnly
            className="mt-1 w-full cursor-not-allowed rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--muted)]"
            value={profile.user.email}
          />
        </label>

        <label className="block text-sm font-medium">
          Отображаемое имя
          <input
            type="text"
            required
            maxLength={255}
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Локаль (например ru, en-US)
          <input
            type="text"
            maxLength={32}
            placeholder="ru"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={locale}
            onChange={(e) => setLocale(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Часовой пояс (IANA, например Europe/Moscow)
          <input
            type="text"
            maxLength={64}
            placeholder="Europe/Moscow"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Тема интерфейса в аккаунте
          <select
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={theme}
            onChange={(e) => setTheme(e.target.value as typeof theme)}
          >
            <option value="">Не задано (как в шапке)</option>
            <option value="system">Как в системе</option>
            <option value="light">Светлая</option>
            <option value="dark">Тёмная</option>
          </select>
        </label>
        <p className="text-xs text-[var(--muted)]">
          Если задано, при сохранении тема применяется сразу. Переключатель «Тема» в шапке по-прежнему хранится локально в браузере.
        </p>

        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-lg bg-[var(--accent)] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-[var(--accent-hover)] disabled:opacity-60"
        >
          {mutation.isPending ? 'Сохранение…' : 'Сохранить'}
        </button>
      </form>

      {profile.user.isAdmin ? <AiAdminSection /> : null}
    </div>
  )
}
