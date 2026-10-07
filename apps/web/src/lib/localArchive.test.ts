import { describe, expect, it } from 'vitest'
import { writePostArchive, writeSelectedMedia, type LocalPost } from './localPosts'

class MemoryDirectory {
  readonly directories = new Map<string, MemoryDirectory>()
  readonly files = new Map<string, string | Blob>()

  async getDirectoryHandle(name: string, options?: { create: boolean }): Promise<MemoryDirectory> {
    if (!this.directories.has(name) && options?.create) this.directories.set(name, new MemoryDirectory())
    const directory = this.directories.get(name)
    if (!directory) throw new DOMException('Missing', 'NotFoundError')
    return directory
  }

  async getFileHandle(name: string, options?: { create: boolean }) {
    if (!this.files.has(name) && !options?.create) throw new DOMException('Missing', 'NotFoundError')
    return {
      createWritable: async () => ({
        write: async (data: string | Blob) => { this.files.set(name, data) },
        close: async () => {},
      }),
    }
  }
}

describe('local post archive', () => {
  it('writes the selected image and post files under publisher/stable-id', async () => {
    const root = new MemoryDirectory()
    const localId = 'stable-id'
    const image = new File(['image bytes'], 'cover.png', { type: 'image/png' })
    const folder = root as unknown as Parameters<typeof writeSelectedMedia>[0]
    const media = await writeSelectedMedia(folder, localId, image)
    const post: LocalPost = {
      key: '7:stable-id', localId, serverId: null, savedAt: '2026-10-07T12:00:00Z',
      payload: {
        title: 'Пост с обложкой', slug: '', excerpt: '', bodySource: '# Текст', bodyHtml: '',
        visibility: 'PRIVATE', status: 'DRAFT', categoryId: null, tagIds: [], aiGenerated: false,
        mediaAssetIds: [], socialPublishEnabled: false,
      },
      media: [media],
      publication: {
        publishedAt: '2026-10-07T13:00:00Z', publicUrl: 'https://example.com/blog/post',
        channels: [{ channelType: 'TELEGRAM', status: 'SENT', sentAt: '2026-10-07T13:01:00Z',
          externalUrl: 'https://t.me/channel/123' }],
      },
    }
    await writePostArchive(folder, post)

    const postFolder = root.directories.get('publisher')!.directories.get(localId)!
    expect(postFolder.files.get('content.md')).toBe('# Текст')
    expect(postFolder.directories.get('media')!.files.get(media.fileName)).toBe(image)
    const parameters = JSON.parse(postFolder.files.get('parameters.json') as string) as Record<string, unknown>
    expect(parameters).toMatchObject({ localId, serverId: null, title: 'Пост с обложкой' })
    expect(parameters.media).toEqual([media])
    expect(parameters.publication).toEqual(post.publication)
  })
})
