import { apiFetch } from './client'

export interface AiInvokeResponse {
  ok: boolean
  output: string | null
  errorCode: string | null
}

export interface StudioAiRequest {
  requestType: string
  payload: Record<string, unknown>
  metadata?: Record<string, string> | null
  externalUserId?: string | null
  networkName?: string | null
}

export function studioInvoke(body: StudioAiRequest): Promise<AiInvokeResponse> {
  return apiFetch<AiInvokeResponse>('/api/ai/studio/invoke', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
