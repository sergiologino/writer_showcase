import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { establishSessionWithTokens } from '../api/auth'
import type { TokenResponse } from '../api/types'
import { PublisherWordmark } from '../components/PublisherWordmark'

function safeInternalRedirect(raw: string | null): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//')) {
    return null
  }
  return raw
}

/** strict mode / remount: не отрабатывать один и тот же # дважды */
let lastProcessedOAuthCallbackHash: string | null = null

/**
 * #access_token=…&refresh_token=…&token_type=…&expires_in=…
 */
export function AuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const hash = window.location.hash
    if (!hash || hash.length < 2) {
      setError('Нет данных авторизации')
      return
    }
    if (lastProcessedOAuthCallbackHash === hash) {
      return
    }
    const p = new URLSearchParams(hash.slice(1))
    const accessToken = p.get('access_token')
    const refreshToken = p.get('refresh_token')
    if (!accessToken || !refreshToken) {
      setError('Неполные данные сессии')
      return
    }
    lastProcessedOAuthCallbackHash = hash
    const token: TokenResponse = {
      accessToken,
      refreshToken,
      tokenType: p.get('token_type') ?? 'Bearer',
      expiresIn: Number(p.get('expires_in') ?? '0') || 0,
    }
    const redirect = safeInternalRedirect(new URLSearchParams(window.location.search).get('redirect'))
    void establishSessionWithTokens(token)
      .then(() => {
        window.history.replaceState(null, '', window.location.pathname + window.location.search)
        navigate(redirect ?? '/app/feed', { replace: true })
      })
      .catch(() => {
        lastProcessedOAuthCallbackHash = null
        setError('Не удалось завершить вход')
      })
  }, [navigate])

  return (
    <div className="mx-auto flex min-h-[50vh] max-w-md flex-col justify-center px-4">
      <div className="-mt-4 mb-6">
        <PublisherWordmark size="lg" />
      </div>
      {error ? (
        <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
      ) : (
        <p className="text-sm text-[var(--muted)]">Завершение входа…</p>
      )}
    </div>
  )
}
