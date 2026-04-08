import { describe, expect, it } from 'vitest'
import { resolveApiUrl } from './client'

describe('resolveApiUrl', () => {
  it('keeps relative path when base empty', () => {
    expect(resolveApiUrl('/api/me', '')).toBe('/api/me')
  })

  it('prepends base URL and trims slash', () => {
    expect(resolveApiUrl('/api/me', 'https://api.example.com/')).toBe('https://api.example.com/api/me')
  })

  it('returns absolute URLs unchanged', () => {
    expect(resolveApiUrl('https://other.test/x')).toBe('https://other.test/x')
  })
})
