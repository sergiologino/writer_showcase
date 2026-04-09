import { apiFetch } from './client'
import type { PageDto } from './types'

export interface MediaAsset {
  id: number
  type: string
  sourceType: string
  mimeType: string | null
  sizeBytes: number | null
  altText: string | null
  createdAt: string
}

export function fetchMediaPage(page = 0, size = 24): Promise<PageDto<MediaAsset>> {
  const sp = new URLSearchParams({ page: String(page), size: String(size) })
  return apiFetch<PageDto<MediaAsset>>(`/api/media?${sp}`)
}

export function uploadMedia(file: File, altText?: string): Promise<MediaAsset> {
  const fd = new FormData()
  fd.append('file', file)
  if (altText?.trim()) {
    fd.append('altText', altText.trim())
  }
  return apiFetch<MediaAsset>('/api/media', { method: 'POST', body: fd })
}

export function deleteMedia(id: number): Promise<void> {
  return apiFetch<void>(`/api/media/${id}`, { method: 'DELETE' })
}
