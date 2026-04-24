const KEY = 'writer-theme'

export type Theme = 'light' | 'dark' | 'system'

export function getStoredTheme(): Theme {
  const v = localStorage.getItem(KEY)
  if (v === 'light' || v === 'dark' || v === 'system') {
    return v
  }
  return 'system'
}

export function applyTheme(theme: Theme): void {
  localStorage.setItem(KEY, theme)
  const root = document.documentElement
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  const dark = theme === 'dark' || (theme === 'system' && prefersDark)
  root.classList.toggle('dark', dark)
}

export function isEffectiveThemeDark(theme: Theme): boolean {
  if (theme === 'dark') {
    return true
  }
  if (theme === 'light') {
    return false
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}
