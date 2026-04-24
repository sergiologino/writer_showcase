import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { fetchAiRouting, fetchAvailableNetworks, saveAiRouting } from '../api/aiAdmin'
import { ApiError } from '../api/client'

const REQUEST_TYPES = ['chat', 'image_generation', 'video_generation', 'transcription', 'speech_synthesis'] as const

export function AiAdminSection() {
  const qc = useQueryClient()
  const netsQ = useQuery({ queryKey: ['ai', 'networks'], queryFn: fetchAvailableNetworks })
  const routingQ = useQuery({ queryKey: ['ai', 'routing'], queryFn: fetchAiRouting })
  const [lines, setLines] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!routingQ.data) {
      return
    }
    const o: Record<string, string> = {}
    for (const rt of REQUEST_TYPES) {
      o[rt] = (routingQ.data[rt] ?? []).join('\n')
    }
    setLines(o)
  }, [routingQ.data])

  const save = useMutation({
    mutationFn: () => {
      const routing: Record<string, string[]> = {}
      for (const rt of REQUEST_TYPES) {
        const t = (lines[rt] ?? '')
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean)
        routing[rt] = t
      }
      return saveAiRouting(routing)
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'routing'] }),
  })

  const err = save.error instanceof ApiError ? save.error.message : save.error ? 'Ошибка сохранения' : null

  return (
    <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Нейросети (админ)</h2>
      <p className="mt-2 text-sm text-[var(--muted)]">
        Порядок имён сетей сверху вниз — от большего приоритета к меньшему (как в noteapp-ai-integration). Пусто =
        автовыбор интеграции. Ключи совпадают с <code className="rounded bg-[var(--bg)] px-1">requestType</code>.
      </p>

      {netsQ.data && netsQ.data.length > 0 ? (
        <details className="mt-3 text-xs text-[var(--muted)]">
          <summary className="cursor-pointer font-medium text-[var(--text)]">Доступные сети (справочно)</summary>
          <ul className="mt-2 list-inside list-disc space-y-1">
            {netsQ.data.map((n) => (
              <li key={n.id}>
                <span className="font-mono">{n.name}</span>
                {n.displayName ? ` — ${n.displayName}` : null}
                {n.networkType ? ` (${n.networkType})` : null}
              </li>
            ))}
          </ul>
        </details>
      ) : netsQ.isLoading ? (
        <p className="mt-2 text-xs text-[var(--muted)]">Загрузка списка сетей…</p>
      ) : (
        <p className="mt-2 text-xs text-amber-700 dark:text-amber-400">
          Не удалось загрузить список (проверьте <code className="rounded bg-[var(--bg)] px-1">AI_INTEGRATION_*</code> на
          backend).
        </p>
      )}

      {err ? (
        <p className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          {err}
        </p>
      ) : null}

      <div className="mt-4 space-y-4">
        {REQUEST_TYPES.map((rt) => (
          <label key={rt} className="block text-sm">
            <span className="font-medium">{rt}</span>
            <span className="mt-1 block text-xs text-[var(--muted)]">Одна сеть на строку (поле name)</span>
            <textarea
              className="mt-1 w-full max-w-xl rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm"
              rows={3}
              value={lines[rt] ?? ''}
              onChange={(e) => setLines((prev) => ({ ...prev, [rt]: e.target.value }))}
              placeholder="например: openai-gpt4"
            />
          </label>
        ))}
      </div>
      <button
        type="button"
        disabled={save.isPending}
        className="mt-4 rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
        onClick={() => save.mutate()}
      >
        {save.isPending ? 'Сохранение…' : 'Сохранить приоритеты'}
      </button>
    </section>
  )
}
