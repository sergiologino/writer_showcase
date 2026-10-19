import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { register } from '../api/auth'
import { ApiError } from '../api/client'
import { Oauth2LoginButtons } from '../components/Oauth2LoginButtons'
import { PublisherWordmark } from '../components/PublisherWordmark'
import { Seo } from '../components/Seo'

function safeInternalRedirect(raw: string | null): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//')) {
    return null
  }
  return raw
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () =>
      register(email.trim(), password, displayName.trim() || email.trim().split('@')[0] || 'Author'),
    onSuccess: () => {
      const r = safeInternalRedirect(searchParams.get('redirect'))
      navigate(r ?? '/app/feed')
    },
    onError: (e: unknown) => {
      setError(e instanceof ApiError ? e.message : 'Не удалось зарегистрироваться')
    },
  })

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4">
      <Seo
        title="Регистрация автора"
        description="Создайте рабочее пространство автора в Altacod Publisher: блог, редактор материалов, медиа и публикация в каналы."
        keywords="регистрация блогера, регистрация писателя, авторский блог, платформа для авторов"
        canonicalPath="/register"
        noIndex
      />
      <div className="-mt-4 mb-6">
        <PublisherWordmark size="lg" />
      </div>
      <h1 className="text-2xl font-semibold tracking-tight">Регистрация</h1>
      <p className="mt-1 text-sm text-[var(--muted)]">Создаётся личный workspace автоматически</p>
      <form
        className="ui-card mt-8 space-y-4 p-6"
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
        <label className="block text-sm font-medium">
          Email
          <input
            type="email"
            required
            autoComplete="email"
            className="ui-field mt-1 w-full"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="block text-sm font-medium">
          Имя (необязательно)
          <input
            type="text"
            autoComplete="nickname"
            placeholder="Как вас представить"
            className="ui-field mt-1 w-full"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </label>
        <label className="block text-sm font-medium">
          Пароль (мин. 8 символов)
          <input
            type="password"
            required
            minLength={8}
            autoComplete="new-password"
            className="ui-field mt-1 w-full"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="ui-button ui-button--accent w-full"
        >
          {mutation.isPending ? 'Создание…' : 'Создать аккаунт'}
        </button>
      </form>
      <Oauth2LoginButtons />
      <p className="mt-6 text-center text-sm text-[var(--muted)]">
        Уже есть аккаунт?{' '}
        <Link
          className="font-medium text-[var(--accent)] hover:underline"
          to={
            searchParams.get('redirect')
              ? `/login?redirect=${encodeURIComponent(searchParams.get('redirect')!)}`
              : '/login'
          }
        >
          Вход
        </Link>
      </p>
    </div>
  )
}
