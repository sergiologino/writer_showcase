import { apiFetch } from './client'

export interface PublicComment {
  id: number
  authorDisplayName: string
  body: string
  createdAt: string
}

export interface PublicEngagement {
  likeCount: number
  commentCount: number
  likedByMe: boolean
  comments: PublicComment[]
}

export function fetchPublicEngagement(workspaceSlug: string, postSlug: string): Promise<PublicEngagement> {
  return apiFetch<PublicEngagement>(
    `/api/public/w/${encodeURIComponent(workspaceSlug)}/posts/${encodeURIComponent(postSlug)}/engagement`,
  )
}

export function togglePostLike(workspaceSlug: string, postSlug: string): Promise<PublicEngagement> {
  return apiFetch<PublicEngagement>(
    `/api/engagement/w/${encodeURIComponent(workspaceSlug)}/posts/${encodeURIComponent(postSlug)}/likes`,
    { method: 'POST' },
  )
}

export function postPublicComment(
  workspaceSlug: string,
  postSlug: string,
  body: string,
): Promise<PublicEngagement> {
  return apiFetch<PublicEngagement>(
    `/api/engagement/w/${encodeURIComponent(workspaceSlug)}/posts/${encodeURIComponent(postSlug)}/comments`,
    { method: 'POST', body: JSON.stringify({ body }) },
  )
}
