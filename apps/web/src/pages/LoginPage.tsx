import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { login } from '../api/auth'
import { ApiError } from '../api/client'
import { Oauth2LoginButtons } from '../components/Oauth2LoginButtons'
import { PublisherWordmark } from '../components/PublisherWordmark'

function safeInternalRedirect(raw: string | null): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//')) {
    return null
  }
  return raw
}

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const oauth = searchParams.get('oauth')
  const oauthMessage =
    oauth === 'link_conflict'
      ? 'Этот email уже привязан к другому способу входа.'
      : oauth === 'error'
        ? 'Ошибка входа через внешнего провайдера.'
        : null

  const mutation = useMutation({
    mutationFn: () => login(email.trim(), password),
    onSuccess: () => {
      const r = safeInternalRedirect(searchParams.get('redirect'))
      navigate(r ?? '/app/feed')
    },
    onError: (e: unknown) => {
      setError(e instanceof ApiError ? e.message : 'Не удалось войти')
    },
  })

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4">
      <div className="-mt-4 mb-6">
        <PublisherWordmark size="lg" />
      </div>
      <h1 className="text-2xl font-semibold tracking-tight">Вход</h1>
      <p className="mt-1 text-sm text-[var(--muted)]">Ваша лента, материалы и публикации в одном месте</p>
      <form
        className="mt-8 space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm"
        onSubmit={(ev) => {
          ev.preventDefault()
          setError(null)
          mutation.mutate()
        }}
      >
        {error ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
            {error}
          </p>
        ) : null}
        {oauthMessage && !error ? (
          <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-100">
            {oauthMessage}
          </p>
        ) : null}
        <label className="block text-sm font-medium">
          Email
          <input
            type="email"
            required
            autoComplete="email"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="block text-sm font-medium">
          Пароль
          <input
            type="password"
            required
            autoComplete="current-password"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full rounded-lg bg-[var(--accent)] py-2.5 text-sm font-medium text-white transition hover:bg-[var(--accent-hover)] disabled:opacity-60"
        >
          {mutation.isPending ? 'Вход…' : 'Войти'}
        </button>
      </form>
      <Oauth2LoginButtons />
      <p className="mt-6 text-center text-sm text-[var(--muted)]">
        Нет аккаунта?{' '}
        <Link
          className="font-medium text-[var(--accent)] hover:underline"
          to={
            searchParams.get('redirect')
              ? `/register?redirect=${encodeURIComponent(searchParams.get('redirect')!)}`
              : '/register'
          }
        >
          Регистрация
        </Link>
      </p>
    </div>
  )
}
