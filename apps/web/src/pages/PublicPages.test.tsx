import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchPublicPost, fetchPublicPosts } from '../api/posts'
import type { PublicPostDetail, PublicPostSummary } from '../api/types'
import { fetchPublicEngagement } from '../api/publicEngagement'
import { PublicBlogPage } from './PublicBlogPage'
import { PublicPostPage } from './PublicPostPage'

vi.mock('../api/posts', () => ({ fetchPublicPosts: vi.fn(), fetchPublicPost: vi.fn() }))
vi.mock('../api/publicEngagement', () => ({
  fetchPublicEngagement: vi.fn(), postPublicComment: vi.fn(), togglePostLike: vi.fn(),
}))
vi.mock('../components/PublicSiteNav', () => ({ PublicSiteNav: () => <nav>Навигация</nav> }))
vi.mock('../components/PublicPostOwnerActions', () => ({ PublicPostOwnerActions: () => null }))

const summary: PublicPostSummary = {
  id: 5, authorId: 9, title: 'Статья без обложки', slug: 'article',
  excerpt: 'Описание из публикации', publishedAt: '2026-10-10T10:00:00Z',
  firstMediaUrl: null, firstMediaMimeType: null, bodyPreviewPlain: 'Текст статьи',
  authorDisplayName: 'Анна',
}

function renderPage(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/blog/:workspaceSlug" element={<PublicBlogPage />} />
          <Route path="/blog/:workspaceSlug/p/:postSlug" element={<PublicPostPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => {
  vi.clearAllMocks()
  localStorage.removeItem('accessToken')
})

describe('public pages', () => {
  it('renders a text-only blog card and navigates the existing public pages', async () => {
    vi.mocked(fetchPublicPosts).mockImplementation(async (_, page) => ({
      content: page === 0 ? [summary] : [{ ...summary, id: 6, title: 'Следующая статья', slug: 'next' }],
      totalElements: 21, totalPages: 2, number: page, size: 20,
    }))
    renderPage('/blog/author-space')

    expect(await screen.findByRole('heading', { name: 'Статья без обложки' })).toBeInTheDocument()
    expect(screen.getByText('Описание из публикации')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Читать дальше →' })).toHaveAttribute('href', '/blog/author-space/p/article')
    await waitFor(() => expect(document.head.querySelector('script[type="application/ld+json"]')?.textContent).toContain('"@type":"Blog"'))
    fireEvent.click(screen.getByRole('link', { name: 'Далее →' }))
    expect(await screen.findByRole('heading', { name: 'Следующая статья' })).toBeInTheDocument()
    expect(fetchPublicPosts).toHaveBeenCalledWith('author-space', 1)
  })

  it('uses article data for title, category, media and BlogPosting schema', async () => {
    const detail: PublicPostDetail = {
      ...summary, bodyHtml: '<p>Содержимое статьи</p>',
      category: { id: 2, name: 'Мысли', slug: 'thoughts' },
      tags: [], updatedAt: '2026-10-11T10:00:00Z',
      media: [{ id: 3, url: '/media/3', mimeType: 'image/jpeg', altText: 'Горы', sortOrder: 0, caption: 'Вид на горы' }],
    }
    vi.mocked(fetchPublicPost).mockResolvedValue(detail)
    vi.mocked(fetchPublicEngagement).mockResolvedValue({
      likeCount: 0, likedByMe: false, comments: [], commentCount: 0,
    })
    renderPage('/blog/author-space/p/article')

    expect(await screen.findByRole('heading', { name: 'Статья без обложки' })).toBeInTheDocument()
    expect(screen.getByText('Мысли')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Горы' })).toBeInTheDocument()
    expect(screen.getByText('Содержимое статьи')).toBeInTheDocument()
    await waitFor(() => {
      const schema = document.head.querySelector('script[type="application/ld+json"]')
      expect(schema?.textContent).toContain('"@type":"BlogPosting"')
      expect(schema?.textContent).toContain('"headline":"Статья без обложки"')
      expect(schema?.textContent).toContain('"name":"Анна"')
    })
    expect(document.title).toContain('Статья без обложки')
  })

  it('marks a missing article noindex', async () => {
    vi.mocked(fetchPublicPost).mockRejectedValue(new Error('Not found'))
    vi.mocked(fetchPublicEngagement).mockRejectedValue(new Error('Not found'))
    renderPage('/blog/author-space/p/missing')
    expect(await screen.findByRole('alert')).toHaveTextContent('Материал не найден')
    await waitFor(() => expect(document.head.querySelector('meta[name="robots"]')).toHaveAttribute('content', 'noindex,nofollow'))
  })
})
