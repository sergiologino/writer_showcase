import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { studioInvoke } from '../api/aiStudio'
import { uploadMedia } from '../api/media'
import { createPost } from '../api/posts'
import { choosePostFolder, saveLocalPost, saveSelectedMedia } from '../lib/localPosts'
import { PostEditorPage } from './PostEditorPage'

vi.mock('../api/categories', () => ({ fetchCategories: vi.fn().mockResolvedValue([]), createCategory: vi.fn() }))
vi.mock('../api/channels', () => ({ fetchWorkspaceChannels: vi.fn().mockResolvedValue([]) }))
vi.mock('../api/posts', () => ({ createPost: vi.fn(), updatePost: vi.fn(), fetchPost: vi.fn() }))
vi.mock('../api/aiStudio', () => ({ studioInvoke: vi.fn() }))
vi.mock('../api/media', () => ({ uploadMedia: vi.fn() }))
vi.mock('../components/LocalMediaThumb', () => ({ LocalMediaThumb: () => <span>Локальное превью</span> }))
vi.mock('../lib/localPosts', () => ({
  localPostKey: (workspace: string, id: number | string) => `${workspace}:${id}`,
  getPostFolder: vi.fn().mockResolvedValue(undefined),
  loadLocalPost: vi.fn().mockResolvedValue(undefined),
  saveLocalPost: vi.fn(),
  saveSelectedMedia: vi.fn(),
  bindServerId: vi.fn(),
  choosePostFolder: vi.fn(),
  folderPickerAvailable: vi.fn().mockReturnValue(true),
}))

describe('local-first save in the editor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.setItem('workspaceId', '1')
  })
  afterEach(() => vi.unstubAllGlobals())

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

  it('adds a generated image to local post media before uploading it', async () => {
    vi.mocked(saveLocalPost).mockResolvedValue(undefined)
    vi.mocked(choosePostFolder).mockResolvedValue('Documents')
    vi.mocked(saveSelectedMedia).mockResolvedValue({
      fileName: 'generated.png', mimeType: 'image/png', serverId: null, originalName: 'generated.png',
    })
    vi.mocked(uploadMedia).mockResolvedValue({
      id: 9, type: 'IMAGE', sourceType: 'UPLOAD', mimeType: 'image/png', sizeBytes: 4,
      altText: null, createdAt: '2026-10-08T10:00:00Z',
    })
    vi.mocked(studioInvoke).mockResolvedValue({
      ok: true, output: 'Изображение готово', imageDataUrl: 'data:image/png;base64,iVBORw0KGgo=',
      errorCode: null, tokensUsed: null, postTokensTotal: null,
    })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      blob: async () => new Blob(['image'], { type: 'image/png' }),
    }))
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/app/posts/new']}>
        <Routes><Route path="/app/posts/new" element={<PostEditorPage />} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>)

    fireEvent.click(screen.getByRole('button', { name: 'Создать изображение' }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Выбрать папку' }))
    await waitFor(() => expect(within(screen.getByRole('dialog')).getByRole('button', { name: 'Создать изображение' })).toBeEnabled())
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Создать изображение' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Готово — добавить в медиа поста' }))
    await waitFor(() => expect(uploadMedia).toHaveBeenCalledTimes(1))
    expect(saveSelectedMedia).toHaveBeenCalledWith(expect.any(String), expect.any(File))
    expect(vi.mocked(saveSelectedMedia).mock.invocationCallOrder[0])
      .toBeLessThan(vi.mocked(uploadMedia).mock.invocationCallOrder[0])
    expect(await screen.findByText('#9')).toBeInTheDocument()
  })
})
