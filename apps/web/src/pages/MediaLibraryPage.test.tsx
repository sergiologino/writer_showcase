import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchMediaPage } from '../api/media'
import { MediaLibraryPage } from './MediaLibraryPage'

vi.mock('../api/media', () => ({
  fetchMediaPage: vi.fn(),
  uploadMedia: vi.fn(),
  deleteMedia: vi.fn(),
}))

vi.mock('../api/client', () => ({
  getStoredAccessToken: () => 'test-token',
  resolveApiUrl: (path: string) => path,
}))

afterEach(() => {
  localStorage.removeItem('workspaceId')
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('MediaLibraryPage', () => {
  it('shows uploaded image previews from the authenticated media endpoint', async () => {
    localStorage.setItem('workspaceId', '7')
    vi.mocked(fetchMediaPage).mockResolvedValue({
      content: [{
        id: 4,
        type: 'IMAGE',
        sourceType: 'UPLOAD',
        mimeType: 'image/jpeg',
        sizeBytes: 184320,
        altText: null,
        createdAt: '2026-10-08T10:00:00Z',
      }],
      number: 0,
      size: 48,
      totalElements: 1,
      totalPages: 1,
    })
    const fetchFile = vi.fn().mockResolvedValue({
      ok: true,
      blob: async () => new Blob(['image'], { type: 'image/jpeg' }),
    })
    vi.stubGlobal('fetch', fetchFile)
    vi.stubGlobal('URL', class TestURL extends URL {
      static createObjectURL = vi.fn(() => 'blob:preview')
      static revokeObjectURL = vi.fn()
    })

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter><MediaLibraryPage /></MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('img', { name: 'Превью изображения #4' })).toHaveAttribute('src', 'blob:preview')
    expect(fetchFile).toHaveBeenCalledWith('/api/media/4/file', {
      headers: { Authorization: 'Bearer test-token', 'X-Workspace-Id': '7' },
    })
  })
})
