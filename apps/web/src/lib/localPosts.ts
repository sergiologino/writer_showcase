import type { PostPayload } from '../api/posts'
import { getStoredAccessToken, resolveApiUrl } from '../api/client'
import type { PostResponse } from '../api/types'

export interface LocalMedia {
  fileName: string
  mimeType: string | null
  serverId: number | null
  originalName: string | null
}

export interface LocalPublication {
  publishedAt: string | null
  publicUrl: string | null
  channels: Array<{
    channelType: string
    status: string
    sentAt: string | null
    externalUrl: string | null
  }>
}

export interface LocalArchiveItem {
  localId: string
  title: string
  serverId: number | null
  status: string
  savedAt: string
  contentPreview: string
  publication: LocalPublication
  media: LocalMedia[]
}

export interface LocalPost {
  key: string
  localId: string
  serverId: number | null
  savedAt: string
  payload: PostPayload
  media?: LocalMedia[]
  publication?: LocalPublication
}

interface DirectoryHandle {
  name: string
  isSameEntry(other: DirectoryHandle): Promise<boolean>
  resolve(other: DirectoryHandle): Promise<string[] | null>
  queryPermission(options: { mode: 'readwrite' }): Promise<PermissionState>
  requestPermission(options: { mode: 'readwrite' }): Promise<PermissionState>
  getDirectoryHandle(name: string, options?: { create: boolean }): Promise<DirectoryHandle>
  removeEntry(name: string, options?: { recursive: boolean }): Promise<void>
  getFileHandle(name: string, options?: { create: boolean }): Promise<{
    createWritable(): Promise<{ write(data: string | Blob): Promise<void>; close(): Promise<void> }>
    getFile(): Promise<File>
  }>
  values(): AsyncIterable<{ kind: string; name: string; getFile?: () => Promise<File> }>
}

type PickerWindow = Window & { showDirectoryPicker?: () => Promise<DirectoryHandle> }

const DB_NAME = 'publisher-local-posts'
const DB_VERSION = 1
const POSTS = 'posts'
const SETTINGS = 'settings'
const FOLDER_KEY = 'postFolder'

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(POSTS)) db.createObjectStore(POSTS, { keyPath: 'key' })
      if (!db.objectStoreNames.contains(SETTINGS)) db.createObjectStore(SETTINGS)
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function read<T>(store: string, key: string): Promise<T | undefined> {
  const db = await openDatabase()
  try {
    return await new Promise<T | undefined>((resolve, reject) => {
      const request = db.transaction(store, 'readonly').objectStore(store).get(key)
      request.onsuccess = () => resolve(request.result as T | undefined)
      request.onerror = () => reject(request.error)
    })
  } finally {
    db.close()
  }
}

async function write(store: string, value: unknown, key?: string): Promise<void> {
  const db = await openDatabase()
  try {
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(store, 'readwrite')
      if (key === undefined) tx.objectStore(store).put(value)
      else tx.objectStore(store).put(value, key)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
      tx.onabort = () => reject(tx.error)
    })
  } finally {
    db.close()
  }
}

export function localPostKey(workspaceId: string, id: number | string): string {
  return `${workspaceId}:${id}`
}

export function folderPickerAvailable(): boolean {
  return typeof (window as PickerWindow).showDirectoryPicker === 'function'
}

export async function choosePostFolder(): Promise<string> {
  const saved = await getPostFolder()
  if (saved) {
    await ensurePostFolderWriteAccess()
    return saved.name
  }
  const picker = (window as PickerWindow).showDirectoryPicker
  if (!picker) throw new Error('Выбор папки не поддерживается этим браузером.')
  const folder = await picker()
  if ((await folder.requestPermission({ mode: 'readwrite' })) !== 'granted') {
    throw new Error('Нет разрешения на запись в выбранную папку.')
  }
  await write(SETTINGS, folder, FOLDER_KEY)
  return folder.name
}

export async function getPostFolder(): Promise<DirectoryHandle | undefined> {
  return read<DirectoryHandle>(SETTINGS, FOLDER_KEY)
}

export async function postFolderStatus(): Promise<{ name: string | null; permission: PermissionState | null }> {
  const folder = await getPostFolder()
  if (!folder) return { name: null, permission: null }
  return { name: folder.name, permission: await folder.queryPermission({ mode: 'readwrite' }) }
}

export async function ensurePostFolderWriteAccess(): Promise<string> {
  const folder = await getPostFolder()
  if (!folder) throw new Error('Сначала выберите папку материалов в профиле.')
  const mode = { mode: 'readwrite' as const }
  if (await folder.queryPermission(mode) !== 'granted' && await folder.requestPermission(mode) !== 'granted') {
    throw new Error('Браузер не предоставил доступ к сохранённой папке материалов.')
  }
  return folder.name
}

async function copyDirectory(source: DirectoryHandle, target: DirectoryHandle): Promise<void> {
  for await (const entry of source.values()) {
    if (entry.kind === 'directory') {
      const childSource = await source.getDirectoryHandle(entry.name)
      const childTarget = await target.getDirectoryHandle(entry.name, { create: true })
      await copyDirectory(childSource, childTarget)
    } else if (entry.kind === 'file') {
      const file = await (await source.getFileHandle(entry.name)).getFile()
      await writeFile(target, entry.name, file)
      const copied = await (await target.getFileHandle(entry.name)).getFile()
      if (!await filesMatch(file, copied)) throw new Error(`Не удалось проверить копию файла ${entry.name}.`)
    }
  }
}

async function filesMatch(source: File, target: File): Promise<boolean> {
  if (source.size !== target.size) return false
  const chunkSize = 1024 * 1024
  for (let offset = 0; offset < source.size; offset += chunkSize) {
    const [left, right] = await Promise.all([
      source.slice(offset, offset + chunkSize).arrayBuffer(),
      target.slice(offset, offset + chunkSize).arrayBuffer(),
    ])
    const a = new Uint8Array(left)
    const b = new Uint8Array(right)
    for (let i = 0; i < a.length; i++) if (a[i] !== b[i]) return false
  }
  return true
}

/** Copy first, then switch the saved folder; never overwrite an existing archive. */
export async function movePostArchive(sourceRoot: DirectoryHandle, targetRoot: DirectoryHandle): Promise<boolean> {
  if (await sourceRoot.isSameEntry(targetRoot)) return false
  let source: DirectoryHandle
  try {
    source = await sourceRoot.getDirectoryHandle('publisher')
  } catch (error) {
    if (error instanceof DOMException && error.name === 'NotFoundError') return false
    throw error
  }
  if (await source.resolve(targetRoot) !== null) {
    throw new Error('Новая папка не должна находиться внутри текущего архива publisher.')
  }
  try {
    await targetRoot.getDirectoryHandle('publisher')
    throw new Error('В новом месте уже есть папка publisher. Выберите папку без существующего архива.')
  } catch (error) {
    if (!(error instanceof DOMException) || error.name !== 'NotFoundError') throw error
  }
  const target = await targetRoot.getDirectoryHandle('publisher', { create: true })
  try {
    await copyDirectory(source, target)
  } catch (error) {
    await targetRoot.removeEntry('publisher', { recursive: true }).catch(() => {})
    throw error
  }
  return true
}

export async function changePostFolder(): Promise<{ name: string; previousRetained: boolean }> {
  const picker = (window as PickerWindow).showDirectoryPicker
  if (!picker) throw new Error('Выбор папки не поддерживается этим браузером.')
  const previous = await getPostFolder()
  if (previous && await previous.queryPermission({ mode: 'readwrite' }) !== 'granted') {
    throw new Error('Сначала восстановите доступ к старой папке, затем повторите перенос.')
  }
  const next = await picker()
  if (await next.requestPermission({ mode: 'readwrite' }) !== 'granted') {
    throw new Error('Нет разрешения на запись в новую папку.')
  }
  const copied = previous ? await movePostArchive(previous, next) : false
  try {
    await write(SETTINGS, next, FOLDER_KEY)
  } catch (error) {
    if (copied) await next.removeEntry('publisher', { recursive: true }).catch(() => {})
    throw error
  }
  if (!previous || !copied) return { name: next.name, previousRetained: false }
  try {
    await previous.removeEntry('publisher', { recursive: true })
    return { name: next.name, previousRetained: false }
  } catch {
    return { name: next.name, previousRetained: true }
  }
}

function extension(name: string, mimeType: string | null): string {
  const fromName = /\.([a-z0-9]{1,8})$/i.exec(name)?.[1]?.toLowerCase()
  if (fromName) return fromName
  const types: Record<string, string> = {
    'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp',
    'image/gif': 'gif', 'image/svg+xml': 'svg', 'video/mp4': 'mp4',
    'video/webm': 'webm', 'audio/mpeg': 'mp3', 'audio/wav': 'wav',
    'audio/ogg': 'ogg', 'application/pdf': 'pdf',
  }
  return types[mimeType ?? ''] ?? 'bin'
}

async function postDirectory(root: DirectoryHandle, localId: string, create: boolean): Promise<DirectoryHandle> {
  const publisher = await root.getDirectoryHandle('publisher', { create })
  return publisher.getDirectoryHandle(localId, { create })
}

async function writeFile(directory: DirectoryHandle, name: string, data: string | Blob): Promise<void> {
  const file = await directory.getFileHandle(name, { create: true })
  const writable = await file.createWritable()
  await writable.write(data)
  await writable.close()
}

async function requireFolder(): Promise<DirectoryHandle> {
  const folder = await getPostFolder()
  if (!folder || (await folder.queryPermission({ mode: 'readwrite' })) !== 'granted') {
    throw new Error('Выберите папку для материалов или восстановите разрешение на запись.')
  }
  return folder
}

export async function writeSelectedMedia(root: DirectoryHandle, localId: string, file: File): Promise<LocalMedia> {
  const post = await postDirectory(root, localId, true)
  const media = await post.getDirectoryHandle('media', { create: true })
  const fileName = `${crypto.randomUUID()}.${extension(file.name, file.type)}`
  await writeFile(media, fileName, file)
  return { fileName, mimeType: file.type || null, serverId: null, originalName: file.name }
}

export async function saveSelectedMedia(localId: string, file: File): Promise<LocalMedia> {
  await ensurePostFolderWriteAccess()
  return writeSelectedMedia(await requireFolder(), localId, file)
}

export async function readLocalMedia(localId: string, fileName: string): Promise<File> {
  const root = await requireFolder()
  const post = await postDirectory(root, localId, false)
  const media = await post.getDirectoryHandle('media')
  return (await media.getFileHandle(fileName)).getFile()
}

export async function inspectLocalArchive(): Promise<{ folderName: string | null; posts: LocalArchiveItem[] }> {
  const root = await getPostFolder()
  if (!root) return { folderName: null, posts: [] }
  if ((await root.queryPermission({ mode: 'readwrite' })) !== 'granted') {
    throw new Error('Для просмотра папки восстановите разрешение кнопкой «Выбрать папку».')
  }
  let publisher: DirectoryHandle
  try {
    publisher = await root.getDirectoryHandle('publisher')
  } catch (error) {
    if (error instanceof DOMException && error.name === 'NotFoundError') {
      return { folderName: root.name, posts: [] }
    }
    throw error
  }
  const posts: LocalArchiveItem[] = []
  for await (const entry of publisher.values()) {
    if (entry.kind !== 'directory') continue
    try {
      const directory = await publisher.getDirectoryHandle(entry.name)
      const parametersFile = await (await directory.getFileHandle('parameters.json')).getFile()
      const parameters = JSON.parse(await parametersFile.text()) as Record<string, unknown>
      const content = await (await directory.getFileHandle('content.md')).getFile()
      const rawMedia = Array.isArray(parameters.media) ? parameters.media : []
      const rawPublication = typeof parameters.publication === 'object' && parameters.publication !== null
        ? parameters.publication as Record<string, unknown> : {}
      const channels = Array.isArray(rawPublication.channels) ? rawPublication.channels : []
      posts.push({
        localId: entry.name,
        title: typeof parameters.title === 'string' ? parameters.title : entry.name,
        serverId: typeof parameters.serverId === 'number' ? parameters.serverId : null,
        status: typeof parameters.status === 'string' ? parameters.status : 'DRAFT',
        savedAt: typeof parameters.savedAt === 'string' ? parameters.savedAt : '',
        contentPreview: (await content.text()).slice(0, 300),
        publication: {
          publishedAt: typeof rawPublication.publishedAt === 'string' ? rawPublication.publishedAt : null,
          publicUrl: typeof rawPublication.publicUrl === 'string' ? rawPublication.publicUrl : null,
          channels: channels.filter((channel): channel is LocalPublication['channels'][number] =>
            typeof channel === 'object' && channel !== null && typeof channel.channelType === 'string'),
        },
        media: rawMedia.filter((item): item is LocalMedia =>
          typeof item === 'object' && item !== null && typeof item.fileName === 'string'),
      })
    } catch {
      posts.push({
        localId: entry.name, title: entry.name, serverId: null, status: 'Не удалось прочитать',
        savedAt: '', contentPreview: '', publication: { publishedAt: null, publicUrl: null, channels: [] }, media: [],
      })
    }
  }
  posts.sort((a, b) => b.savedAt.localeCompare(a.savedAt))
  return { folderName: root.name, posts }
}

async function fetchServerMedia(id: number): Promise<Blob> {
  const token = getStoredAccessToken()
  const workspaceId = localStorage.getItem('workspaceId')
  const response = await fetch(resolveApiUrl(`/api/media/${id}/file`), {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(workspaceId ? { 'X-Workspace-Id': workspaceId } : {}),
    },
  })
  if (!response.ok) throw new Error(`Не удалось сохранить медиа #${id} локально (${response.status}).`)
  return response.blob()
}

export function publicationFromResponse(post: PostResponse, workspaceSlug?: string): LocalPublication {
  return {
    publishedAt: post.publishedAt,
    publicUrl: post.publishedAt && workspaceSlug
      ? `${window.location.origin}/blog/${encodeURIComponent(workspaceSlug)}/p/${encodeURIComponent(post.slug)}`
      : null,
    channels: (post.outbound ?? []).map((outbound) => ({
      channelType: outbound.channelType,
      status: outbound.deliveryStatus,
      sentAt: outbound.sentAt ?? null,
      externalUrl: outbound.externalUrl,
    })),
  }
}

export function serializeParameters(post: LocalPost): string {
  return `${JSON.stringify({
    schemaVersion: 1,
    workspaceId: post.key.split(':')[0],
    localId: post.localId,
    serverId: post.serverId,
    title: post.payload.title,
    status: post.payload.status,
    visibility: post.payload.visibility,
    savedAt: post.savedAt,
    publication: post.publication ?? { publishedAt: null, publicUrl: null, channels: [] },
    media: post.media ?? [],
  }, null, 2)}\n`
}

export async function loadLocalPost(key: string): Promise<LocalPost | undefined> {
  return read<LocalPost>(POSTS, key)
}

export async function listLocalPosts(workspaceId: string): Promise<LocalPost[]> {
  const db = await openDatabase()
  try {
    const posts = await new Promise<LocalPost[]>((resolve, reject) => {
      const request = db.transaction(POSTS, 'readonly').objectStore(POSTS).getAll()
      request.onsuccess = () => resolve(request.result as LocalPost[])
      request.onerror = () => reject(request.error)
    })
    return posts.filter((post) => post.key.startsWith(`${workspaceId}:`))
      .sort((a, b) => b.savedAt.localeCompare(a.savedAt))
  } finally {
    db.close()
  }
}

export function serializePost(post: LocalPost): string {
  const p = post.payload
  const fields: Record<string, unknown> = {
    workspace_id: post.key.split(':')[0],
    local_id: post.localId,
    server_id: post.serverId,
    saved_at: post.savedAt,
    title: p.title,
    slug: p.slug,
    excerpt: p.excerpt,
    status: p.status,
    visibility: p.visibility,
    category_id: p.categoryId,
    tag_ids: p.tagIds,
    ai_generated: p.aiGenerated,
    media_asset_ids: p.mediaAssetIds,
    body_html: p.bodyHtml,
    social_publish_enabled: p.socialPublishEnabled ?? false,
    publish_channels: p.publishChannels ?? null,
    scheduled_publish_at: p.scheduledPublishAt ?? null,
  }
  const frontMatter = Object.entries(fields)
    .map(([key, value]) => `${key}: ${JSON.stringify(value)}`)
    .join('\n')
  return `---\n${frontMatter}\n---\n\n${p.bodySource.replace(/^\n+/, '')}\n`
}

export function deserializePost(source: string): LocalPost {
  const match = /^---\r?\n([\s\S]*?)\r?\n---\r?\n\r?\n?([\s\S]*)$/.exec(source)
  if (!match) throw new Error('Неверный формат файла материала.')
  const fields: Record<string, unknown> = {}
  for (const line of match[1].split(/\r?\n/)) {
    const colon = line.indexOf(': ')
    if (colon < 1) throw new Error('Неверные метаданные материала.')
    fields[line.slice(0, colon)] = JSON.parse(line.slice(colon + 2)) as unknown
  }
  if (typeof fields.workspace_id !== 'string' || typeof fields.local_id !== 'string' ||
      typeof fields.title !== 'string' || typeof fields.saved_at !== 'string' ||
      (fields.server_id !== null && typeof fields.server_id !== 'number')) {
    throw new Error('Не хватает обязательных метаданных материала.')
  }
  const serverId = fields.server_id as number | null
  return {
    key: localPostKey(fields.workspace_id, serverId ?? fields.local_id),
    localId: fields.local_id,
    serverId,
    savedAt: fields.saved_at,
    payload: {
      title: fields.title,
      slug: typeof fields.slug === 'string' ? fields.slug : '',
      excerpt: typeof fields.excerpt === 'string' ? fields.excerpt : '',
      bodySource: match[2].replace(/\n$/, ''),
      bodyHtml: typeof fields.body_html === 'string' ? fields.body_html : '',
      status: fields.status === 'PUBLISHED' || fields.status === 'REVIEW' || fields.status === 'ARCHIVED'
        ? fields.status : 'DRAFT',
      visibility: fields.visibility === 'PRIVATE' || fields.visibility === 'UNLISTED'
        ? fields.visibility : 'PUBLIC',
      categoryId: typeof fields.category_id === 'number' ? fields.category_id : null,
      tagIds: Array.isArray(fields.tag_ids) ? fields.tag_ids.filter((id): id is number => typeof id === 'number') : [],
      aiGenerated: fields.ai_generated === true,
      mediaAssetIds: Array.isArray(fields.media_asset_ids)
        ? fields.media_asset_ids.filter((id): id is number => typeof id === 'number') : [],
      socialPublishEnabled: fields.social_publish_enabled === true,
      publishChannels: Array.isArray(fields.publish_channels) ? fields.publish_channels as PostPayload['publishChannels'] : null,
      scheduledPublishAt: typeof fields.scheduled_publish_at === 'string' ? fields.scheduled_publish_at : null,
    },
  }
}

export async function importPostFolder(workspaceId: string): Promise<number> {
  const folder = await getPostFolder()
  if (!folder) throw new Error('Сначала выберите папку материалов.')
  let postsFolder: DirectoryHandle
  try {
    postsFolder = await folder.getDirectoryHandle('posts')
  } catch (error) {
    if (error instanceof DOMException && error.name === 'NotFoundError') return 0
    throw error
  }
  let count = 0
  for await (const entry of postsFolder.values()) {
    if (entry.kind !== 'file' || !entry.name.endsWith('.md') || !entry.getFile) continue
    const post = deserializePost(await (await entry.getFile()).text())
    if (post.key.split(':')[0] !== workspaceId) continue
    const current = await loadLocalPost(post.key)
    if (!current || current.savedAt < post.savedAt) {
      await write(POSTS, post)
      count++
    }
  }
  return count
}

export async function writePostArchive(root: DirectoryHandle, post: LocalPost): Promise<LocalPost> {
  const directory = await postDirectory(root, post.localId, true)
  const mediaFolder = await directory.getDirectoryHandle('media', { create: true })
  const media: LocalMedia[] = []
  for (const attachment of post.media ?? []) {
    const fileName = attachment.fileName || (attachment.serverId !== null
      ? `server-${attachment.serverId}.${extension('', attachment.mimeType)}` : '')
    if (!fileName) throw new Error('У вложения нет локального файла.')
    try {
      await mediaFolder.getFileHandle(fileName)
    } catch (error) {
      if (!(error instanceof DOMException) || error.name !== 'NotFoundError' || attachment.serverId === null) {
        throw new Error(`Локальное вложение ${fileName} недоступно.`)
      }
      await writeFile(mediaFolder, fileName, await fetchServerMedia(attachment.serverId))
    }
    media.push({ ...attachment, fileName })
  }
  const complete = { ...post, media }
  await writeFile(directory, 'content.md', post.payload.bodySource)
  await writeFile(directory, 'parameters.json', serializeParameters(complete))
  return complete
}

export async function saveLocalPost(post: LocalPost): Promise<void> {
  // База сохраняет правки даже при потере разрешения на папку; вызывающий код показывает ошибку.
  await write(POSTS, post)
  const root = await requireFolder()
  const complete = await writePostArchive(root, post)
  await write(POSTS, complete)
}

export async function bindServerId(post: LocalPost, serverId: number): Promise<void> {
  const key = localPostKey(post.key.split(':')[0], serverId)
  const db = await openDatabase()
  try {
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(POSTS, 'readwrite')
      const store = tx.objectStore(POSTS)
      store.delete(post.key)
      store.put({ ...post, key, serverId })
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
      tx.onabort = () => reject(tx.error)
    })
  } finally {
    db.close()
  }
}
