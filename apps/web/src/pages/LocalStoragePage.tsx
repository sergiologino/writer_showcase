import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LocalMediaThumb } from '../components/LocalMediaThumb'
import { Seo } from '../components/Seo'
import { choosePostFolder, folderPickerAvailable, inspectLocalArchive, type LocalArchiveItem } from '../lib/localPosts'

export function LocalStoragePage() {
  const [folderName, setFolderName] = useState<string | null>(null)
  const [posts, setPosts] = useState<LocalArchiveItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = async () => {
    setBusy(true)
    try {
      const archive = await inspectLocalArchive()
      setFolderName(archive.folderName)
      setPosts(archive.posts)
      setError(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Не удалось прочитать папку.')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => { void refresh() }, [])

  return (
    <div className="space-y-6">
      <Seo title="Локальное хранилище" description="Текст, медиа и параметры публикации в папке Publisher." noIndex />
      <div>
        <Link to="/app/profile" className="text-sm text-[var(--accent)] hover:underline">← В профиль</Link>
        <h1 className="mt-3 text-2xl font-semibold">Локальное хранилище</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Структура папки: publisher / ID материала / content.md, media, parameters.json.
          ID материала постоянный и сохраняется до появления серверного ID.
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4">
        <button
          type="button"
          disabled={!folderPickerAvailable() || busy}
          className="rounded-lg bg-[var(--accent)] px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
          onClick={() => {
            void choosePostFolder().then(() => refresh()).catch((reason: unknown) => {
              if (reason instanceof DOMException && reason.name === 'AbortError') return
              setError(reason instanceof Error ? reason.message : 'Не удалось выбрать папку.')
            })
          }}
        >
          Выбрать папку
        </button>
        <button type="button" onClick={() => void refresh()} disabled={busy}
          className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm disabled:opacity-50">
          Обновить
        </button>
        <span className="text-sm text-[var(--muted)]">{folderName ? `${folderName}/publisher` : 'Папка не выбрана'}</span>
      </div>
      {error ? <p role="alert" className="text-sm text-red-600">{error}</p> : null}
      {posts.length === 0 ? (
        <p className="text-sm text-[var(--muted)]">Материалов в папке пока нет.</p>
      ) : (
        <ul className="space-y-4">
          {posts.map((post) => (
            <li key={post.localId} className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm">
              <h2 className="font-medium">{post.title}</h2>
              <p className="mt-1 font-mono text-xs text-[var(--muted)]">{post.localId} · серверный ID: {post.serverId ?? 'ещё нет'}</p>
              <p className="mt-1 text-xs text-[var(--muted)]">{post.status} · сохранено: {post.savedAt || 'неизвестно'}</p>
              {post.contentPreview ? <p className="mt-3 whitespace-pre-wrap text-sm">{post.contentPreview}</p> : null}
              {post.media.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-3">
                  {post.media.map((media) => (
                    <div key={media.fileName} className="max-w-32 text-xs text-[var(--muted)]">
                      <LocalMediaThumb localId={post.localId} fileName={media.fileName} mimeType={media.mimeType} />
                      <p className="mt-1 break-all">{media.originalName || media.fileName}</p>
                    </div>
                  ))}
                </div>
              ) : null}
              {post.publication.publishedAt ? (
                <p className="mt-3 text-xs">Свой блог: {post.publication.publishedAt}{post.publication.publicUrl ? ` · ${post.publication.publicUrl}` : ''}</p>
              ) : null}
              {post.publication.channels.length > 0 ? (
                <ul className="mt-2 space-y-1 text-xs text-[var(--muted)]">
                  {post.publication.channels.map((channel) => (
                    <li key={channel.channelType}>
                      {channel.channelType}: {channel.status}
                      {channel.sentAt ? ` · ${channel.sentAt}` : ''}
                      {channel.externalUrl ? ` · ${channel.externalUrl}` : ''}
                    </li>
                  ))}
                </ul>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
