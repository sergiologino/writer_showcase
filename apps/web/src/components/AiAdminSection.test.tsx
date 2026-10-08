import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchAiRouting, fetchAvailableNetworks, saveAiRouting } from '../api/aiAdmin'
import { ApiError } from '../api/client'
import { AiAdminSection } from './AiAdminSection'

vi.mock('../api/aiAdmin', () => ({
  fetchAiRouting: vi.fn(),
  fetchAvailableNetworks: vi.fn(),
  saveAiRouting: vi.fn(),
}))

function renderSection() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><AiAdminSection /></QueryClientProvider>)
}

describe('AiAdminSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(fetchAvailableNetworks).mockResolvedValue([
      { id: '1', name: 'alpha', displayName: 'Alpha', networkType: 'chat' },
      { id: '2', name: 'beta', displayName: 'Beta', networkType: 'chat' },
      { id: '3', name: 'vision', displayName: 'Vision', networkType: 'image_generation' },
    ])
    vi.mocked(fetchAiRouting).mockResolvedValue({ chat: ['alpha'] })
    vi.mocked(saveAiRouting).mockResolvedValue()
  })

  it('selects available networks by task type and saves their priority order', async () => {
    renderSection()
    const chatSelect = await screen.findByRole('combobox', { name: 'Добавить сеть для chat' })
    await waitFor(() => expect(within(chatSelect).getByRole('option', { name: 'Beta (beta)' })).toBeInTheDocument())
    expect(within(chatSelect).queryByRole('option', { name: /Vision/ })).not.toBeInTheDocument()

    fireEvent.change(chatSelect, { target: { value: 'beta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Поднять beta для chat' }))
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить приоритеты' }))
    await waitFor(() => expect(saveAiRouting).toHaveBeenCalledWith({
      chat: ['beta', 'alpha'], image_generation: [], video_generation: [], transcription: [], speech_synthesis: [],
    }))
  })

  it('keeps saved unavailable names visible until an admin removes them', async () => {
    vi.mocked(fetchAiRouting).mockResolvedValue({ chat: ['old-network'] })
    renderSection()
    expect(await screen.findByText('нет в доступном списке')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Сохранить приоритеты' })).toBeEnabled())
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить приоритеты' }))
    await waitFor(() => expect(saveAiRouting).toHaveBeenCalledWith(expect.objectContaining({ chat: ['old-network'] })))
    fireEvent.click(screen.getByRole('button', { name: 'Убрать old-network из chat' }))
    expect(screen.getAllByText('Автовыбор интеграции')).toHaveLength(5)
  })

  it('shows the upstream error instead of treating it as an empty list', async () => {
    vi.mocked(fetchAvailableNetworks).mockRejectedValue(new ApiError(502, 'Сервис нейросетей ответил HTTP 502'))
    renderSection()
    expect(await screen.findByText('Сервис нейросетей ответил HTTP 502')).toBeInTheDocument()
    expect(screen.queryByText('Для Publisher пока нет доступных нейросетей.')).not.toBeInTheDocument()
  })
})
