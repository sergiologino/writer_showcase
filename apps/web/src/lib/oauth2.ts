/**
 * Прямой URL бэкенда для /oauth2/authorization/… (сессия OAuth на порту 8080).
 * В dev: http://localhost:8080 (Vite-прокси сюда не направляют, чтобы JSESSIONID совпадал с redirect_uri).
 */
export function getOAuth2BackendBase(): string {
  const raw = (import.meta.env.VITE_OAUTH_BASE_URL ?? 'http://localhost:8080').trim()
  const base = raw.replace(/\/$/, '')
  return base || 'http://localhost:8080'
}

export function getOAuth2AuthorizationUrl(provider: 'google' | 'yandex'): string {
  return `${getOAuth2BackendBase()}/oauth2/authorization/${provider}`
}
