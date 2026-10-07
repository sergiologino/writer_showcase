import { describe, expect, it } from 'vitest'
import { deserializePost, localPostKey, serializePost, type LocalPost } from './localPosts'

describe('portable post source', () => {
  it('keeps identity, publishing metadata and readable Markdown text', () => {
    const post: LocalPost = {
      key: localPostKey('42', 'a1b2c3'),
      localId: 'a1b2c3',
      serverId: null,
      savedAt: '2026-10-07T10:00:00.000Z',
      payload: {
        title: 'Заметка: «кавычки»', slug: 'note', excerpt: 'Кратко',
        bodySource: '# Заголовок\n\nТекст на русском.', bodyHtml: '<h1>Заголовок</h1>',
        status: 'DRAFT', visibility: 'PRIVATE', categoryId: null, tagIds: [],
        aiGenerated: false, mediaAssetIds: [], socialPublishEnabled: false,
        publishChannels: null, scheduledPublishAt: null,
      },
    }
    const source = serializePost(post)
    expect(source).toContain('local_id: "a1b2c3"')
    expect(source).toContain('title: "Заметка: «кавычки»"')
    expect(source).toContain('status: "DRAFT"')
    expect(source).toContain('\n---\n\n# Заголовок\n\nТекст на русском.\n')
    expect(deserializePost(source)).toEqual(post)
    expect(localPostKey('42', 7)).toBe('42:7')
  })
})
