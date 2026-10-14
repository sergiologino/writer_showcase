import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchMe } from '../api/account'
import { unpublishPost } from '../api/posts'
import { loadLocalPost, saveLocalPost } from '../lib/localPosts'
import { PublicPostOwnerActions } from './PublicPostOwnerActions'

vi.mock('../api/account', () => ({ fetchMe: vi.fn() }))
vi.mock('../api/posts', () => ({ unpublishPost: vi.fn() }))
vi.mock('../lib/localPosts', () => ({
  localPostKey: (workspace: string, id: number) => `${workspace}:${id}`,
  loadLocalPost: vi.fn(), saveLocalPost: vi.fn(),
}))

const me = {
  user: { id: 7 }, workspaces: [{ id: 42, slug: 'my-blog', role: 'OWNER' }],
}

function renderActions(authorId: number) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter>
    <PublicPostOwnerActions postId={15} authorId={authorId} workspaceSlug="my-blog" title="Мой материал" />
  </MemoryRouter></QueryClientProvider>)
}

describe('public blog owner actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.setItem('accessToken', 'test-token')
    localStorage.setItem('workspaceId', '999')
    vi.mocked(fetchMe).mockResolvedValue(me as Awaited<ReturnType<typeof fetchMe>>)
    vi.mocked(unpublishPost).mockResolvedValue({ status: 'DRAFT' } as Awaited<ReturnType<typeof unpublishPost>>)
    vi.mocked(loadLocalPost).mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('lets the author remove a post from the public blog using its workspace', async () => {
    renderActions(7)
    fireEvent.click(await screen.findByRole('button', { name: 'Снять с публикации' }))
    await waitFor(() => expect(unpublishPost).toHaveBeenCalledWith(15, 42))
    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('Уже отправленные посты в соцсетях сохранятся'))
  })

  it('does not show the action for another author', async () => {
    renderActions(8)
    await waitFor(() => expect(fetchMe).toHaveBeenCalled())
    expect(screen.queryByRole('button', { name: 'Снять с публикации' })).not.toBeInTheDocument()
  })

  it('opens the editor in the blog workspace', async () => {
    renderActions(7)
    fireEvent.click(await screen.findByRole('link', { name: 'Редактировать' }))
    expect(localStorage.getItem('workspaceId')).toBe('42')
  })

  it('updates an existing local copy after taking the post down', async () => {
    vi.mocked(loadLocalPost).mockResolvedValue({
      key: '42:15', localId: 'stable-id', serverId: 15, savedAt: '2026-10-01T00:00:00Z',
      payload: {
        title: 'Мой материал', slug: 'post', excerpt: '', bodySource: '# Текст', bodyHtml: '<h1>Текст</h1>',
        visibility: 'PUBLIC', status: 'PUBLISHED', categoryId: null, tagIds: [], aiGenerated: false,
        mediaAssetIds: [], scheduledPublishAt: '2026-10-02T00:00:00Z',
      },
      publication: { publishedAt: '2026-10-02T00:00:00Z', publicUrl: '/blog/my-blog/p/post', channels: [] },
    })
    renderActions(7)

    fireEvent.click(await screen.findByRole('button', { name: 'Снять с публикации' }))
    await waitFor(() => expect(saveLocalPost).toHaveBeenCalledWith(expect.objectContaining({
      payload: expect.objectContaining({ status: 'DRAFT', scheduledPublishAt: null }),
      publication: expect.objectContaining({ publishedAt: null, publicUrl: null }),
    })))
  })
})
