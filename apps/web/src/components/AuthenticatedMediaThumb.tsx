import { useEffect, useState } from 'react'
import { getStoredAccessToken, resolveApiUrl } from '../api/client'

type Props = {
  mediaId: number
  mimeType: string | null
}

/**
 * Превью файла из `/api/media/{id}/file` с заголовком Authorization (обычный &lt;img src&gt; его не шлёт).
 */
export function AuthenticatedMediaThumb({ mediaId, mimeType }: Props) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let revoked = false
    const fileUrl = resolveApiUrl(`/api/media/${mediaId}/file`)
    const token = getStoredAccessToken()
    ;(async () => {
      try {
        const res = await fetch(fileUrl, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        })
        if (!res.ok) {
          throw new Error(String(res.status))
        }
        const blob = await res.blob()
        if (revoked) {
          return
        }
        setObjectUrl(URL.createObjectURL(blob))
      } catch {
        if (!revoked) {
          setFailed(true)
        }
      }
    })()
    return () => {
      revoked = true
      setObjectUrl((prev) => {
        if (prev) {
          URL.revokeObjectURL(prev)
        }
        return null
      })
    }
  }, [mediaId])

  const isImage = mimeType?.startsWith('image/')
  const isVideo = mimeType?.startsWith('video/')

  if (failed || !objectUrl) {
    return (
      <div className="flex h-20 w-24 shrink-0 items-center justify-center rounded border border-[var(--border)] bg-[var(--bg)] text-[10px] text-[var(--muted)]">
        {failed ? 'нет превью' : '…'}
      </div>
    )
  }

  if (isImage) {
    return (
      <img
        src={objectUrl}
        alt=""
        className="h-20 w-24 shrink-0 rounded border border-[var(--border)] object-cover"
      />
    )
  }

  if (isVideo) {
    return (
      <video
        src={objectUrl}
        className="h-20 w-24 shrink-0 rounded border border-[var(--border)] object-cover"
        muted
        playsInline
        preload="metadata"
      />
    )
  }

  return (
    <div className="flex h-20 w-24 shrink-0 flex-col items-center justify-center rounded border border-[var(--border)] bg-[var(--bg)] px-1 text-center text-[10px] text-[var(--muted)]">
      <span className="line-clamp-3">{mimeType ?? 'файл'}</span>
    </div>
  )
}
