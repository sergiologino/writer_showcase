import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { clearSession, resolveApiUrl } from '../api/client'

function initials(displayName: string, email: string): string {
  const t = displayName.trim()
  if (t) {
    const parts = t.split(/\s+/).filter(Boolean)
    if (parts.length >= 2) {
      return (parts[0]![0]! + parts[1]![0]!).toUpperCase()
    }
    return t.slice(0, 2).toUpperCase()
  }
  const local = email.split('@')[0] ?? email
  return local.slice(0, 2).toUpperCase() || '?'
}

type Props = {
  displayName: string
  email: string
  /** Публичный путь к файлу аватара (как в /api/me) или null */
  avatarUrl?: string | null
}

export function UserAvatarMenu({ displayName, email, avatarUrl = null }: Props) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const label = useMemo(() => initials(displayName, email), [displayName, email])

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [])

  return (
    <div className="relative" ref={rootRef}>
      <button
        type="button"
        className="flex h-9 w-9 shrink-0 items-center justify-center overflow-hidden rounded-full border border-[var(--border)] bg-[var(--bg)] text-xs font-semibold text-[var(--text)] transition hover:border-[var(--accent)] hover:ring-2 hover:ring-[var(--accent)]/30"
        title={email}
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={() => setOpen((v) => !v)}
      >
        {avatarUrl ? (
          <img
            src={resolveApiUrl(avatarUrl)}
            alt=""
            className="h-full w-full rounded-full object-cover"
          />
        ) : (
          label
        )}
      </button>
      {open ? (
        <div
          role="menu"
          className="absolute right-0 z-20 mt-1 min-w-[11rem] rounded-lg border border-[var(--border)] bg-[var(--surface)] py-1 shadow-lg"
        >
          <Link
            role="menuitem"
            className="block px-3 py-2 text-sm text-[var(--text)] hover:bg-[var(--bg)]"
            to="/app/profile"
            onClick={() => setOpen(false)}
          >
            Профиль
          </Link>
          <Link
            role="menuitem"
            className="block px-3 py-2 text-sm text-[var(--text)] hover:bg-[var(--bg)]"
            to="/app/channels"
            onClick={() => setOpen(false)}
          >
            Каналы
          </Link>
          <button
            type="button"
            role="menuitem"
            className="w-full px-3 py-2 text-left text-sm text-[var(--text)] hover:bg-[var(--bg)]"
            onClick={() => {
              setOpen(false)
              clearSession()
              navigate('/login')
            }}
          >
            Выйти
          </button>
        </div>
      ) : null}
    </div>
  )
}
