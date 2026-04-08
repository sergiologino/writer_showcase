import { afterEach, describe, expect, it } from 'vitest'
import { applyTheme, getStoredTheme } from './theme'

describe('theme', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('defaults to system when unset', () => {
    expect(getStoredTheme()).toBe('system')
  })

  it('persists light selection', () => {
    applyTheme('light')
    expect(getStoredTheme()).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
