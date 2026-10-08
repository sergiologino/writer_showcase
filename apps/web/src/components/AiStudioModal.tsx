import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useId, useState } from 'react'
import { studioInvoke, type StudioAiRequest } from '../api/aiStudio'
import { extractAssistantText } from '../lib/aiOutputParse'

export type StudioMode = 'text' | 'split' | 'image' | 'video'

const DEFAULT_PROMPTS: Record<StudioMode, string> = {
  text: 'Улучши текст для публикации: исправь ошибки, сделай его яснее и живее, сохрани смысл и авторский голос. Не добавляй непроверенных фактов. Верни только готовый текст в Markdown.',
  split: 'Разбей материал на несколько самостоятельных постов. Для каждого предложи короткий заголовок и готовый текст.',
  image: 'Создай выразительную иллюстрацию к материалу в современном редакционном стиле. Без надписей, водяных знаков и логотипов.',
  video: 'Создай короткое видео по главной идее материала. Без надписей и логотипов.',
}

const MODE_HELP: Record<StudioMode, string> = {
  text: 'Помощник использует уже написанный текст. Измените пожелание, если нужен другой тон или объём.',
  split: 'На выходе получится список фрагментов. Отдельные черновики автоматически не создаются.',
  image: 'Можно создать новую иллюстрацию или приложить изображение и описать, что в нём изменить.',
  video: 'Опишите сцену и действие, которые хотите увидеть в ролике.',
}

const MODE_EXAMPLES: Record<StudioMode, string> = {
  text: 'Например: «Сократи до трёх абзацев и сохрани дружелюбный тон».',
  split: 'Например: «Сделай три поста: проблема, решение и личный опыт».',
  image: 'Например: «Акварельная иллюстрация городского сада весной, без надписей».',
  video: 'Например: «Короткий спокойный пролёт камеры над садом на закате».',
}

type Iter = {
  id: string
  label: string
  mode: StudioMode
  prompt: string
  output: string
  ok: boolean | null
  tokensUsed: number | null
  postTotal: number | null
  imageDataUrl: string | null
}

function newIter(
  mode: StudioMode,
  prompt: string,
  output: string,
  ok: boolean | null,
  tokensUsed: number | null,
  postTotal: number | null,
  imageDataUrl: string | null
): Iter {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    label: `Шаг ${mode}`,
    mode,
    prompt,
    output,
    ok,
    tokensUsed,
    postTotal,
    imageDataUrl,
  }
}

const MODES: { id: StudioMode; label: string; requestType: string }[] = [
  { id: 'text', label: 'Текст', requestType: 'chat' },
  { id: 'split', label: 'Разбить на посты', requestType: 'chat' },
  { id: 'image', label: 'Иллюстрация', requestType: 'image_generation' },
  { id: 'video', label: 'Видео', requestType: 'video_generation' },
]

function buildPayload(
  mode: StudioMode,
  original: string,
  userPrompt: string,
  refDataUrl: string | null,
  referenceAction: string
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
            (userPrompt.trim() || DEFAULT_PROMPTS.split),
        },
      ],
    }
  }
  if (mode === 'image' || mode === 'video') {
    const p: Record<string, unknown> = {
      prompt: (userPrompt.trim() || DEFAULT_PROMPTS[mode])
        + (original.trim() ? `\n\nСодержание материала для контекста: ${original.trim().slice(0, 1800)}` : '')
        + (refDataUrl && referenceAction.trim() ? `\n\nЧто сделать с референсом: ${referenceAction.trim()}` : ''),
    }
    if (original.trim()) {
      p.context = original.slice(0, 8000)
    }
    if (refDataUrl) {
      if (mode === 'image') {
        p.imageBase64 = refDataUrl
        p.imageContentType = /^data:([^;]+);/.exec(refDataUrl)?.[1] ?? 'image/jpeg'
      } else {
        p.referenceDataUrl = refDataUrl
      }
    }
    return p
  }
  return {
    messages: [
      {
        role: 'user',
        content:
          (original ? `Оригинал статьи (Markdown):\n\n${original}\n\n---\n\n` : '') +
          (userPrompt.trim() || DEFAULT_PROMPTS.text),
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
  onApplyImage?: (file: File) => Promise<void>
  canStoreImage?: boolean
  onChooseImageFolder?: () => Promise<void>
  initialMode?: StudioMode
  /** Сохранённый пост — для накопительного учёта токенов; у черновика без id нет. */
  postId?: number | null
  /** Сумма токенов по статье до текущей сессии (из API). */
  articleTokensTotal?: number
}

export function AiStudioModal({
  open,
  onClose,
  originalBody,
  onApplyToArticle,
  onApplyImage,
  canStoreImage = true,
  onChooseImageFolder,
  initialMode = 'text',
  postId = null,
  articleTokensTotal = 0,
}: AiStudioModalProps) {
  const qc = useQueryClient()
  const titleId = useId()
  const [mode, setMode] = useState<StudioMode>(initialMode)
  const [prompts, setPrompts] = useState(DEFAULT_PROMPTS)
  const prompt = prompts[mode]
  const [refFile, setRefFile] = useState<string | null>(null)
  const [refName, setRefName] = useState<string | null>(null)
  const [referenceAction, setReferenceAction] = useState('')
  const [pending, setPending] = useState(false)
  const [applyPending, setApplyPending] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [lastRaw, setLastRaw] = useState<string | null>(null)
  const [iters, setIters] = useState<Iter[]>([])
  const [activeIdx, setActiveIdx] = useState(0)

  const active = iters[activeIdx] ?? null

  useEffect(() => {
    if (open) {
      setMode(initialMode)
      setIters([])
      setRefFile(null)
      setRefName(null)
      setReferenceAction('')
    }
  }, [open, initialMode])

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
        requestType: mode === 'image' && refFile ? 'image_edit' : meta.requestType,
        payload: buildPayload(mode, originalBody, prompt, refFile, referenceAction),
        metadata: { 'publisher.studio.mode': mode },
        externalUserId: null,
        networkName: null,
        postId: postId ?? undefined,
      }
      const res = await studioInvoke(body)
      if (!res.ok) {
        throw new Error(res.errorCode?.includes('No available network')
          ? 'Для работы с референсом администратор должен подключить нейросеть для редактирования изображений.'
          : res.errorCode || 'Нейросеть не смогла выполнить запрос.')
      }
      if (mode === 'image' && !res.imageDataUrl) {
        throw new Error('Нейросеть не вернула изображение. Попробуйте другой запрос или сеть.')
      }
      setLastRaw(res.output)
      const text = res.output != null && res.output.trim() !== '' ? extractAssistantText(res.output) : ''
      if (postId) {
        void qc.invalidateQueries({ queryKey: ['post', String(postId)] })
      }
      setIters((prev) => [
        ...prev,
        newIter(mode, prompt, text, res.ok, res.tokensUsed ?? null, res.postTokensTotal ?? null,
          res.imageDataUrl ?? null),
      ])
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Запрос не удался')
    } finally {
      setPending(false)
    }
  }, [mode, originalBody, prompt, postId, refFile, referenceAction, qc])

  const applyResult = useCallback(async () => {
    if (!active || !active.ok) return
    if (active.mode !== 'image') {
      onApplyToArticle(active.output)
      onClose()
      return
    }
    if (!active.imageDataUrl || !onApplyImage) return
    setApplyPending(true)
    setErr(null)
    try {
      const blob = await (await fetch(active.imageDataUrl)).blob()
      const extension = blob.type === 'image/jpeg' ? 'jpg' : blob.type === 'image/webp' ? 'webp' : 'png'
      await onApplyImage(new File([blob], `publisher-ai-${Date.now()}.${extension}`, { type: blob.type }))
      onClose()
    } catch (error) {
      setErr(error instanceof Error ? error.message : 'Не удалось добавить изображение в пост.')
    } finally {
      setApplyPending(false)
    }
  }, [active, onApplyImage, onApplyToArticle, onClose])

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
            {postId ? (
              <p className="mt-1 text-xs text-[var(--muted)]">
                Токены по статье (накопительно):{' '}
                <span className="font-medium text-[var(--text)] tabular-nums">{articleTokensTotal}</span>
              </p>
            ) : (
              <p className="mt-1 text-xs text-amber-800 dark:text-amber-200/90">
                Сохраните материал — затем токены будут считаться по статье.
              </p>
            )}
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
              <p className="shrink-0 text-sm font-medium">1. Текст материала</p>
              <p className="text-xs text-[var(--muted)]">Текущий текст уже передан помощнику как контекст.</p>
              <textarea
                readOnly
                className="min-h-[6rem] w-full min-w-0 flex-1 resize-y rounded-lg border border-[var(--border)] bg-[var(--bg)] p-2 font-mono text-xs leading-relaxed text-[var(--text)]"
                value={originalBody}
                rows={8}
                aria-label="Контекст статьи"
                placeholder="Пока нет текста. Можно сначала написать его в редакторе или описать задачу справа."
              />
            </div>
            <div className="flex min-h-0 min-w-0 flex-col space-y-2">
              <label className="flex min-h-0 min-w-0 flex-1 flex-col text-sm font-medium">
                <span className="shrink-0">2. Что сделать</span>
                <textarea
                  className="mt-1 min-h-[6rem] w-full min-w-0 flex-1 resize-y rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                  value={prompt}
                  onChange={(e) => setPrompts((prev) => ({ ...prev, [mode]: e.target.value }))}
                  placeholder={DEFAULT_PROMPTS[mode]}
                />
              </label>
              <p className="text-xs text-[var(--muted)]">{MODE_HELP[mode]}</p>
              <p className="text-xs text-[var(--muted)]">{MODE_EXAMPLES[mode]}</p>
              {mode === 'image' && !canStoreImage ? (
                <div className="rounded-lg border border-amber-500/40 bg-amber-50 p-3 text-xs text-amber-950 dark:bg-amber-950/30 dark:text-amber-100">
                  <p>Сначала выберите папку материала: готовое изображение сохранится в ней до отправки на сервер.</p>
                  {onChooseImageFolder ? (
                    <button type="button" className="mt-2 rounded border border-current px-2 py-1"
                      onClick={() => void onChooseImageFolder().catch((error: unknown) =>
                        setErr(error instanceof Error ? error.message : 'Не удалось выбрать папку.'))}>
                      Выбрать папку
                    </button>
                  ) : null}
                </div>
              ) : null}
              {(mode === 'image' || mode === 'video') && (
                <label className="block text-xs text-[var(--muted)]">
                  Изображение-референс (необязательно)
                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    className="mt-1 block w-full text-sm"
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      if (!f) {
                        setRefFile(null)
                        setRefName(null)
                        return
                      }
                      const r = new FileReader()
                      r.onload = () => {
                        setRefFile(typeof r.result === 'string' ? r.result : null)
                        setRefName(f.name)
                      }
                      r.readAsDataURL(f)
                    }}
                  />
                </label>
              )}
              {mode === 'image' && refFile ? (
                <div className="space-y-2">
                  <img src={refFile} alt="Выбранный референс" className="max-h-28 rounded-lg border border-[var(--border)] object-contain" />
                  <p className="text-xs text-[var(--muted)]">{refName} · требуется нейросеть для редактирования изображений</p>
                  <label className="block text-sm font-medium">
                    Что сделать с референсом
                    <input
                      className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                      value={referenceAction}
                      onChange={(e) => setReferenceAction(e.target.value)}
                      placeholder="Например: сохрани композицию, но замени фон и цвета под тему статьи"
                    />
                  </label>
                  <button type="button" className="text-xs text-[var(--accent)] hover:underline" onClick={() => { setRefFile(null); setRefName(null) }}>
                    Убрать референс
                  </button>
                </div>
              ) : null}
              <button
                type="button"
                disabled={pending || (mode === 'image' && !canStoreImage)}
                className="rounded-lg bg-[var(--accent)] px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                onClick={() => void run()}
              >
                {pending ? 'Создаём…' : mode === 'image' ? 'Создать изображение' : mode === 'video' ? 'Создать видео' : 'Получить вариант текста'}
              </button>
              {err ? <p className="text-sm text-red-600">{err}</p> : null}
            </div>
          </div>

          {iters.length > 0 ? (
            <div className="mt-4">
              <p className="text-sm font-medium">3. Результат</p>
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
                    {i + 1}. {MODES.find((m) => m.id === it.mode)?.label ?? it.mode}
                    {it.ok === false ? ' (!)' : ''}
                  </button>
                ))}
              </div>
              {active ? (
                <div className="mt-2 flex min-h-[8rem] flex-col space-y-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-3 text-sm">
                  {active.mode === 'image' && active.imageDataUrl ? (
                    <img src={active.imageDataUrl} alt="Сгенерированная иллюстрация" className="max-h-72 self-start rounded-lg object-contain" />
                  ) : (
                    <pre className="min-h-0 min-w-0 max-h-64 flex-1 resize-y overflow-auto whitespace-pre-wrap break-words font-sans text-sm">
                      {active.output || '—'}
                    </pre>
                  )}
                  {active.tokensUsed != null || active.postTotal != null ? (
                    <p className="text-xs text-[var(--muted)]">
                      {active.tokensUsed != null ? (
                        <>
                          За запрос: <span className="tabular-nums text-[var(--text)]">{active.tokensUsed}</span> ток.
                        </>
                      ) : null}
                      {active.tokensUsed != null && active.postTotal != null ? ' · ' : null}
                      {active.postTotal != null ? (
                        <>
                          Всего по статье:{' '}
                          <span className="font-medium tabular-nums text-[var(--text)]">{active.postTotal}</span>
                        </>
                      ) : null}
                    </p>
                  ) : null}
                  {(active.mode === 'text' || active.mode === 'split' || active.mode === 'image') && active.ok ? (
                    <button
                      type="button"
                      disabled={applyPending || (active.mode === 'image' && !onApplyImage)}
                      className="self-start rounded-lg bg-[var(--accent)] px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                      onClick={() => void applyResult()}
                    >
                      {applyPending ? 'Добавляем…' : active.mode === 'image' ? 'Готово — добавить в медиа поста' : 'Готово — заменить текст поста'}
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
