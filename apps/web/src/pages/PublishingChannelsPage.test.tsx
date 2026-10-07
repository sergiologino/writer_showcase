import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { PublishingChannelsPage } from './PublishingChannelsPage'

vi.mock('../api/channels', () => ({
  fetchWorkspaceChannels: vi.fn().mockResolvedValue([]),
  upsertWorkspaceChannel: vi.fn(),
}))

describe('PublishingChannelsPage instructions', () => {
  it('shows actionable Facebook and X setup steps and generic delivery wording', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <PublishingChannelsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await screen.findByRole('heading', { name: 'Каналы публикации' })

    const facebook = screen.getByRole('heading', { name: 'Facebook (страница)' }).closest('section')!
    const x = screen.getByRole('heading', { name: 'X (Twitter)' }).closest('section')!

    expect(within(facebook).getByText('Как подключить (пошагово)')).toBeInTheDocument()
    expect(within(facebook).getAllByRole('listitem')).toHaveLength(4)
    expect(within(facebook).getByText('pages_manage_posts')).toBeInTheDocument()
    expect(within(facebook).getByText('/me/accounts')).toBeInTheDocument()

    expect(within(x).getByText('Как подключить (пошагово)')).toBeInTheDocument()
    expect(within(x).getAllByRole('listitem')).toHaveLength(4)
    expect(within(x).getByText('tweet.write')).toBeInTheDocument()
    expect(within(x).getByText(/Токен приложения без авторизации пользователя/)).toBeInTheDocument()

    expect(screen.getByText(/отправляются через специальный промежуточный сервис/)).toBeInTheDocument()
    expect(screen.queryByText(/noteapp-ai-integration/)).not.toBeInTheDocument()
  })
})
