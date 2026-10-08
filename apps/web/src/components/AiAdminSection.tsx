import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { fetchAiRouting, fetchAvailableNetworks, saveAiRouting } from '../api/aiAdmin'
import { ApiError } from '../api/client'

const REQUEST_TYPES = ['chat', 'image_generation', 'video_generation', 'transcription', 'speech_synthesis'] as const
const REQUEST_TYPE_LABELS: Record<(typeof REQUEST_TYPES)[number], string> = {
  chat: 'Текст и чат',
  image_generation: 'Генерация изображений',
  video_generation: 'Генерация видео',
  transcription: 'Распознавание речи',
  speech_synthesis: 'Синтез речи',
}

export function AiAdminSection() {
  const qc = useQueryClient()
  const netsQ = useQuery({ queryKey: ['ai', 'networks'], queryFn: fetchAvailableNetworks })
  const routingQ = useQuery({ queryKey: ['ai', 'routing'], queryFn: fetchAiRouting })
  const [routing, setRouting] = useState<Record<string, string[]>>({})

  useEffect(() => {
    if (routingQ.data) {
      setRouting(Object.fromEntries(REQUEST_TYPES.map((rt) => [rt, routingQ.data[rt] ?? []])))
    }
  }, [routingQ.data])

  const updateOrder = (rt: string, names: string[]) => setRouting((prev) => ({ ...prev, [rt]: names }))
  const move = (rt: string, names: string[], index: number, direction: -1 | 1) => {
    const next = [...names]
    const other = index + direction
    const name = next[index]
    next[index] = next[other]
    next[other] = name
    updateOrder(rt, next)
  }
  const save = useMutation({
    mutationFn: () => saveAiRouting(Object.fromEntries(REQUEST_TYPES.map((rt) => [rt, routing[rt] ?? []]))),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'routing'] }),
  })
  const err = save.error instanceof ApiError ? save.error.message : save.error ? 'Ошибка сохранения' : null

  return (
    <section className="rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Нейросети (админ)</h2>
      <p className="mt-2 text-sm text-[var(--muted)]">
        Выберите доступные сети для каждого типа задачи. Порядок сверху вниз задаёт приоритет; пустой список означает
        автовыбор интеграции.
      </p>

      {netsQ.isLoading ? (
        <p className="mt-3 text-sm text-[var(--muted)]">Загрузка списка сетей…</p>
      ) : netsQ.isError ? (
        <p className="mt-3 text-sm text-amber-700 dark:text-amber-400">
          {netsQ.error instanceof ApiError ? netsQ.error.message : 'Не удалось загрузить список сетей.'}
        </p>
      ) : !netsQ.data?.length ? (
        <p className="mt-3 text-sm text-amber-700 dark:text-amber-400">
          Для Publisher пока нет доступных нейросетей.
        </p>
      ) : null}
      {err ? (
        <p className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          {err}
        </p>
      ) : null}

      <div className="mt-4 space-y-5">
        {REQUEST_TYPES.map((rt) => {
          const selected = routing[rt] ?? []
          const available = (netsQ.data ?? []).filter(
            (net) => net.networkType === rt && net.name && !selected.includes(net.name),
          )
          return (
            <div key={rt} className="rounded-lg border border-[var(--border)] p-4">
              <h3 className="text-sm font-semibold">{REQUEST_TYPE_LABELS[rt]}</h3>
              <p className="mt-0.5 text-xs text-[var(--muted)]">{rt}</p>
              {selected.length ? (
                <ol className="mt-3 space-y-2">
                  {selected.map((name, index) => {
                    const net = netsQ.data?.find((item) => item.name === name && item.networkType === rt)
                    return (
                      <li key={`${name}-${index}`} className="flex flex-wrap items-center gap-2 rounded-lg bg-[var(--bg)] px-3 py-2 text-sm">
                        <span className="w-5 text-[var(--muted)]">{index + 1}.</span>
                        <span className="min-w-0 flex-1">
                          <span className="font-medium">{net?.displayName || name}</span>
                          {net?.displayName ? <span className="ml-2 text-xs text-[var(--muted)]">{name}</span> : null}
                          {!net && netsQ.data?.length ? <span className="ml-2 text-xs text-amber-700 dark:text-amber-400">нет в доступном списке</span> : null}
                        </span>
                        <button
                          type="button"
                          aria-label={`Поднять ${name} для ${rt}`}
                          disabled={index === 0}
                          onClick={() => move(rt, selected, index, -1)}
                          className="rounded border border-[var(--border)] px-2 py-1 disabled:opacity-40"
                        >↑</button>
                        <button
                          type="button"
                          aria-label={`Опустить ${name} для ${rt}`}
                          disabled={index === selected.length - 1}
                          onClick={() => move(rt, selected, index, 1)}
                          className="rounded border border-[var(--border)] px-2 py-1 disabled:opacity-40"
                        >↓</button>
                        <button
                          type="button"
                          aria-label={`Убрать ${name} из ${rt}`}
                          onClick={() => updateOrder(rt, selected.filter((_, i) => i !== index))}
                          className="rounded border border-[var(--border)] px-2 py-1 text-[var(--muted)] hover:text-[var(--text)]"
                        >Убрать</button>
                      </li>
                    )
                  })}
                </ol>
              ) : <p className="mt-3 text-xs text-[var(--muted)]">Автовыбор интеграции</p>}
              <label className="mt-3 block max-w-xl text-sm">
                <span className="sr-only">Добавить сеть для {rt}</span>
                <select
                  aria-label={`Добавить сеть для ${rt}`}
                  value=""
                  disabled={routingQ.isLoading || available.length === 0}
                  onChange={(event) => updateOrder(rt, [...selected, event.target.value])}
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 disabled:opacity-60"
                >
                  <option value="">{available.length ? 'Добавить сеть…' : 'Нет доступных сетей'}</option>
                  {available.map((net) => (
                    <option key={net.id} value={net.name}>{net.displayName || net.name} ({net.name})</option>
                  ))}
                </select>
              </label>
            </div>
          )
        })}
      </div>
      <button
        type="button"
        disabled={save.isPending || routingQ.isLoading || !routingQ.data}
        className="mt-4 rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
        onClick={() => save.mutate()}
      >
        {save.isPending ? 'Сохранение…' : 'Сохранить приоритеты'}
      </button>
    </section>
  )
}
