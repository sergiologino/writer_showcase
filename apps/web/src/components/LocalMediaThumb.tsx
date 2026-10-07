import { useEffect, useState } from 'react'
import { readLocalMedia } from '../lib/localPosts'

export function LocalMediaThumb({ localId, fileName, mimeType }: {
  localId: string
  fileName: string
  mimeType: string | null
}) {
  const [url, setUrl] = useState<string | null>(null)
  useEffect(() => {
    let active = true
    let objectUrl: string | null = null
    void readLocalMedia(localId, fileName).then((file) => {
      objectUrl = URL.createObjectURL(file)
      if (active) setUrl(objectUrl)
      else URL.revokeObjectURL(objectUrl)
    }).catch(() => {})
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [localId, fileName])

  if (!url) return <span className="flex h-20 w-24 items-center justify-center text-xs text-[var(--muted)]">…</span>
  if (mimeType?.startsWith('image/')) {
    return <img src={url} alt="" className="h-20 w-24 rounded border border-[var(--border)] object-cover" />
  }
  if (mimeType?.startsWith('video/')) {
    return <video src={url} className="h-20 w-24 rounded border border-[var(--border)] object-cover" muted playsInline preload="metadata" />
  }
  if (mimeType?.startsWith('audio/')) return <audio src={url} controls className="w-32" />
  return <span className="text-xs text-[var(--muted)]">{mimeType ?? 'Файл'}</span>
}
