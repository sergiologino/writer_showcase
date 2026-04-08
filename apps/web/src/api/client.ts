const getWorkspaceId = (): string | null => localStorage.getItem('workspaceId')

const getToken = (): string | null => localStorage.getItem('accessToken')

/**
 * Собирает URL запроса к API.
 * `baseUrl` — для тестов; в приложении не передаётся (берётся `import.meta.env.VITE_API_BASE_URL`).
 */
export function resolveApiUrl(path: string, baseUrl?: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  const raw = (baseUrl !== undefined ? baseUrl : (import.meta.env.VITE_API_BASE_URL ?? '')).trim()
  const base = raw.replace(/\/$/, '')
  const p = path.startsWith('/') ? path : `/${path}`
  return base ? `${base}${p}` : p
}

export function setSession(token: string, workspaceId: string): void {
  localStorage.setItem('accessToken', token)
  localStorage.setItem('workspaceId', workspaceId)
}

export function clearSession(): void {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('workspaceId')
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function parseJson<T>(res: Response): Promise<T> {
  const text = await res.text()
  if (!text) {
    return undefined as T
  }
  return JSON.parse(text) as T
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const url = resolveApiUrl(path)
  const headers = new Headers(init.headers)
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const ws = getWorkspaceId()
  if (ws && !path.startsWith('/api/auth/') && !path.startsWith('/api/public/')) {
    headers.set('X-Workspace-Id', ws)
  }
  if (init.body && typeof init.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const res = await fetch(url, { ...init, headers })

  if (res.status === 204) {
    return undefined as T
  }

  if (!res.ok) {
    let msg = res.statusText
    try {
      const body = (await parseJson<{ error?: string }>(res)) as { error?: string }
      if (body?.error && typeof body.error === 'string') {
        msg = body.error
      }
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, msg)
  }

  return parseJson<T>(res)
}
