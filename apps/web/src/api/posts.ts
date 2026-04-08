import { apiFetch } from './client'
import type { PageDto, PostResponse, PostStatus, PostVisibility } from './types'

export function fetchPostPage(params: {
  status?: PostStatus
  q?: string
  page?: number
  size?: number
}): Promise<PageDto<PostResponse>> {
  const sp = new URLSearchParams()
  if (params.status) {
    sp.set('status', params.status)
  }
  if (params.q) {
    sp.set('q', params.q)
  }
  sp.set('page', String(params.page ?? 0))
  sp.set('size', String(params.size ?? 20))
  return apiFetch<PageDto<PostResponse>>(`/api/posts?${sp.toString()}`)
}

export function fetchPost(id: number): Promise<PostResponse> {
  return apiFetch<PostResponse>(`/api/posts/${id}`)
}

export interface PostPayload {
  title: string
  slug: string
  excerpt: string
  bodySource: string
  bodyHtml: string
  visibility: PostVisibility
  status: PostStatus
  categoryId: number | null
  tagIds: number[]
  aiGenerated: boolean
}

export function createPost(payload: PostPayload): Promise<PostResponse> {
  return apiFetch<PostResponse>('/api/posts', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updatePost(id: number, payload: PostPayload): Promise<PostResponse> {
  return apiFetch<PostResponse>(`/api/posts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function fetchPublicPosts(
  workspaceSlug: string,
  page: number
): Promise<PageDto<import('./types').PublicPostSummary>> {
  const sp = new URLSearchParams({ page: String(page), size: '20' })
  return apiFetch(`/api/public/w/${encodeURIComponent(workspaceSlug)}/posts?${sp}`)
}

export function fetchPublicPost(workspaceSlug: string, postSlug: string) {
  return apiFetch<import('./types').PublicPostDetail>(
    `/api/public/w/${encodeURIComponent(workspaceSlug)}/posts/${encodeURIComponent(postSlug)}`
  )
}
