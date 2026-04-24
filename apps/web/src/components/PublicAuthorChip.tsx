import { resolveApiUrl } from '../api/client'
import { publicAuthorInitials } from '../lib/publicAuthorDisplay'

type Props = {
  name: string
  avatarUrl?: string | null
  className?: string
}

/**
 * Круг с аватаром или инициалами + имя (публичная лента, посты).
 */
export function PublicAuthorChip({ name, avatarUrl, className = '' }: Props) {
  const display = (name || '').trim() || 'Автор'
  const initials = publicAuthorInitials(display)
  const src = avatarUrl ? resolveApiUrl(avatarUrl) : null

  return (
    <div className={`flex items-center gap-2.5 ${className}`.trim()}>
      {src ? (
        <img
          src={src}
          alt=""
          className="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-[var(--border)]"
          loading="lazy"
        />
      ) : (
        <div
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--bg)] text-xs font-semibold ring-1 ring-[var(--border)]"
          aria-hidden
        >
          {initials}
        </div>
      )}
      <span className="min-w-0 text-sm font-medium text-[var(--text)] [overflow-wrap:anywhere]">{display}</span>
    </div>
  )
}
