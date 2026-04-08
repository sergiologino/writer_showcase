import { apiFetch, setSession } from './client'
import type { MeResponse, TokenResponse } from './types'

export async function register(email: string, password: string, displayName: string): Promise<void> {
  const token = await apiFetch<TokenResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  })
  await establishSession(token.accessToken)
}

export async function login(email: string, password: string): Promise<void> {
  const token = await apiFetch<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  await establishSession(token.accessToken)
}

async function establishSession(accessToken: string): Promise<void> {
  localStorage.setItem('accessToken', accessToken)
  const me = await apiFetch<MeResponse>('/api/me')
  const first = me.workspaces[0]
  if (!first) {
    throw new Error('No workspace')
  }
  setSession(accessToken, String(first.id))
}
