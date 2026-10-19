import { describe, expect, it } from 'vitest'
import { movePostArchive } from './localPosts'

class Directory {
  readonly dirs = new Map<string, Directory>()
  readonly files = new Map<string, File>()
  constructor(readonly name: string) {}

  async isSameEntry(other: Directory) { return this === other }
  async resolve(other: Directory): Promise<string[] | null> {
    if (this === other) return []
    for (const [name, child] of this.dirs) {
      const path = await child.resolve(other)
      if (path) return [name, ...path]
    }
    return null
  }
  async getDirectoryHandle(name: string, options?: { create: boolean }) {
    if (!this.dirs.has(name) && options?.create) this.dirs.set(name, new Directory(name))
    const child = this.dirs.get(name)
    if (!child) throw new DOMException('Missing', 'NotFoundError')
    return child
  }
  async getFileHandle(name: string, options?: { create: boolean }) {
    if (!this.files.has(name) && !options?.create) throw new DOMException('Missing', 'NotFoundError')
    return {
      getFile: async () => this.files.get(name)!,
      createWritable: async () => ({
        write: async (data: string | Blob) => {
          this.files.set(name, new File([data], name))
        },
        close: async () => {},
      }),
    }
  }
  async *values() {
    for (const name of this.dirs.keys()) yield { kind: 'directory', name }
    for (const name of this.files.keys()) yield { kind: 'file', name }
  }
  async removeEntry(name: string) { this.dirs.delete(name); this.files.delete(name) }
}

const handle = (directory: Directory) => directory as unknown as Parameters<typeof movePostArchive>[0]

describe('moving the Publisher archive', () => {
  it('copies text, parameters, and binary media into a new folder', async () => {
    const oldRoot = new Directory('Old')
    const newRoot = new Directory('New')
    const post = await (await oldRoot.getDirectoryHandle('publisher', { create: true }))
      .getDirectoryHandle('post-id', { create: true })
    post.files.set('content.md', new File(['# Материал'], 'content.md'))
    post.files.set('parameters.json', new File(['{"status":"DRAFT"}'], 'parameters.json'))
    const media = await post.getDirectoryHandle('media', { create: true })
    media.files.set('cover.png', new File([new Uint8Array([0, 1, 2, 255])], 'cover.png'))

    expect(await movePostArchive(handle(oldRoot), handle(newRoot))).toBe(true)
    const moved = await (await newRoot.getDirectoryHandle('publisher')).getDirectoryHandle('post-id')
    expect(await moved.files.get('content.md')!.text()).toBe('# Материал')
    expect(await moved.files.get('parameters.json')!.text()).toBe('{"status":"DRAFT"}')
    expect(Array.from(new Uint8Array(await (await moved.getDirectoryHandle('media')).files.get('cover.png')!.arrayBuffer())))
      .toEqual([0, 1, 2, 255])
    expect(await post.files.get('content.md')!.text()).toBe('# Материал')
  })

  it('does not overwrite a different archive at the destination', async () => {
    const oldRoot = new Directory('Old')
    const newRoot = new Directory('New')
    await oldRoot.getDirectoryHandle('publisher', { create: true })
    await newRoot.getDirectoryHandle('publisher', { create: true })
    await expect(movePostArchive(handle(oldRoot), handle(newRoot))).rejects.toThrow('уже есть папка publisher')
  })

  it('rejects a destination inside the current archive', async () => {
    const oldRoot = new Directory('Old')
    const publisher = await oldRoot.getDirectoryHandle('publisher', { create: true })
    const nested = await publisher.getDirectoryHandle('nested', { create: true })
    await expect(movePostArchive(handle(oldRoot), handle(nested))).rejects.toThrow('внутри текущего архива')
  })
})
