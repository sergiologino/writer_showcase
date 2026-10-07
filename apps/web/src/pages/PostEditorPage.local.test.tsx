import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPost } from '../api/posts'
import { saveLocalPost } from '../lib/localPosts'
import { PostEditorPage } from './PostEditorPage'

vi.mock('../api/categories', () => ({ fetchCategories: vi.fn().mockResolvedValue([]), createCategory: vi.fn() }))
vi.mock('../api/channels', () => ({ fetchWorkspaceChannels: vi.fn().mockResolvedValue([]) }))
vi.mock('../api/posts', () => ({ createPost: vi.fn(), updatePost: vi.fn(), fetchPost: vi.fn() }))
vi.mock('../lib/localPosts', () => ({
  localPostKey: (workspace: string, id: number | string) => `${workspace}:${id}`,
  getPostFolder: vi.fn().mockResolvedValue(undefined),
  loadLocalPost: vi.fn().mockResolvedValue(undefined),
  saveLocalPost: vi.fn(),
  bindServerId: vi.fn(),
  choosePostFolder: vi.fn(),
  folderPickerAvailable: vi.fn().mockReturnValue(true),
}))

describe('local-first save in the editor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.setItem('workspaceId', '1')
  })

  it('does not send a post to the API when the local source cannot be written', async () => {
    vi.mocked(saveLocalPost).mockRejectedValue(new Error('Нет доступа к папке'))
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={['/app/posts/new']}>
          <Routes><Route path="/app/posts/new" element={<PostEditorPage />} /></Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )
    fireEvent.change(screen.getByLabelText('Заголовок'), { target: { value: 'Локальная статья' } })
    fireEvent.click(screen.getByRole('button', { name: /^Сохранить$/ }))
    await waitFor(() => expect(screen.getByText('Нет доступа к папке')).toBeInTheDocument())
    expect(createPost).not.toHaveBeenCalled()
  })

  it('writes the local source before creating a server post', async () => {
    vi.mocked(saveLocalPost).mockResolvedValue(undefined)
    vi.mocked(createPost).mockResolvedValue({ id: 17 } as Awaited<ReturnType<typeof createPost>>)
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={['/app/posts/new']}>
          <Routes><Route path="/app/posts/new" element={<PostEditorPage />} /></Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )
    fireEvent.change(screen.getByLabelText('Заголовок'), { target: { value: 'Сначала файл' } })
    fireEvent.click(screen.getByRole('button', { name: /^Сохранить$/ }))
    await waitFor(() => expect(createPost).toHaveBeenCalledTimes(1))
    expect(vi.mocked(saveLocalPost).mock.invocationCallOrder[0])
      .toBeLessThan(vi.mocked(createPost).mock.invocationCallOrder[0])
  })
})
