import { apiFetch, setSession } from './client'
import type { MeResponse, TokenResponse } from './types'

export async function register(email: string, password: string, displayName: string): Promise<void> {
  const token = await apiFetch<TokenResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  })
  await establishSessionWithTokens(token)
}

export async function login(email: string, password: string): Promise<void> {
  const token = await apiFetch<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  await establishSessionWithTokens(token)
}

/**
 * Сбрасываем workspaceId до /api/me — иначе старый id (другой аккаунт/БД) тянул 403 на запросах с workspace.
 * Используется после пароля и после OAuth (callback).
 */
export async function establishSessionWithTokens(token: TokenResponse): Promise<void> {
  localStorage.setItem('accessToken', token.accessToken)
  localStorage.setItem('refreshToken', token.refreshToken)
  localStorage.removeItem('workspaceId')
  const me = await apiFetch<MeResponse>('/api/me')
  const first = me.workspaces[0]
  if (!first) {
    throw new Error('No workspace')
  }
  setSession(token.accessToken, String(first.id), token.refreshToken)
}
