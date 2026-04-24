import { apiFetch } from './client'
import type { MeResponse, UpdateProfilePayload } from './types'

export function updateProfile(payload: UpdateProfilePayload): Promise<MeResponse> {
  return apiFetch<MeResponse>('/api/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

/** null — сбросить аватар */
export function setProfileAvatar(mediaAssetId: number | null): Promise<MeResponse> {
  return apiFetch<MeResponse>('/api/me/avatar', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mediaAssetId }),
  })
}

export function addProfileGalleryPhoto(mediaAssetId: number): Promise<MeResponse> {
  return apiFetch<MeResponse>('/api/me/profile-photos', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mediaAssetId }),
  })
}

export function removeProfileGalleryPhoto(mediaAssetId: number): Promise<MeResponse> {
  return apiFetch<MeResponse>(`/api/me/profile-photos/${mediaAssetId}`, {
    method: 'DELETE',
  })
}
