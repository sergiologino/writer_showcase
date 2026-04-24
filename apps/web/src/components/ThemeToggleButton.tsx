import { useEffect, useState } from 'react'
import { applyTheme, getStoredTheme, isEffectiveThemeDark, type Theme } from '../lib/theme'

function SunIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12zm0-16a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0V3a1 1 0 0 1 1-1zm0 18a1 1 0 0 1-1-1v-1a1 1 0 1 1 2 0v1a1 1 0 0 1-1 1zM5.64 5.64a1 1 0 0 1 1.41 0l.71.71a1 1 0 0 1-1.41 1.41l-.71-.71a1 1 0 0 1 0-1.41zm12.02 12.02a1 1 0 0 1-1.41 0l-.71-.71a1 1 0 1 1 1.41-1.41l.71.71a1 1 0 0 1 0 1.41zM4 13H3a1 1 0 1 1 0-2h1a1 1 0 1 1 0 2zm17 0h-1a1 1 0 1 1 0-2h1a1 1 0 1 1 0 2zM6.05 18.36l-.71.71a1 1 0 0 1-1.41-1.41l.71-.71a1 1 0 0 1 1.41 1.41zm12.02-12.02l.71-.71a1 1 0 1 0-1.41 1.41l-.71.71a1 1 0 0 0 1.41-1.41z" />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
    </svg>
  )
}

/**
 * Переключение светлой/тёмной темы: в чате «системная» сохраняется в localStorage,
 * кнопка переключает на явный light/dark относительно текущего эффективного режима.
 */
export function ThemeToggleButton() {
  const [theme, setTheme] = useState<Theme>(() => getStoredTheme())

  useEffect(() => {
    applyTheme(theme)
  }, [theme])

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

  const dark = isEffectiveThemeDark(theme)

  return (
    <button
      type="button"
      className="rounded-lg border border-[var(--border)] p-2 text-[var(--muted)] transition hover:bg-[var(--bg)] hover:text-[var(--text)]"
      title={dark ? 'Светлая тема' : 'Тёмная тема'}
      aria-label={dark ? 'Включить светлую тему' : 'Включить тёмную тему'}
      onClick={() => {
        setTheme((prev) => {
          const wasDark = isEffectiveThemeDark(prev)
          return wasDark ? 'light' : 'dark'
        })
      }}
    >
      {dark ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}
