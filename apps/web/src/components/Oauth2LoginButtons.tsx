import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'
import { getOAuth2AuthorizationUrl } from '../lib/oauth2'

type Providers = { google: boolean; yandex: boolean }

function GoogleIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" aria-hidden>
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
      />
      <path
        fill="#EA4335"
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      />
    </svg>
  )
}

function YandexIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" aria-hidden>
      <rect width="24" height="24" fill="none" />
      <path
        fill="#FC3F1D"
        d="M12.186 2C6.5 2 2 6.4 2 12s4.5 10 10.186 10C17.8 22 22 17.5 22 12S18 2 12.186 2zm3.4 15.1h-2.1l-3.1-4.4v4.4h-2.2V6.8h2.2v3.3l2.6-3.3h2.1l-3.1 3.7 3.4 3.2z"
      />
    </svg>
  )
}

export function Oauth2LoginButtons() {
  const q = useQuery({
    queryKey: ['oauth2-providers'],
    queryFn: () => apiFetch<Providers>('/api/auth/oauth2/providers'),
    staleTime: 60_000,
  })
  if (!q.isSuccess || (!q.data.google && !q.data.yandex)) {
    return null
  }
  return (
    <div className="mt-4 space-y-2">
      <p className="text-center text-xs text-[var(--muted)]">или</p>
      {q.data.google ? (
        <a
          href={getOAuth2AuthorizationUrl('google')}
          className="flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] py-2.5 text-sm font-medium text-[var(--text)] transition hover:bg-[var(--surface)]"
        >
          <GoogleIcon />
          Войти через Google
        </a>
      ) : null}
      {q.data.yandex ? (
        <a
          href={getOAuth2AuthorizationUrl('yandex')}
          className="flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] py-2.5 text-sm font-medium text-[var(--text)] transition hover:bg-[var(--surface)]"
        >
          <YandexIcon />
          Войти через Yandex
        </a>
      ) : null}
    </div>
  )
}
