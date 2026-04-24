import { useCallback, useEffect, useId, useState } from 'react'
import { studioInvoke, type StudioAiRequest } from '../api/aiStudio'
import { ApiError } from '../api/client'
import { extractAssistantText } from '../lib/aiOutputParse'

type StudioMode = 'text' | 'split' | 'image' | 'video'

type Iter = { id: string; label: string; mode: StudioMode; prompt: string; output: string; ok: boolean | null }

function newIter(mode: StudioMode, prompt: string, output: string, ok: boolean | null): Iter {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    label: `Шаг ${mode}`,
    mode,
    prompt,
    output,
    ok,
  }
}

const MODES: { id: StudioMode; label: string; requestType: string }[] = [
  { id: 'text', label: 'Текст', requestType: 'chat' },
  { id: 'split', label: 'Части (2.2)', requestType: 'chat' },
  { id: 'image', label: 'Иллюстрация', requestType: 'image_generation' },
  { id: 'video', label: 'Видео', requestType: 'video_generation' },
]

function buildPayload(
  mode: StudioMode,
  original: string,
  userPrompt: string,
  refDataUrl: string | null
): Record<string, unknown> {
  if (mode === 'split') {
    const system =
      'Ты помощник редактора. Разбей входной материал на отдельные логичные фрагменты для публикации как отдельные посты. ' +
      'Верни нумерованный список: каждая строка с номером — один будущий пост. Без вводного текста.'
    return {
      messages: [
        { role: 'system', content: system },
        {
          role: 'user',
          content:
            (original ? `Исходный текст:\n\n${original}\n\n---\n\n` : '') +
            (userPrompt.trim() || 'Разбей на части:'),
        },
      ],
    }
  }
  if (mode === 'image' || mode === 'video') {
    const p: Record<string, unknown> = {
      prompt: userPrompt.trim() || (mode === 'image' ? 'Иллюстрация к статье' : 'Короткое видео по описанию'),
    }
    if (original.trim()) {
      p.context = original.slice(0, 8000)
    }
    if (refDataUrl) {
      p.referenceDataUrl = refDataUrl
    }
    return p
  }
  return {
    messages: [
      {
        role: 'user',
        content:
          (original ? `Оригинал статьи (Markdown):\n\n${original}\n\n---\n\n` : '') +
          (userPrompt.trim() || 'Ответь по контексту выше.'),
      },
    ],
  }
}

export interface AiStudioModalProps {
  open: boolean
  onClose: () => void
  /** Текст статьи (Markdown) как контекст. */
  originalBody: string
  onApplyToArticle: (markdown: string) => void
}

export function AiStudioModal({ open, onClose, originalBody, onApplyToArticle }: AiStudioModalProps) {
  const titleId = useId()
  const [mode, setMode] = useState<StudioMode>('text')
  const [prompt, setPrompt] = useState('')
  const [refFile, setRefFile] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [lastRaw, setLastRaw] = useState<string | null>(null)
  const [iters, setIters] = useState<Iter[]>([])
  const [activeIdx, setActiveIdx] = useState(0)

  const active = iters[activeIdx] ?? null

  useEffect(() => {
    if (!open) {
      return
    }
    setErr(null)
    setLastRaw(null)
  }, [open, mode])

  const run = useCallback(async () => {
    const meta = MODES.find((m) => m.id === mode)
    if (!meta) {
      return
    }
    setPending(true)
    setErr(null)
    setLastRaw(null)
    try {
      const body: StudioAiRequest = {
        requestType: meta.requestType,
        payload: buildPayload(mode, originalBody, prompt, refFile),
        metadata: { 'publisher.studio.mode': mode },
        externalUserId: null,
        networkName: null,
      }
      const res = await studioInvoke(body)
      setLastRaw(res.output)
      const text = extractAssistantText(res.output)
      setIters((prev) => [...prev, newIter(mode, prompt, text, res.ok)])
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Запрос не удался')
    } finally {
      setPending(false)
    }
  }, [mode, originalBody, prompt, refFile])

  useEffect(() => {
    if (iters.length === 0) {
      return
    }
    setActiveIdx(iters.length - 1)
  }, [iters.length])

  if (!open) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-auto bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
    >
      <div className="flex h-[min(90vh,56rem)] w-full min-h-[20rem] min-w-[min(100%,20rem)] max-w-5xl resize flex-col overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--surface)] shadow-xl">
        <div className="flex shrink-0 items-start justify-between gap-3 border-b border-[var(--border)] px-4 py-3">
          <div>
            <h2 id={titleId} className="text-lg font-semibold">
              AI-студия
            </h2>
            <p className="text-xs text-[var(--muted)]">
              Запросы идут в noteapp-ai-integration с маршрутом приоритетов; ответы можно вставить в статью (текст) или
              скопировать.
            </p>
          </div>
          <button
            type="button"
            className="rounded-lg border border-[var(--border)] px-2 py-1 text-sm hover:bg-[var(--bg)]"
            onClick={onClose}
          >
            Закрыть
          </button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-4">
          <div className="mb-3 flex flex-shrink-0 flex-wrap gap-2">
            {MODES.map((m) => (
              <button
                key={m.id}
                type="button"
                className={
                  mode === m.id
                    ? 'rounded-full bg-[var(--accent)] px-3 py-1 text-xs font-medium text-white'
                    : 'rounded-full border border-[var(--border)] px-3 py-1 text-xs hover:bg-[var(--bg)]'
                }
                onClick={() => setMode(m.id)}
              >
                {m.label}
              </button>
            ))}
          </div>

          <div className="grid min-h-[12rem] flex-1 grid-cols-1 gap-3 md:grid-cols-2">
            <div className="flex min-h-0 min-w-0 flex-col space-y-2">
              <p className="shrink-0 text-sm font-medium">Оригинал (контекст)</p>
              <textarea
                readOnly
                className="min-h-[6rem] w-full min-w-0 flex-1 resize-y rounded-lg border border-[var(--border)] bg-[var(--bg)] p-2 font-mono text-xs leading-relaxed text-[var(--text)]"
                value={originalBody.trim() || '— пусто —'}
                rows={8}
                aria-label="Контекст статьи"
              />
            </div>
            <div className="flex min-h-0 min-w-0 flex-col space-y-2">
              <label className="flex min-h-0 min-w-0 flex-1 flex-col text-sm font-medium">
                <span className="shrink-0">Промпт</span>
                <textarea
                  className="mt-1 min-h-[6rem] w-full min-w-0 flex-1 resize-y rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder="Что сделать с текстом, какая иллюстрация, сценарий видео…"
                />
              </label>
              {(mode === 'image' || mode === 'video') && (
                <label className="block text-xs text-[var(--muted)]">
                  Референс (опционально)
                  <input
                    type="file"
                    accept="image/*,video/*"
                    className="mt-1 block w-full text-sm"
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      if (!f) {
                        setRefFile(null)
                        return
                      }
                      const r = new FileReader()
                      r.onload = () => setRefFile(typeof r.result === 'string' ? r.result : null)
                      r.readAsDataURL(f)
                    }}
                  />
                </label>
              )}
              <button
                type="button"
                disabled={pending}
                className="rounded-lg bg-[var(--accent)] px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                onClick={() => void run()}
              >
                {pending ? 'Выполняется…' : 'Выполнить'}
              </button>
              {err ? <p className="text-sm text-red-600">{err}</p> : null}
            </div>
          </div>

          {iters.length > 0 ? (
            <div className="mt-4">
              <p className="text-sm font-medium">Итерации</p>
              <div className="mt-1 flex flex-wrap gap-1">
                {iters.map((it, i) => (
                  <button
                    key={it.id}
                    type="button"
                    className={
                      i === activeIdx
                        ? 'rounded-md bg-[var(--bg)] px-2 py-1 text-xs font-medium ring-1 ring-[var(--accent)]'
                        : 'rounded-md border border-[var(--border)] px-2 py-1 text-xs'
                    }
                    onClick={() => setActiveIdx(i)}
                  >
                    {i + 1}. {it.mode}
                    {it.ok === false ? ' (!)' : ''}
                  </button>
                ))}
              </div>
              {active ? (
                <div className="mt-2 flex min-h-[8rem] flex-col space-y-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-3 text-sm">
                  <p className="shrink-0 text-xs text-[var(--muted)]">Промпт</p>
                  <pre className="min-h-0 min-w-0 max-h-40 flex-1 resize-y overflow-auto whitespace-pre-wrap break-words text-xs">
                    {active.prompt || '—'}
                  </pre>
                  <p className="shrink-0 text-xs text-[var(--muted)]">Ответ</p>
                  <pre className="min-h-0 min-w-0 max-h-64 flex-1 resize-y overflow-auto whitespace-pre-wrap break-words font-sans text-sm">
                    {active.output || '—'}
                  </pre>
                  {active.mode === 'text' || active.mode === 'split' ? (
                    <button
                      type="button"
                      className="rounded border border-[var(--border)] bg-[var(--surface)] px-2 py-1 text-sm"
                      onClick={() => onApplyToArticle(active.output)}
                    >
                      Вставить ответ в статью
                    </button>
                  ) : null}
                </div>
              ) : null}
            </div>
          ) : null}

          {lastRaw && import.meta.env.DEV ? (
            <details className="mt-3 text-xs">
              <summary>Сырой ответ (dev)</summary>
              <pre className="mt-1 max-h-32 overflow-auto rounded bg-[var(--bg)] p-2">{lastRaw}</pre>
            </details>
          ) : null}
        </div>
      </div>
    </div>
  )
}
