import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LandingPage } from './LandingPage'

vi.mock('../components/ThemeToggleButton', () => ({
  ThemeToggleButton: () => <button type="button">Тема</button>,
}))

describe('LandingPage', () => {
  it('keeps the product message, sections and working account links without invented metrics', async () => {
    render(<MemoryRouter><LandingPage /></MemoryRouter>)

    expect(screen.getByRole('heading', { level: 1, name: 'Ваш текст сначала принадлежит вам.' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Пишите в одном месте' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Публикуйте дальше' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ускоряйте редактуру' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'От заметки до публичного архива' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Создать авторское пространство/ })).toHaveAttribute('href', '/register')
    expect(screen.getByRole('link', { name: 'У меня уже есть аккаунт' })).toHaveAttribute('href', '/login')
    expect(screen.queryByText(/3 черновика, 1 публикация, 4 канала/)).not.toBeInTheDocument()

    const hero = screen.getByRole('img', { name: 'Рабочий стол автора с ноутбуком и заметками' })
    expect(hero).toHaveAttribute('width', '1200')
    expect(hero).toHaveAttribute('height', '1500')
    expect(hero).toHaveAttribute('srcset')
    await waitFor(() => expect(document.head.querySelector('script[type="application/ld+json"]')?.textContent).toContain('"@type":"WebSite"'))
    expect(document.head.querySelector('link[rel="canonical"]')).toHaveAttribute('href', 'http://localhost:3000/')
  })
})
