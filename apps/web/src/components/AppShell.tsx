import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ApiError, apiFetch, clearSession } from '../api/client'
import type { MeResponse } from '../api/types'
import { applyTheme, getStoredTheme, type Theme } from '../lib/theme'
import { useEffect, useMemo, useState } from 'react'

export function AppShell() {
  const navigate = useNavigate()
  const [themeChoice, setThemeChoice] = useState<Theme>(() => getStoredTheme())

  useEffect(() => {
    applyTheme(themeChoice)
  }, [themeChoice])

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => {
      if (getStoredTheme() === 'system') {
        applyTheme('system')
      }
    }
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<MeResponse>('/api/me'),
    retry: false,
  })

  useEffect(() => {
    if (!me.isError || !me.error) {
      return
    }
    const status = me.error instanceof ApiError ? me.error.status : 0
    if (status === 401 || status === 403) {
      clearSession()
      navigate('/login', { replace: true })
    }
  }, [me.isError, me.error, navigate])

  const workspaceSlug = useMemo(() => {
    const list = me.data?.workspaces
    if (!list?.length) {
      return undefined
    }
    const id = localStorage.getItem('workspaceId')
    if (id) {
      const hit = list.find((w) => String(w.id) === id)
      if (hit) {
        return hit.slug
      }
    }
    return list[0].slug
  }, [me.data])

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 border-b border-[var(--border)] bg-[var(--surface)]/90 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <div className="flex items-center gap-6">
            <Link to="/app/feed" className="text-sm font-semibold tracking-tight text-[var(--text)]">
              Publisher
            </Link>
            <nav className="hidden gap-4 text-sm text-[var(--muted)] sm:flex">
              <Link className="hover:text-[var(--text)]" to="/app/feed">
                Лента
              </Link>
              <Link className="hover:text-[var(--text)]" to="/app/posts/new">
                Новый материал
              </Link>
              <Link className="hover:text-[var(--text)]" to="/app/media">
                Медиа
              </Link>
              <Link className="hover:text-[var(--text)]" to="/app/profile">
                Профиль
              </Link>
              <Link className="hover:text-[var(--text)]" to="/app/channels">
                Каналы
              </Link>
              {workspaceSlug ? (
                <Link className="hover:text-[var(--text)]" to={`/blog/${workspaceSlug}`}>
                  Публичный блог
                </Link>
              ) : null}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <label className="hidden text-xs text-[var(--muted)] sm:block">
              Тема
              <select
                className="ml-2 rounded-md border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-xs text-[var(--text)]"
                value={themeChoice}
                onChange={(e) => setThemeChoice(e.target.value as Theme)}
              >
                <option value="system">Системная</option>
                <option value="light">Светлая</option>
                <option value="dark">Тёмная</option>
              </select>
            </label>
            {me.isSuccess ? (
              <>
                <Link
                  className="max-w-[10rem] truncate text-xs text-[var(--muted)] hover:text-[var(--text)] hover:underline"
                  to="/app/profile"
                  title={me.data.user.email}
                >
                  {me.data.user.displayName}
                </Link>
                <button
                  type="button"
                  className="rounded-md border border-[var(--border)] px-3 py-1.5 text-xs font-medium hover:bg-[var(--bg)]"
                  onClick={() => {
                    clearSession()
                    navigate('/login')
                  }}
                >
                  Выйти
                </button>
              </>
            ) : me.isPending ? (
              <span className="text-xs text-[var(--muted)]">Проверка сессии…</span>
            ) : null}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
