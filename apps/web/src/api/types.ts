export type PostVisibility = 'PUBLIC' | 'PRIVATE' | 'UNLISTED'
export type PostStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'ARCHIVED'

export interface TokenResponse {
  accessToken: string
  tokenType: string
}

export interface UserSummary {
  id: number
  email: string
  displayName: string
}

export interface WorkspaceSummary {
  id: number
  name: string
  slug: string
  role: string
}

export interface MeResponse {
  user: UserSummary
  workspaces: WorkspaceSummary[]
}

export interface TagSummary {
  id: number
  name: string
  slug: string
}

export interface PostResponse {
  id: number
  title: string
  slug: string
  excerpt: string | null
  bodySource: string | null
  bodyHtml: string | null
  visibility: PostVisibility
  status: PostStatus
  aiGenerated: boolean
  categoryId: number | null
  tags: TagSummary[]
  createdAt: string
  updatedAt: string
  publishedAt: string | null
}

export interface PageDto<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface PublicPostSummary {
  id: number
  title: string
  slug: string
  excerpt: string | null
  publishedAt: string | null
}

export interface PublicPostDetail extends PublicPostSummary {
  bodyHtml: string | null
  category: {
    id: number
    name: string
    slug: string
  } | null
  tags: TagSummary[]
  updatedAt: string
}
