import { Link } from 'react-router-dom'

const sizeClass = {
  sm: { img: 'h-7 w-7', text: 'text-base' },
  md: { img: 'h-8 w-8', text: 'text-lg' },
  lg: { img: 'h-10 w-10', text: 'text-2xl' },
} as const

type Size = keyof typeof sizeClass

type PublisherWordmarkProps = {
  /** Куда ведёт клик. Без `to` — только визуальный блок (например страница входа). */
  to?: string
  className?: string
  showText?: boolean
  size?: Size
}

/**
 * Логотип (favicon.svg) + название «Publisher» шрифтом Newsreader.
 */
export function PublisherWordmark({ to, className = '', showText = true, size = 'md' }: PublisherWordmarkProps) {
  const s = sizeClass[size]
  const content = (
    <>
      <img
        src="/favicon.svg"
        alt=""
        className={`${s.img} shrink-0`}
        width={40}
        height={40}
        decoding="async"
        fetchPriority="high"
        aria-hidden
      />
      {showText ? (
        <span className={`font-brand font-semibold tracking-tight ${s.text} text-[var(--text)]`}>Publisher</span>
      ) : null}
    </>
  )

  if (to) {
    return (
      <Link to={to} className={`inline-flex items-center gap-2.5 min-w-0 rounded-md outline-none ring-offset-2 ring-offset-[var(--surface)] focus-visible:ring-2 focus-visible:ring-[var(--accent)] ${className}`}>
        {content}
      </Link>
    )
  }

  return <div className={`inline-flex items-center gap-2.5 ${className}`}>{content}</div>
}
