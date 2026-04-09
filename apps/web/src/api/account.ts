import { apiFetch } from './client'
import type { MeResponse, UpdateProfilePayload } from './types'

export function updateProfile(payload: UpdateProfilePayload): Promise<MeResponse> {
  return apiFetch<MeResponse>('/api/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}
