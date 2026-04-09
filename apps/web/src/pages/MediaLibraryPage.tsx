import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { deleteMedia, fetchMediaPage, uploadMedia } from '../api/media'

export function MediaLibraryPage() {
  const qc = useQueryClient()
  const list = useQuery({
    queryKey: ['media', 0],
    queryFn: () => fetchMediaPage(0, 48),
  })

  const upload = useMutation({
    mutationFn: (file: File) => uploadMedia(file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['media'] }),
  })

  const del = useMutation({
    mutationFn: (id: number) => deleteMedia(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['media'] }),
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">Медиа</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">Загрузки привязаны к текущему воркспейсу</p>
        </div>
        <Link
          to="/app/feed"
          className="text-sm text-[var(--muted)] hover:text-[var(--text)] hover:underline"
        >
          ← к ленте
        </Link>
      </div>

      <label className="flex max-w-md cursor-pointer flex-col gap-2 rounded-xl border border-dashed border-[var(--border)] bg-[var(--surface)] p-4 text-sm">
        <span className="font-medium">Загрузить файл</span>
        <input
          type="file"
          className="text-xs file:mr-3 file:rounded-md file:border file:border-[var(--border)] file:bg-[var(--bg)] file:px-2 file:py-1"
          disabled={upload.isPending}
          onChange={(e) => {
            const f = e.target.files?.[0]
            e.target.value = ''
            if (f) {
              upload.mutate(f)
            }
          }}
        />
        {upload.isError ? <span className="text-xs text-red-600">Не удалось загрузить</span> : null}
      </label>

      {list.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка списка…</p>
      ) : list.isError ? (
        <p className="text-sm text-red-600">Ошибка списка</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {(list.data?.content ?? []).map((m) => (
            <li
              key={m.id}
              className="flex flex-col gap-2 rounded-lg border border-[var(--border)] bg-[var(--surface)] p-3 text-xs"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="font-mono text-[var(--text)]">#{m.id}</span>
                <button
                  type="button"
                  className="text-red-600 hover:underline disabled:opacity-50"
                  disabled={del.isPending}
                  onClick={() => {
                    if (confirm('Удалить файл?')) {
                      del.mutate(m.id)
                    }
                  }}
                >
                  Удалить
                </button>
              </div>
              <div className="text-[var(--muted)]">
                <div>{m.type}</div>
                {m.mimeType ? <div>{m.mimeType}</div> : null}
                {m.sizeBytes != null ? <div>{Math.round(m.sizeBytes / 1024)} KB</div> : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
