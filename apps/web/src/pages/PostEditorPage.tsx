import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { fetchWorkspaceChannels } from '../api/channels'
import { fetchMediaPage } from '../api/media'
import { createPost, fetchPost, updatePost, type PostPayload } from '../api/posts'
import type { ChannelType, PostStatus, PostVisibility } from '../api/types'

function channelLabel(t: ChannelType): string {
  const m: Record<ChannelType, string> = {
    TELEGRAM: 'Telegram',
    VK: 'ВКонтакте',
    ODNOKLASSNIKI: 'Одноклассники',
  }
  return m[t] ?? t
}

const schema = z.object({
  title: z.string().min(1, 'Заголовок обязателен').max(500),
  slug: z.string().max(500).optional(),
  excerpt: z.string().max(20_000).optional(),
  bodySource: z.string().max(200_000).optional(),
  bodyHtml: z.string().max(500_000).optional(),
  visibility: z.enum(['PUBLIC', 'PRIVATE', 'UNLISTED']),
  status: z.enum(['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']),
})

type FormValues = z.infer<typeof schema>

const defaults: FormValues = {
  title: '',
  slug: '',
  excerpt: '',
  bodySource: '',
  bodyHtml: '',
  visibility: 'PUBLIC',
  status: 'DRAFT',
}

export function PostEditorPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const isNew = !id || id === 'new'

  const existing = useQuery({
    queryKey: ['post', id],
    queryFn: () => fetchPost(Number(id)),
    enabled: !isNew,
  })

  const mediaPicker = useQuery({
    queryKey: ['media', 'picker'],
    queryFn: () => fetchMediaPage(0, 40),
  })

  const channelsQ = useQuery({
    queryKey: ['channels'],
    queryFn: fetchWorkspaceChannels,
  })

  const [mediaIds, setMediaIds] = useState<number[]>([])
  const [socialPublish, setSocialPublish] = useState(true)
  const [channelOn, setChannelOn] = useState<Record<string, boolean>>({})

  const enabledWorkspaceChannels = useMemo(
    () => (channelsQ.data ?? []).filter((c) => c.enabled),
    [channelsQ.data],
  )

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  })

  useEffect(() => {
    if (existing.data) {
      form.reset({
        title: existing.data.title,
        slug: existing.data.slug,
        excerpt: existing.data.excerpt ?? '',
        bodySource: existing.data.bodySource ?? '',
        bodyHtml: existing.data.bodyHtml ?? '',
        visibility: existing.data.visibility,
        status: existing.data.status,
      })
      setMediaIds(existing.data.media?.map((m) => m.mediaAssetId) ?? [])
    }
  }, [existing.data, form])

  useEffect(() => {
    if (!channelsQ.data) {
      return
    }
    const enabled = channelsQ.data.filter((c) => c.enabled)
    if (!isNew && existing.data) {
      setSocialPublish(existing.data.socialPublishEnabled ?? true)
      const pub = existing.data.publishChannelTypes ?? []
      const o: Record<string, boolean> = {}
      for (const c of enabled) {
        o[c.channelType] = pub.length === 0 ? true : pub.includes(c.channelType)
      }
      setChannelOn(o)
      return
    }
    if (isNew) {
      setSocialPublish(true)
      const o: Record<string, boolean> = {}
      for (const c of enabled) {
        o[c.channelType] = true
      }
      setChannelOn(o)
    }
  }, [channelsQ.data, existing.data, isNew])

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const inPickerOrder = (mediaPicker.data?.content ?? [])
        .map((m) => m.id)
        .filter((id) => mediaIds.includes(id))
      const orderedMedia =
        inPickerOrder.length === mediaIds.length ? inPickerOrder : [...mediaIds]

      let socialPublishEnabled = socialPublish
      let publishChannels: ChannelType[] | null = null
      const enList = enabledWorkspaceChannels
      if (!socialPublish) {
        socialPublishEnabled = false
        publishChannels = null
      } else {
        socialPublishEnabled = true
        const selected = enList.filter((c) => channelOn[c.channelType])
        if (enList.length > 0 && selected.length === 0) {
          socialPublishEnabled = false
          publishChannels = null
        } else if (enList.length === 0 || selected.length === enList.length) {
          publishChannels = []
        } else {
          publishChannels = selected.map((c) => c.channelType)
        }
      }

      const payload: PostPayload = {
        title: values.title,
        slug: values.slug?.trim() ?? '',
        excerpt: values.excerpt ?? '',
        bodySource: values.bodySource ?? '',
        bodyHtml: values.bodyHtml ?? '',
        visibility: values.visibility as PostVisibility,
        status: values.status as PostStatus,
        categoryId: null,
        tagIds: [],
        aiGenerated: false,
        mediaAssetIds: orderedMedia,
        socialPublishEnabled,
        publishChannels,
      }
      if (isNew) {
        return createPost(payload)
      }
      return updatePost(Number(id), payload)
    },
    onSuccess: async (post) => {
      await qc.invalidateQueries({ queryKey: ['posts'] })
      await qc.refetchQueries({ queryKey: ['posts'] })
      await qc.invalidateQueries({ queryKey: ['post', String(post.id)] })
      navigate(`/app/posts/${post.id}`)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold">{isNew ? 'Новый материал' : 'Редактирование'}</h1>
        <Link
          to="/app/feed"
          className="text-sm text-[var(--muted)] hover:text-[var(--text)] hover:underline"
        >
          ← к ленте
        </Link>
      </div>

      {!isNew && existing.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка…</p>
      ) : (
        <form
          className="space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm"
          onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
        >
          {mutation.isError ? (
            <p className="text-sm text-red-600">Не удалось сохранить. Проверьте данные.</p>
          ) : null}
          <label className="block text-sm font-medium">
            Заголовок
            <input
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('title')}
            />
          </label>
          {form.formState.errors.title ? (
            <p className="text-xs text-red-600">{form.formState.errors.title.message}</p>
          ) : null}

          <label className="block text-sm font-medium">
            Slug (необязательно, сгенерируется из заголовка)
            <input
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('slug')}
            />
          </label>

          <label className="block text-sm font-medium">
            Краткое описание
            <textarea
              rows={2}
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('excerpt')}
            />
          </label>

          <label className="block text-sm font-medium">
            Текст (источник)
            <textarea
              rows={12}
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('bodySource')}
            />
          </label>

          <label className="block text-sm font-medium">
            HTML (превью для публикации, можно скопировать из редактора позже)
            <textarea
              rows={6}
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('bodyHtml')}
            />
          </label>

          <div className="space-y-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-4">
            <p className="text-sm font-medium">Медиа к материалy</p>
            <p className="text-xs text-[var(--muted)]">
              Отметьте файлы из библиотеки ({' '}
              <Link className="text-[var(--accent)] hover:underline" to="/app/media">
                загрузить
              </Link>
              ). Порядок — как в списке ниже (сверху вниз).
            </p>
            {mediaPicker.isLoading ? (
              <p className="text-xs text-[var(--muted)]">Список медиа…</p>
            ) : (
              <ul className="max-h-48 space-y-2 overflow-y-auto text-sm">
                {(mediaPicker.data?.content ?? []).map((m) => {
                  const checked = mediaIds.includes(m.id)
                  return (
                    <li key={m.id}>
                      <label className="flex cursor-pointer items-center gap-2">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => {
                            setMediaIds((prev) => {
                              const ix = prev.indexOf(m.id)
                              if (ix >= 0) {
                                return prev.filter((x) => x !== m.id)
                              }
                              return [...prev, m.id]
                            })
                          }}
                        />
                        <span className="font-mono text-xs">#{m.id}</span>
                        <span className="text-[var(--muted)]">{m.mimeType ?? m.type}</span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          <div className="space-y-3 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-4">
            <label className="flex cursor-pointer items-center gap-2 text-sm font-medium">
              <input
                type="checkbox"
                checked={socialPublish}
                onChange={(e) => setSocialPublish(e.target.checked)}
              />
              Публиковать в подключённые соцсети при выпуске (ВК, ОК, Telegram…)
            </label>
            {channelsQ.isLoading ? (
              <p className="text-xs text-[var(--muted)]">Каналы workspace…</p>
            ) : enabledWorkspaceChannels.length === 0 ? (
              <p className="text-xs text-[var(--muted)]">
                В workspace пока нет включённых каналов. Настройте их в разделе{' '}
                <Link className="text-[var(--accent)] hover:underline" to="/app/channels">
                  «Каналы публикации»
                </Link>{' '}
                (пошаговые инструкции и поля без JSON).
              </p>
            ) : (
              <ul className="space-y-2 text-sm">
                {enabledWorkspaceChannels.map((c) => (
                  <li key={c.channelType}>
                    <label className="flex cursor-pointer items-center gap-2">
                      <input
                        type="checkbox"
                        disabled={!socialPublish}
                        checked={!!channelOn[c.channelType]}
                        onChange={(e) => {
                          setChannelOn((prev) => ({
                            ...prev,
                            [c.channelType]: e.target.checked,
                          }))
                        }}
                      />
                      {channelLabel(c.channelType)}
                      {c.label ? (
                        <span className="text-xs text-[var(--muted)]">({c.label})</span>
                      ) : null}
                    </label>
                  </li>
                ))}
              </ul>
            )}
            <p className="text-xs text-[var(--muted)]">
              Снятые каналы не получают материал даже при статусе «Опубликован» и публичной видимости. Пустой выбор при
              включённом пункте выше трактуется как «все каналы». Кросс-пост выполняется один раз при первом переводе в
              опубликованный публичный пост.
            </p>
            {!isNew && existing.data && (existing.data.outbound?.length ?? 0) > 0 ? (
              <div className="mt-2 border-t border-[var(--border)] pt-3">
                <p className="text-sm font-medium">Статус публикации на площадках</p>
                <ul className="mt-2 space-y-2 text-xs">
                  {(existing.data.outbound ?? []).map((o) => (
                    <li key={o.channelType} className="rounded-md border border-[var(--border)] bg-[var(--surface)] p-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium">{channelLabel(o.channelType)}</span>
                        <span
                          className={
                            o.deliveryStatus === 'SENT'
                              ? 'text-emerald-600 dark:text-emerald-400'
                              : 'text-amber-600 dark:text-amber-400'
                          }
                        >
                          {o.deliveryStatus}
                        </span>
                        {o.externalUrl ? (
                          <a
                            className="text-[var(--accent)] hover:underline"
                            href={o.externalUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Открыть
                          </a>
                        ) : null}
                      </div>
                      {o.lastError ? (
                        <p className="mt-1 text-red-600 dark:text-red-400">{o.lastError}</p>
                      ) : null}
                      {o.deliveryStatus === 'SENT' ? (
                        <p className="mt-1 text-[var(--muted)]">
                          Лайки {o.likes} · Репосты {o.reposts} · Просмотры {o.views}
                          {o.comments > 0 ? ` · Комментарии ${o.comments}` : ''}
                          {o.metricsFetchedAt ? (
                            <span className="block opacity-80">
                              метрики: {new Date(o.metricsFetchedAt).toLocaleString()}
                            </span>
                          ) : (
                            <span className="block opacity-80">метрики обновляются автоматически (ВК)</span>
                          )}
                        </p>
                      ) : null}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm font-medium">
              Видимость
              <select
                className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                {...form.register('visibility')}
              >
                <option value="PUBLIC">Публичная</option>
                <option value="PRIVATE">Приватная</option>
                <option value="UNLISTED">По ссылке</option>
              </select>
            </label>
            <label className="block text-sm font-medium">
              Статус
              <select
                className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                {...form.register('status')}
              >
                <option value="DRAFT">Черновик</option>
                <option value="REVIEW">Review</option>
                <option value="PUBLISHED">Опубликован</option>
                <option value="ARCHIVED">Архив</option>
              </select>
              <p className="mt-1 text-xs text-[var(--muted)]">
                Публичный блог показывает только сочетание «Опубликован» + видимость «Публичная».
              </p>
            </label>
          </div>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={mutation.isPending}
              className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-60"
            >
              {mutation.isPending ? 'Сохранение…' : 'Сохранить'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
