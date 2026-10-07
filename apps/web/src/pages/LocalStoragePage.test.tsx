import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LocalStoragePage } from './LocalStoragePage'

vi.mock('../lib/localPosts', () => ({
  folderPickerAvailable: vi.fn().mockReturnValue(true),
  choosePostFolder: vi.fn(),
  inspectLocalArchive: vi.fn().mockResolvedValue({
    folderName: 'Documents',
    posts: [{
      localId: 'local-123', title: 'Иллюстрированный пост', serverId: 8,
      status: 'PUBLISHED', savedAt: '2026-10-07T10:00:00Z', contentPreview: '# Текст',
      publication: { publishedAt: '2026-10-07T11:00:00Z', publicUrl: 'https://example.com/blog/post', channels: [] },
      media: [],
    }],
  }),
  readLocalMedia: vi.fn(),
}))

describe('LocalStoragePage', () => {
  it('shows the folder structure, material and publication parameters', async () => {
    render(<MemoryRouter><LocalStoragePage /></MemoryRouter>)
    expect(await screen.findByText('Иллюстрированный пост')).toBeInTheDocument()
    expect(screen.getByText(/Documents\/publisher/)).toBeInTheDocument()
    expect(screen.getByText(/серверный ID: 8/)).toBeInTheDocument()
    expect(screen.getByText(/Свой блог: 2026-10-07T11:00:00Z/)).toBeInTheDocument()
  })
})
