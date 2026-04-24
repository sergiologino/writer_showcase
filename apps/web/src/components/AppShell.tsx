import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ApiError, apiFetch, clearSession } from '../api/client'
import type { MeResponse } from '../api/types'
import { useEffect, useMemo } from 'react'
import { PublisherWordmark } from './PublisherWordmark'
import { ThemeToggleButton } from './ThemeToggleButton'
import { UserAvatarMenu } from './UserAvatarMenu'

export function AppShell() {
  const navigate = useNavigate()

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
          <div className="flex min-w-0 items-center gap-6">
            <PublisherWordmark to="/app/feed" size="md" />
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
              {workspaceSlug ? (
                <Link className="hover:text-[var(--text)]" to={`/blog/${workspaceSlug}`}>
                  Публичный блог
                </Link>
              ) : null}
            </nav>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            <ThemeToggleButton />
            {me.isSuccess ? (
              <UserAvatarMenu
                displayName={me.data.user.displayName}
                email={me.data.user.email}
                avatarUrl={me.data.user.avatarUrl ?? null}
              />
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
