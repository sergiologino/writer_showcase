import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchPostPage } from '../api/posts'
import type { PageDto, PostResponse } from '../api/types'
import { listLocalPosts } from '../lib/localPosts'
import { FeedPage } from './FeedPage'

vi.mock('../api/posts', () => ({ fetchPostPage: vi.fn() }))
vi.mock('../lib/localPosts', () => ({ listLocalPosts: vi.fn() }))

const publishedPost: PostResponse = {
  id: 8, title: 'Образ инициативы', slug: 'obraz-initsiativy',
  status: 'PUBLISHED', visibility: 'PUBLIC', excerpt: 'Короткое описание статьи',
  bodySource: 'Текст', bodyHtml: '<p>Текст</p>', aiGenerated: false,
  categoryId: null, tags: [],
  media: [{ mediaAssetId: 4, mimeType: 'image/jpeg', altText: null, sortOrder: 0, caption: null }],
  createdAt: '2026-10-09T10:00:00Z', updatedAt: '2026-10-10T10:00:00Z',
  publishedAt: '2026-10-11T10:00:00Z', scheduledPublishAt: null, scheduleMissed: false,
  lateScheduleReleased: false, channelSyndicationBlocked: false,
  socialPublishEnabled: false, aiTokensTotal: 0, publishChannelTypes: [], outbound: [],
}

function page(content: PostResponse[], number: number, totalPages = 1): PageDto<PostResponse> {
  return { content, number, size: 20, totalElements: totalPages * 20, totalPages }
}

function renderFeed() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter><FeedPage /></MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => {
  localStorage.removeItem('workspaceId')
  vi.clearAllMocks()
})

describe('FeedPage', () => {
  it('separates local copies and shows real post metadata with pagination', async () => {
    vi.mocked(listLocalPosts).mockResolvedValue([{
      key: '7:local-1', localId: 'local-1', serverId: null,
      savedAt: '2026-10-10T10:00:00Z',
      payload: { title: 'Только локально', status: 'DRAFT' },
    } as Awaited<ReturnType<typeof listLocalPosts>>[number]])
    vi.mocked(fetchPostPage).mockImplementation(async ({ page: pageNumber }) =>
      page(pageNumber === 0 ? [publishedPost] : [{ ...publishedPost, id: 9, title: 'Второй материал' }], pageNumber ?? 0, 2))
    localStorage.setItem('workspaceId', '7')

    renderFeed()

    const localSection = screen.getByRole('region', { name: 'Локальные материалы' })
    expect(await within(localSection).findByText('Только локально')).toBeInTheDocument()
    expect(within(localSection).getByText(/Только на этом устройстве/)).toBeInTheDocument()
    expect(await screen.findByText('Образ инициативы')).toBeInTheDocument()
    expect(screen.getByText('Публичная')).toBeInTheDocument()
    expect(screen.getByText('Вложения: 1')).toBeInTheDocument()
    expect(screen.getByText('Короткое описание статьи')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Далее →' }))
    expect(await screen.findByText('Второй материал')).toBeInTheDocument()
    expect(fetchPostPage).toHaveBeenCalledWith({ status: undefined, q: undefined, page: 1 })
  })

  it('passes the selected status and search text to the existing API', async () => {
    vi.mocked(listLocalPosts).mockResolvedValue([])
    vi.mocked(fetchPostPage).mockResolvedValue(page([], 0))
    renderFeed()

    fireEvent.change(screen.getByRole('combobox', { name: 'Статус' }), { target: { value: 'DRAFT' } })
    fireEvent.change(screen.getByRole('searchbox', { name: 'Поиск по публикациям' }), { target: { value: '  заметка  ' } })
    await waitFor(() => expect(fetchPostPage).toHaveBeenCalledWith({ status: 'DRAFT', q: 'заметка', page: 0 }))
  })
})
