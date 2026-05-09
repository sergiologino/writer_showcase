export function getOAuth2BackendBase(): string {
  const fallback = import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin
  const raw = (import.meta.env.VITE_OAUTH_BASE_URL ?? fallback).trim()
  const base = raw.replace(/\/$/, '')
  return base || fallback
}

export function getOAuth2AuthorizationUrl(provider: 'google' | 'yandex'): string {
  return `${getOAuth2BackendBase()}/oauth2/authorization/${provider}`
}
