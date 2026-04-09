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

export function setSession(token: string, workspaceId: string, refreshToken?: string): void {
  localStorage.setItem('accessToken', token)
  localStorage.setItem('workspaceId', workspaceId)
  if (refreshToken !== undefined) {
    localStorage.setItem('refreshToken', refreshToken)
  }
}

export function clearSession(): void {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
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

let refreshInFlight: Promise<boolean> | null = null

async function tryRefreshAccessToken(): Promise<boolean> {
  const rt = localStorage.getItem('refreshToken')
  if (!rt) {
    return false
  }
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const url = resolveApiUrl('/api/auth/refresh')
        const res = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: rt }),
        })
        if (!res.ok) {
          return false
        }
        const data = (await res.json()) as {
          accessToken: string
          refreshToken?: string
        }
        localStorage.setItem('accessToken', data.accessToken)
        if (data.refreshToken) {
          localStorage.setItem('refreshToken', data.refreshToken)
        }
        return true
      } catch {
        return false
      } finally {
        refreshInFlight = null
      }
    })()
  }
  return refreshInFlight
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, isRetry = false): Promise<T> {
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

  if (res.status === 401 && !isRetry && token && !path.startsWith('/api/auth/')) {
    const refreshed = await tryRefreshAccessToken()
    if (refreshed) {
      return apiFetch<T>(path, init, true)
    }
    clearSession()
  }

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
