import { apiFetch } from './client'

export interface AiInvokeResponse {
  ok: boolean
  output: string | null
  errorCode: string | null
  /** Токены последнего ответа интеграции */
  tokensUsed: number | null
  /** Сумма по статье после вызова, если в запросе был postId */
  postTokensTotal: number | null
}

export interface StudioAiRequest {
  requestType: string
  payload: Record<string, unknown>
  metadata?: Record<string, string> | null
  externalUserId?: string | null
  networkName?: string | null
  /** Учёт накопительных токенов по статье */
  postId?: number | null
}

export function studioInvoke(body: StudioAiRequest): Promise<AiInvokeResponse> {
  return apiFetch<AiInvokeResponse>('/api/ai/studio/invoke', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
