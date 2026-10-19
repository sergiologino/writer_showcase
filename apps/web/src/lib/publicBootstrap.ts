import type { PageDto, PublicPostDetail, PublicPostSummary } from '../api/types'

type PublicBootstrap = {
  kind: 'blog' | 'article'
  workspaceSlug: string
  postSlug: string | null
  page: number
  data: PageDto<PublicPostSummary> | PublicPostDetail
}

declare global {
  interface Window {
    __PUBLISHER_BOOTSTRAP__?: PublicBootstrap
  }
}

/** Use the data that produced the initial HTML so React does not show a loading page during takeover. */
export function initialBlogData(slug: string, page: number): PageDto<PublicPostSummary> | undefined {
  const initial = window.__PUBLISHER_BOOTSTRAP__
  return initial?.kind === 'blog' && initial.workspaceSlug === slug && initial.page === page
    ? initial.data as PageDto<PublicPostSummary>
    : undefined
}

export function initialArticleData(slug: string, postSlug: string): PublicPostDetail | undefined {
  const initial = window.__PUBLISHER_BOOTSTRAP__
  return initial?.kind === 'article' && initial.workspaceSlug === slug && initial.postSlug === postSlug
    ? initial.data as PublicPostDetail
    : undefined
}
