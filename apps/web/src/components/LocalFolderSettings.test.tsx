import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { changePostFolder, choosePostFolder, ensurePostFolderWriteAccess, postFolderStatus } from '../lib/localPosts'
import { LocalFolderSettings } from './LocalFolderSettings'

vi.mock('../lib/localPosts', () => ({
  folderPickerAvailable: vi.fn().mockReturnValue(true),
  postFolderStatus: vi.fn(),
  choosePostFolder: vi.fn(),
  ensurePostFolderWriteAccess: vi.fn(),
  changePostFolder: vi.fn(),
}))

describe('profile folder settings', () => {
  beforeEach(() => vi.clearAllMocks())

  it('restores permission to the saved folder without picking it again', async () => {
    vi.mocked(postFolderStatus).mockResolvedValueOnce({ name: 'Documents', permission: 'prompt' })
      .mockResolvedValueOnce({ name: 'Documents', permission: 'granted' })
    vi.mocked(ensurePostFolderWriteAccess).mockResolvedValue('Documents')
    render(<LocalFolderSettings />)

    fireEvent.click(await screen.findByRole('button', { name: 'Восстановить доступ' }))
    await waitFor(() => expect(ensurePostFolderWriteAccess).toHaveBeenCalledTimes(1))
    expect(choosePostFolder).not.toHaveBeenCalled()
    expect(await screen.findByText('Доступ к Documents/publisher восстановлен.')).toBeInTheDocument()
  })

  it('shows a retained-old-copy warning after changing the folder', async () => {
    vi.mocked(postFolderStatus).mockResolvedValue({ name: 'Documents', permission: 'granted' })
    vi.mocked(changePostFolder).mockResolvedValue({ name: 'Archive', previousRetained: true })
    render(<LocalFolderSettings />)

    fireEvent.click(await screen.findByRole('button', { name: 'Сменить папку и перенести материалы' }))
    expect(await screen.findByText(/Старую копию браузер не смог удалить/)).toBeInTheDocument()
  })
})
