export type PostVisibility = 'PUBLIC' | 'PRIVATE' | 'UNLISTED'
export type PostStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'ARCHIVED'

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  /** TTL access-токена в секундах */
  expiresIn: number
}

export interface UserSummary {
  id: number
  email: string
  displayName: string
  locale: string | null
  timezone: string | null
  /** Предпочитаемая тема в аккаунте; null — не задано (используйте переключатель в шапке). */
  theme: 'light' | 'dark' | 'system' | null
}

export interface UpdateProfilePayload {
  displayName: string
  locale: string | null
  timezone: string | null
  theme: 'light' | 'dark' | 'system' | null
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

export interface PostMediaAttachment {
  mediaAssetId: number
  mimeType: string | null
  altText: string | null
  sortOrder: number
  caption: string | null
}

/** Каналы кросс-постинга (совпадает с бэкендом `ChannelType`). */
export type ChannelType = 'TELEGRAM' | 'VK' | 'ODNOKLASSNIKI' | 'MAX'

/** Совпадает с `ChannelDeliveryStatus` на бэкенде. */
export type ChannelDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED' | 'REJECTED'

export interface PostOutboundInfo {
  channelType: ChannelType
  deliveryStatus: ChannelDeliveryStatus
  externalUrl: string | null
  lastError: string | null
  metricsFetchedAt: string | null
  likes: number
  reposts: number
  views: number
  comments: number
  shares: number
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
  media: PostMediaAttachment[]
  createdAt: string
  updatedAt: string
  publishedAt: string | null
  socialPublishEnabled: boolean
  /** Пустой массив при включённой соцпубликации = все каналы workspace. */
  publishChannelTypes: ChannelType[]
  outbound: PostOutboundInfo[]
}

export interface PageDto<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first?: boolean
  last?: boolean
}

export interface PublicPostSummary {
  id: number
  title: string
  slug: string
  excerpt: string | null
  publishedAt: string | null
  /** Первое вложение (миниатюра в списке; в ответе детальной страницы может отсутствовать). */
  firstMediaId?: number | null
  firstMediaUrl?: string | null
  firstMediaMimeType?: string | null
  /** Plain-text превью тела (для ленты). */
  bodyPreviewPlain?: string | null
}

export interface PublicMedia {
  id: number
  url: string
  mimeType: string | null
  altText: string | null
  sortOrder: number
  caption: string | null
}

export interface PublicPostDetail extends PublicPostSummary {
  bodyHtml: string | null
  category: {
    id: number
    name: string
    slug: string
  } | null
  tags: TagSummary[]
  media: PublicMedia[]
  updatedAt: string
}
