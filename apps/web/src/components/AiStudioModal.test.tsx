import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { studioInvoke } from '../api/aiStudio'
import { AiStudioModal } from './AiStudioModal'

vi.mock('../api/aiStudio', () => ({ studioInvoke: vi.fn() }))

describe('AiStudioModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })
  afterEach(() => vi.unstubAllGlobals())

  it('uses a clear name for splitting text and omits the technical introduction', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <AiStudioModal open onClose={vi.fn()} originalBody="Текст поста" onApplyToArticle={vi.fn()} />
      </QueryClientProvider>,
    )

    expect(screen.getByRole('button', { name: 'Разбить на посты' })).toBeInTheDocument()
    expect(screen.queryByText(/noteapp-ai-integration/)).not.toBeInTheDocument()
  })

  it('starts with an editing prompt and applies the finished text to the article', async () => {
    vi.mocked(studioInvoke).mockResolvedValue({
      ok: true, output: 'Улучшенный текст', errorCode: null, tokensUsed: 12, postTokensTotal: null,
    })
    const onApply = vi.fn()
    const client = new QueryClient()
    render(<QueryClientProvider client={client}>
      <AiStudioModal open initialMode="text" onClose={vi.fn()} originalBody="Черновик" onApplyToArticle={onApply} />
    </QueryClientProvider>)

    expect(screen.getByRole('textbox', { name: 'Контекст статьи' })).toHaveValue('Черновик')
    expect((screen.getByRole('textbox', { name: '2. Что сделать' }) as HTMLTextAreaElement).value).toContain('сохрани смысл')
    fireEvent.click(screen.getByRole('button', { name: 'Получить вариант текста' }))
    await waitFor(() => expect(studioInvoke).toHaveBeenCalledWith(expect.objectContaining({
      requestType: 'chat', payload: expect.objectContaining({ messages: expect.any(Array) }),
    })))
    fireEvent.click(await screen.findByRole('button', { name: 'Готово — заменить текст поста' }))
    expect(onApply).toHaveBeenCalledWith('Улучшенный текст')
  })

  it('uses image_edit for a reference and returns the generated file', async () => {
    const imageDataUrl = 'data:image/png;base64,iVBORw0KGgo='
    vi.mocked(studioInvoke).mockResolvedValue({
      ok: true, output: 'Изображение готово', imageDataUrl,
      errorCode: null, tokensUsed: null, postTokensTotal: null,
    })
    const onApplyImage = vi.fn().mockResolvedValue(undefined)
    const fetchImage = vi.fn().mockResolvedValue({ blob: async () => new Blob(['image'], { type: 'image/png' }) })
    vi.stubGlobal('fetch', fetchImage)
    const client = new QueryClient()
    render(<QueryClientProvider client={client}>
      <AiStudioModal open initialMode="image" onClose={vi.fn()} originalBody="Статья о саде"
        onApplyToArticle={vi.fn()} onApplyImage={onApplyImage} />
    </QueryClientProvider>)

    expect((screen.getByRole('textbox', { name: '2. Что сделать' }) as HTMLTextAreaElement).value).toContain('иллюстрацию')
    fireEvent.change(screen.getByLabelText('Изображение-референс (необязательно)'), {
      target: { files: [new File(['reference'], 'reference.png', { type: 'image/png' })] },
    })
    await screen.findByRole('img', { name: 'Выбранный референс' })
    fireEvent.change(screen.getByRole('textbox', { name: 'Что сделать с референсом' }), {
      target: { value: 'Замени фон' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Создать изображение' }))
    await waitFor(() => expect(studioInvoke).toHaveBeenCalledWith(expect.objectContaining({
      requestType: 'image_edit',
      payload: expect.objectContaining({ imageBase64: expect.stringContaining('data:image/png;base64,'),
        prompt: expect.stringContaining('Замени фон') }),
    })))
    fireEvent.click(await screen.findByRole('button', { name: 'Готово — добавить в медиа поста' }))
    await waitFor(() => expect(onApplyImage).toHaveBeenCalledWith(expect.any(File)))
  })
})
