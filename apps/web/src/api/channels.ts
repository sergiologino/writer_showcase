import { apiFetch } from './client'
import type { ChannelType } from './types'

export interface WorkspaceChannel {
  id: number
  channelType: ChannelType
  enabled: boolean
  label: string | null
  configMasked: string
  updatedAt: string
}

export function fetchWorkspaceChannels(): Promise<WorkspaceChannel[]> {
  return apiFetch<WorkspaceChannel[]>('/api/channels')
}

export function upsertWorkspaceChannel(
  type: ChannelType,
  enabled: boolean,
  label: string,
  configJson: string,
): Promise<WorkspaceChannel> {
  return apiFetch<WorkspaceChannel>(`/api/channels/${encodeURIComponent(type)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      enabled,
      label: label.trim() ? label.trim() : null,
      configJson,
    }),
  })
}
