import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PublisherWordmark } from './PublisherWordmark'

function hasSession(): boolean {
  try {
    return Boolean(localStorage.getItem('accessToken'))
  } catch {
    return false
  }
}

/**
 * Шапка публичного блога: выход в приложение для авторизованных, вход/регистрация для гостей.
 */
export function PublicSiteNav() {
  const [authed, setAuthed] = useState(hasSession)

  useEffect(() => {
    const sync = () => setAuthed(hasSession())
    window.addEventListener('focus', sync)
    window.addEventListener('storage', sync)
    return () => {
      window.removeEventListener('focus', sync)
      window.removeEventListener('storage', sync)
    }
  }, [])

  return (
    <div className="space-y-3 border-b border-[var(--border)] pb-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PublisherWordmark to={undefined} size="sm" />
        <div className="flex flex-wrap items-center justify-end gap-4 text-sm">
          {authed ? (
            <Link className="font-medium text-[var(--accent)] hover:underline" to="/app/feed">
              К моим материалам
            </Link>
          ) : (
            <>
              <Link className="font-medium text-[var(--accent)] hover:underline" to="/login">
                Войти
              </Link>
              <Link className="text-[var(--muted)] hover:text-[var(--text)] hover:underline" to="/register">
                Регистрация
              </Link>
            </>
          )}
        </div>
      </div>
      <p className="text-xs text-[var(--muted)]">
        {authed ? 'Вы вошли в аккаунт Publisher.' : 'Войдите, чтобы редактировать материалы в приложении.'}
      </p>
    </div>
  )
}
