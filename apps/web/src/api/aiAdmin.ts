import { apiFetch } from './client'

export interface AvailableNetwork {
  id: string
  name: string
  displayName?: string
  provider?: string
  networkType?: string
  modelName?: string
}

function parseNetworkList(data: unknown): AvailableNetwork[] {
  if (Array.isArray(data)) {
    return data as AvailableNetwork[]
  }
  if (typeof data === 'string' && data.trim()) {
    try {
      const inner = JSON.parse(data) as unknown
      return Array.isArray(inner) ? (inner as AvailableNetwork[]) : []
    } catch {
      return []
    }
  }
  return []
}

export async function fetchAvailableNetworks(): Promise<AvailableNetwork[]> {
  const data = await apiFetch<unknown>('/api/ai/admin/available-networks')
  return parseNetworkList(data)
}

export async function fetchAiRouting(): Promise<Record<string, string[]>> {
  return apiFetch<Record<string, string[]>>('/api/ai/admin/routing')
}

export async function saveAiRouting(routing: Record<string, string[]>): Promise<void> {
  await apiFetch('/api/ai/admin/routing', {
    method: 'PUT',
    body: JSON.stringify({ routing }),
  })
}
