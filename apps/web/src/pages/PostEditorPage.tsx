import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { fetchWorkspaceChannels } from '../api/channels'
import { createCategory, fetchCategories } from '../api/categories'
import { uploadMedia } from '../api/media'
import { createPost, fetchPost, updatePost, type PostPayload } from '../api/posts'
import type { ChannelType, PostStatus, PostVisibility } from '../api/types'
import type { MeResponse } from '../api/types'
import { AiStudioModal, type StudioMode } from '../components/AiStudioModal'
import { AuthenticatedMediaThumb } from '../components/AuthenticatedMediaThumb'
import { LocalMediaThumb } from '../components/LocalMediaThumb'
import { Seo } from '../components/Seo'
import { channelFullName, deliveryStatusLabel, deliveryStatusTone } from '../lib/channelPublish'
import { articleSourceToHtml } from '../lib/articleHtml'
import {
  bindServerId, choosePostFolder, folderPickerAvailable, getPostFolder,
  loadLocalPost, localPostKey, publicationFromResponse, readLocalMedia,
  saveLocalPost, saveSelectedMedia, type LocalMedia, type LocalPost, type LocalPublication,
} from '../lib/localPosts'

function instantToDatetimeLocalValue(iso: string | null | undefined): string {
  if (!iso) {
    return ''
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return ''
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function datetimeLocalToIso(local: string): string | null {
  if (!local.trim()) {
    return null
  }
  const t = new Date(local).getTime()
  if (Number.isNaN(t)) {
    return null
  }
  return new Date(t).toISOString()
}

const schema = z.object({
  title: z.string().min(1, 'Заголовок обязателен').max(500),
  slug: z.string().max(500).optional(),
  excerpt: z.string().max(20_000).optional(),
  bodySource: z.string().max(200_000).optional(),
  bodyHtml: z.string().max(500_000).optional(),
  visibility: z.enum(['PUBLIC', 'PRIVATE', 'UNLISTED']),
  status: z.enum(['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']),
  categoryId: z.number().nullable().optional(),
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
  categoryId: null,
}

export function PostEditorPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const localRouteId = id?.startsWith('local-') ? id.slice('local-'.length) : null
  const isNew = !id || id === 'new' || !!localRouteId

  const existing = useQuery({
    queryKey: ['post', id],
    queryFn: () => fetchPost(Number(id)),
    enabled: !isNew,
  })

  const categoriesQ = useQuery({
    queryKey: ['categories'],
    queryFn: fetchCategories,
  })

  const channelsQ = useQuery({
    queryKey: ['channels'],
    queryFn: fetchWorkspaceChannels,
  })

  type MediaRow = LocalMedia & { id: number | null }
  const [mediaItems, setMediaItems] = useState<MediaRow[]>([])
  const [uploadBusy, setUploadBusy] = useState(false)
  const [uploadErr, setUploadErr] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [socialPublish, setSocialPublish] = useState(true)
  const [channelOn, setChannelOn] = useState<Record<string, boolean>>({})
  const [htmlAdvancedOpen, setHtmlAdvancedOpen] = useState(false)
  const [newCategoryName, setNewCategoryName] = useState('')
  const [scheduledLocal, setScheduledLocal] = useState('')
  const [studioOpen, setStudioOpen] = useState(false)
  const [studioMode, setStudioMode] = useState<StudioMode>('text')
  const [aiGeneratedOverride, setAiGeneratedOverride] = useState<boolean | null>(null)
  const [folderName, setFolderName] = useState<string | null>(null)
  const [localMessage, setLocalMessage] = useState<string | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)
  const localIdRef = useRef<string>(localRouteId ?? crypto.randomUUID())
  const localLoadedRef = useRef(false)
  const localInitializedRef = useRef(false)
  const localPublishChannelsRef = useRef<ChannelType[] | null>(null)
  const localPublicationRef = useRef<LocalPublication | undefined>(undefined)
  const localSaveQueue = useRef<Promise<void>>(Promise.resolve())
  const workspaceId = localStorage.getItem('workspaceId') ?? 'default'
  const routeKey = `${workspaceId}:${id ?? 'new'}`
  const previousRouteRef = useRef(routeKey)
  if (previousRouteRef.current !== routeKey) {
    previousRouteRef.current = routeKey
    localInitializedRef.current = false
    localLoadedRef.current = false
    localPublishChannelsRef.current = null
    localPublicationRef.current = undefined
    if (id === 'new' || localRouteId) localIdRef.current = localRouteId ?? crypto.randomUUID()
  }
  const localKey = localPostKey(workspaceId, isNew ? localIdRef.current : Number(id))

  const enabledWorkspaceChannels = useMemo(
    () => (channelsQ.data ?? []).filter((c) => c.enabled),
    [channelsQ.data],
  )

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  })

  const { watch, setValue } = form
  const bodySourceWatch = watch('bodySource')
  const watchedValues = useWatch({ control: form.control })

  useEffect(() => {
    if (existing.data && !localLoadedRef.current) {
      const ed = existing.data
      form.reset({
        title: ed.title,
        slug: ed.slug,
        excerpt: ed.excerpt ?? '',
        bodySource: ed.bodySource ?? '',
        bodyHtml: ed.bodyHtml ?? '',
        visibility: ed.visibility,
        status: ed.status,
        categoryId: ed.categoryId ?? null,
      })
      const sorted = [...(ed.media ?? [])].sort((a, b) => a.sortOrder - b.sortOrder)
      setMediaItems(sorted.map((m) => ({
        id: m.mediaAssetId, serverId: m.mediaAssetId, mimeType: m.mimeType,
        fileName: '', originalName: null,
      })))
      const me = qc.getQueryData<MeResponse>(['me'])
      const slug = me?.workspaces.find((workspace) => String(workspace.id) === workspaceId)?.slug
        ?? me?.workspaces[0]?.slug
      localPublicationRef.current = publicationFromResponse(ed, slug)
      const gen = articleSourceToHtml(ed.bodySource ?? '').trim()
      const stor = (ed.bodyHtml ?? '').trim()
      setHtmlAdvancedOpen(stor.length > 0 && stor !== gen)
      setScheduledLocal(instantToDatetimeLocalValue(ed.scheduledPublishAt))
      setAiGeneratedOverride(null)
    }
  }, [existing.data, form, qc, workspaceId])

  useEffect(() => {
    if (localInitializedRef.current || (!isNew && existing.isLoading)) return
    let cancelled = false
    void (async () => {
      try {
        const [local, folder] = await Promise.all([loadLocalPost(localKey), getPostFolder()])
        if (cancelled) return
        setFolderName(folder?.name ?? null)
        if (local && (isNew || !existing.data || local.savedAt > existing.data.updatedAt)) {
          localIdRef.current = local.localId
          localLoadedRef.current = true
          const p = local.payload
          form.reset({
            title: p.title, slug: p.slug, excerpt: p.excerpt, bodySource: p.bodySource,
            bodyHtml: p.bodyHtml, visibility: p.visibility, status: p.status,
            categoryId: p.categoryId,
          })
          setScheduledLocal(instantToDatetimeLocalValue(p.scheduledPublishAt))
          setSocialPublish(p.socialPublishEnabled ?? false)
          setMediaItems((local.media ?? p.mediaAssetIds.map((mediaId) => ({
            serverId: mediaId, mimeType: null, fileName: '', originalName: null,
          }))).map((media) => ({ ...media, id: media.serverId })))
          if (existing.data) {
            const me = qc.getQueryData<MeResponse>(['me'])
            const slug = me?.workspaces.find((workspace) => String(workspace.id) === workspaceId)?.slug
              ?? me?.workspaces[0]?.slug
            localPublicationRef.current = publicationFromResponse(existing.data, slug)
          } else {
            localPublicationRef.current = local.publication
          }
          localPublishChannelsRef.current = p.publishChannels ?? null
          if (channelsQ.data) {
            setChannelOn(Object.fromEntries(channelsQ.data.filter((c) => c.enabled).map((c) => [
              c.channelType, !p.publishChannels?.length || p.publishChannels.includes(c.channelType),
            ])))
          }
          setHtmlAdvancedOpen(!!p.bodyHtml.trim() && p.bodyHtml.trim() !== articleSourceToHtml(p.bodySource).trim())
          setLocalMessage('Восстановлена локальная версия материала.')
        } else if (local) {
          localIdRef.current = local.localId
        }
        localInitializedRef.current = true
      } catch {
        if (!cancelled) setLocalError('Локальная база недоступна. Проверьте настройки браузера.')
      }
    })()
    return () => { cancelled = true }
  }, [channelsQ.data, existing.data, existing.isLoading, form, isNew, localKey, qc, workspaceId])

  useEffect(() => {
    if (!channelsQ.data) {
      return
    }
    const enabled = channelsQ.data.filter((c) => c.enabled)
    if (localLoadedRef.current) {
      const selected = localPublishChannelsRef.current
      setChannelOn(Object.fromEntries(enabled.map((c) => [
        c.channelType, !selected?.length || selected.includes(c.channelType),
      ])))
      return
    }
    if (!isNew && existing.data && !localLoadedRef.current) {
      setSocialPublish(existing.data.socialPublishEnabled ?? true)
      const pub = existing.data.publishChannelTypes ?? []
      const o: Record<string, boolean> = {}
      for (const c of enabled) {
        o[c.channelType] = pub.length === 0 ? true : pub.includes(c.channelType)
      }
      setChannelOn(o)
      return
    }
    if (isNew && !localLoadedRef.current) {
      setSocialPublish(true)
      const o: Record<string, boolean> = {}
      for (const c of enabled) {
        o[c.channelType] = true
      }
      setChannelOn(o)
    }
  }, [channelsQ.data, existing.data, isNew])

  const createCategoryMut = useMutation({
    mutationFn: (name: string) => createCategory({ name }),
    onSuccess: (cat) => {
      void qc.invalidateQueries({ queryKey: ['categories'] })
      setValue('categoryId', cat.id, { shouldDirty: true })
      setNewCategoryName('')
    },
  })

  const buildPayload = useCallback((values: FormValues): PostPayload => {
      const orderedMedia = mediaItems.map((m) => m.id).filter((mediaId): mediaId is number => mediaId !== null)

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

      const manualHtml = (values.bodyHtml ?? '').trim()
      const autoHtml = articleSourceToHtml(values.bodySource ?? '')
      const bodyHtml =
        htmlAdvancedOpen && manualHtml.length > 0 ? (values.bodyHtml ?? '') : autoHtml

      const scheduledIso = datetimeLocalToIso(scheduledLocal)
      return {
        title: values.title,
        slug: values.slug?.trim() ?? '',
        excerpt: values.excerpt ?? '',
        bodySource: values.bodySource ?? '',
        bodyHtml,
        visibility: values.visibility as PostVisibility,
        status: values.status as PostStatus,
        categoryId: values.categoryId ?? null,
        tagIds: [],
        aiGenerated:
          aiGeneratedOverride !== null
            ? aiGeneratedOverride
            : isNew
              ? false
              : (existing.data?.aiGenerated ?? false),
        mediaAssetIds: orderedMedia,
        socialPublishEnabled,
        publishChannels,
        scheduledPublishAt: scheduledIso,
      }
  }, [mediaItems, socialPublish, enabledWorkspaceChannels, channelOn, htmlAdvancedOpen,
    scheduledLocal, aiGeneratedOverride, isNew, existing.data])

  const persistLocal = useCallback((payload: PostPayload, rows = mediaItems): Promise<LocalPost> => {
    const post: LocalPost = {
      key: localKey, localId: localIdRef.current, serverId: isNew ? null : Number(id),
      savedAt: new Date().toISOString(), payload,
      media: rows.map(({ fileName, mimeType, serverId, originalName }) => ({
        fileName, mimeType, serverId, originalName,
      })),
      publication: localPublicationRef.current,
    }
    const next = localSaveQueue.current.then(() => saveLocalPost(post))
    localSaveQueue.current = next.catch(() => {})
    return next.then(() => post)
  }, [id, isNew, localKey, mediaItems])

  useEffect(() => {
    if (!localInitializedRef.current) return
    const values = watchedValues as FormValues
    if (!values.title?.trim() && !values.bodySource?.trim() && mediaItems.length === 0) return
    const timer = window.setTimeout(() => {
      void persistLocal(buildPayload(values)).then(() => {
        setLocalError(null)
        setLocalMessage('Локальная копия сохранена.')
        if (id === 'new') navigate(`/app/posts/local-${localIdRef.current}`, { replace: true })
      }).catch((error: unknown) => {
        setLocalError(error instanceof Error ? error.message : 'Не удалось сохранить локальную копию.')
      })
    }, 900)
    return () => window.clearTimeout(timer)
  }, [watchedValues, buildPayload, persistLocal, mediaItems.length, id, navigate])

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      let payload = buildPayload(values)
      let local = await persistLocal(payload)
      const uploadedRows = await Promise.all(mediaItems.map(async (row) => {
        if (row.id !== null) return row
        const stored = await readLocalMedia(localIdRef.current, row.fileName)
        const file = new File([stored], row.originalName || stored.name, {
          type: row.mimeType || stored.type,
        })
        const asset = await uploadMedia(file)
        return { ...row, id: asset.id, serverId: asset.id, mimeType: asset.mimeType }
      }))
      if (uploadedRows.some((row, index) => row.id !== mediaItems[index].id)) {
        setMediaItems(uploadedRows)
        payload = { ...payload, mediaAssetIds: uploadedRows.map((row) => row.id).filter((mediaId): mediaId is number => mediaId !== null) }
        local = await persistLocal(payload, uploadedRows)
      }
      const post = isNew ? await createPost(payload) : await updatePost(Number(id), payload)
      const me = qc.getQueryData<MeResponse>(['me'])
      const slug = me?.workspaces.find((workspace) => String(workspace.id) === workspaceId)?.slug
        ?? me?.workspaces[0]?.slug
      localPublicationRef.current = publicationFromResponse(post, slug)
      if (isNew) {
        try {
          await bindServerId(local, post.id)
          await saveLocalPost({ ...local, key: localPostKey(workspaceId, post.id), serverId: post.id,
            publication: localPublicationRef.current })
        } catch {
          setLocalError('Пост отправлен на сервер, но связь с локальной копией не обновилась.')
        }
      } else {
        try {
          await saveLocalPost({ ...local, publication: localPublicationRef.current })
        } catch {
          setLocalError('Пост отправлен на сервер, но параметры локальной публикации не обновились.')
        }
      }
      return post
    },
    onSuccess: async (post) => {
      setAiGeneratedOverride(null)
      await qc.invalidateQueries({ queryKey: ['posts'] })
      await qc.refetchQueries({ queryKey: ['posts'] })
      await qc.invalidateQueries({ queryKey: ['post', String(post.id)] })
      navigate(`/app/posts/${post.id}`)
    },
  })

  const generatedPreview = useMemo(() => articleSourceToHtml(bodySourceWatch ?? ''), [bodySourceWatch])
  const openStudio = (mode: StudioMode) => {
    setStudioMode(mode)
    setStudioOpen(true)
  }

  return (
    <div className="space-y-6">
      <Seo
        title={isNew ? 'Новый материал' : 'Редактирование материала'}
        description="Редактор Altacod Publisher для подготовки статьи, медиа, SEO-описания, расписания и публикации в каналы."
        keywords="редактор статьи, редактор блога, AI студия, публикация в Telegram, расписание публикации"
        canonicalPath={isNew ? '/app/posts/new' : `/app/posts/${id}`}
        noIndex
      />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">{isNew ? 'Новый материал' : 'Редактирование'}</h1>
          {!isNew && existing.data ? (
            <p className="mt-0.5 text-xs text-[var(--muted)]">
              Токены ИИ (по статье):{' '}
              <span className="tabular-nums text-[var(--text)]">{existing.data.aiTokensTotal ?? 0}</span>
            </p>
          ) : null}
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            className="rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-1.5 text-sm hover:border-[var(--accent)]"
            onClick={() => openStudio('text')}
          >
            AI-студия
          </button>
          <Link
            to="/app/feed"
            className="text-sm text-[var(--muted)] hover:text-[var(--text)] hover:underline"
          >
            ← к ленте
          </Link>
        </div>
      </div>

      <AiStudioModal
        open={studioOpen}
        onClose={() => setStudioOpen(false)}
        initialMode={studioMode}
        canStoreImage={!!folderName}
        onChooseImageFolder={async () => {
          const name = await choosePostFolder()
          setFolderName(name)
          setLocalError(null)
        }}
        originalBody={bodySourceWatch ?? ''}
        postId={isNew ? null : Number(id)}
        articleTokensTotal={!isNew ? (existing.data?.aiTokensTotal ?? 0) : 0}
        onApplyToArticle={(md) => {
          setValue('bodySource', md, { shouldDirty: true })
          setAiGeneratedOverride(true)
          setStudioOpen(false)
        }}
        onApplyImage={async (file) => {
          setUploadBusy(true)
          setUploadErr(null)
          try {
            const localMedia = await saveSelectedMedia(localIdRef.current, file)
            const row: MediaRow = { ...localMedia, id: null }
            setMediaItems((prev) => [...prev, row])
            setAiGeneratedOverride(true)
            try {
              const asset = await uploadMedia(file)
              setMediaItems((prev) => prev.map((item) => item.fileName === row.fileName
                ? { ...row, id: asset.id, serverId: asset.id, mimeType: asset.mimeType } : item))
            } catch {
              setUploadErr('Изображение сохранено локально. Загрузка на сервер повторится при сохранении материала.')
            }
          } finally {
            setUploadBusy(false)
          }
        }}
      />

      {!isNew && existing.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка…</p>
      ) : (
        <form
          className="space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm"
          onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
        >
          {mutation.isError ? (
            <p className="text-sm text-red-600">
              {mutation.error instanceof Error ? mutation.error.message : 'Не удалось сохранить материал.'}
            </p>
          ) : null}
          <div className="rounded-lg border border-[var(--border)] bg-[var(--bg)] p-4 text-sm">
            <p className="font-medium">Локальный исходник</p>
            <p className="mt-1 text-xs text-[var(--muted)]">
              Выберите место для папки publisher. Текст, параметры публикации и вложения сохраняются в ней. При отсутствии сети медиа
              остаются локально и будут отправлены на сервер при следующем сохранении материала.
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <button
                type="button"
                className="rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-sm hover:border-[var(--accent)]"
                onClick={() => {
                  void choosePostFolder().then((name) => {
                    setFolderName(name)
                    setLocalError(null)
                    const values = form.getValues()
                    if (values.title.trim() || (values.bodySource ?? '').trim()) {
                      return persistLocal(buildPayload(values)).then(() => setLocalMessage('Локальная копия сохранена.'))
                    }
                  }).catch((error: unknown) => {
                    if (error instanceof DOMException && error.name === 'AbortError') return
                    setLocalError(error instanceof Error ? error.message : 'Не удалось выбрать папку.')
                  })
                }}
                disabled={!folderPickerAvailable()}
              >
                Выбрать папку
              </button>
              <span className="text-xs text-[var(--muted)]">
                {folderName ? `Папка: ${folderName}/publisher` : 'Папка не выбрана'}
              </span>
            </div>
            {!folderPickerAvailable() ? (
              <p className="mt-2 text-xs text-amber-700">Браузер не поддерживает выбор папки для записи.</p>
            ) : null}
            {localMessage ? <p className="mt-2 text-xs text-emerald-700" role="status">{localMessage}</p> : null}
            {localError ? <p className="mt-2 text-xs text-red-600" role="alert">{localError}</p> : null}
          </div>
          {!isNew && existing.data?.channelSyndicationBlocked ? (
            <div
              className="rounded-lg border border-amber-500/50 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-500/30 dark:bg-amber-950/40 dark:text-amber-100"
              role="status"
            >
              Плановая публикация в каналах пропущена (синхронизация с сервера наступила после времени). Автопост
              не выполнялся. Внесите правки и сохраните снова — тогда публикация в соцсети сможет пойти в обычном
              порядке (если включены каналы и статус «Опубликован» + публичная видимость).
            </div>
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

          <div className="space-y-2">
            <span className="block text-sm font-medium">Категория</span>
            {categoriesQ.isLoading ? (
              <p className="text-xs text-[var(--muted)]">Загрузка категорий…</p>
            ) : (
              <>
                <select
                  className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
                  value={watch('categoryId') ?? ''}
                  onChange={(e) => {
                    const v = e.target.value
                    setValue('categoryId', v === '' ? null : Number(v), { shouldDirty: true })
                  }}
                >
                  <option value="">Без категории</option>
                  {(categoriesQ.data ?? []).map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                <div className="flex flex-wrap items-end gap-2">
                  <label className="block min-w-[12rem] flex-1 text-xs text-[var(--muted)]">
                    Новая категория
                    <input
                      type="text"
                      className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm text-[var(--text)]"
                      placeholder="Название"
                      value={newCategoryName}
                      onChange={(e) => setNewCategoryName(e.target.value)}
                    />
                  </label>
                  <button
                    type="button"
                    disabled={!newCategoryName.trim() || createCategoryMut.isPending}
                    className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-[var(--bg)] disabled:opacity-50"
                    onClick={() => createCategoryMut.mutate(newCategoryName.trim())}
                  >
                    {createCategoryMut.isPending ? 'Создание…' : 'Создать и выбрать'}
                  </button>
                </div>
                {createCategoryMut.isError ? (
                  <p className="text-xs text-red-600">Не удалось создать категорию</p>
                ) : null}
              </>
            )}
          </div>

          <div>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <label htmlFor="post-body-source" className="text-sm font-medium">Текст статьи (Markdown)</label>
              <button
                type="button"
                className="rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-1.5 text-sm hover:border-[var(--accent)]"
                onClick={() => openStudio('text')}
              >
                Улучшить текст
              </button>
            </div>
            <textarea
              id="post-body-source"
              rows={14}
              className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('bodySource')}
            />
            <span className="mt-1 block text-xs text-[var(--muted)]">
              Заголовки, списки, ссылки, код — в формате Markdown. Для сайта HTML собирается автоматически.
            </span>
          </div>

          <label className="block text-sm font-medium">
            Плановая публикация (локальное время)
            <input
              type="datetime-local"
              className="mt-1 w-full max-w-md rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
              value={scheduledLocal}
              onChange={(e) => setScheduledLocal(e.target.value)}
            />
            <span className="mt-1 block text-xs text-[var(--muted)]">
              Необязательно. Для офлайн-редакторов: если к моменту синхронизации время уже прошло, кросс-пост в каналы
              не пойдёт по расписанию — снимите блок правкой и сохраните (см. предупреждение выше).
            </span>
          </label>

          <div className="rounded-lg border border-[var(--border)] bg-[var(--bg)] p-3">
            <p className="text-xs font-medium text-[var(--text)]">Как будет на сайте (авто из Markdown)</p>
            <div
              className="markdown-preview mt-2 max-h-48 overflow-y-auto rounded border border-[var(--border)] bg-[var(--surface)] p-3 text-sm leading-relaxed text-[var(--text)] [&_a]:text-[var(--accent)] [&_code]:rounded [&_code]:bg-[var(--bg)] [&_code]:px-1 [&_h1]:text-xl [&_h2]:text-lg [&_h3]:text-base [&_li]:my-0.5 [&_ol]:my-2 [&_p]:my-2 [&_pre]:overflow-x-auto [&_ul]:my-2"
              dangerouslySetInnerHTML={{ __html: generatedPreview || '<p class="text-[var(--muted)]">Пусто</p>' }}
            />
          </div>

          <details
            className="rounded-lg border border-[var(--border)] bg-[var(--bg)] p-3"
            open={htmlAdvancedOpen}
            onToggle={(e) => {
              const open = (e.target as HTMLDetailsElement).open
              setHtmlAdvancedOpen(open)
              if (open && !(form.getValues('bodyHtml') ?? '').trim()) {
                setValue('bodyHtml', articleSourceToHtml(form.getValues('bodySource') ?? ''), {
                  shouldDirty: false,
                })
              }
            }}
          >
            <summary className="cursor-pointer text-sm font-medium text-[var(--text)]">
              HTML для публикации (дополнительно)
            </summary>
            <p className="mt-2 text-xs text-[var(--muted)]">
              Обычно не нужно: HTML генерируется из поля «Текст статьи». Откройте блок, только если хотите вручную
              подправить разметку (опытные авторы).
            </p>
            <textarea
              rows={8}
              className="mt-2 w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
              {...form.register('bodyHtml')}
            />
          </details>

          <div className="space-y-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-4">
            <p className="text-sm font-medium">Медиа к материалу</p>
            <p className="text-xs text-[var(--muted)]">
              Выберите файлы с компьютера — сначала они сохраняются в папке материала. Порядок в
              списке — как в публикации (сверху вниз). Для Telegram в канал уходят изображения из этого списка
              (после текста поста).
            </p>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept="image/*,video/*,audio/*"
              multiple
              onChange={(e) => {
                void (async () => {
                  const list = e.target.files
                  if (!list?.length) {
                    return
                  }
                  setUploadErr(null)
                  setUploadBusy(true)
                  try {
                    for (let i = 0; i < list.length; i++) {
                      const f = list[i]
                      const localMedia = await saveSelectedMedia(localIdRef.current, f)
                      let row: MediaRow = { ...localMedia, id: null }
                      setMediaItems((prev) => [...prev, row])
                      try {
                        const asset = await uploadMedia(f)
                        row = { ...row, id: asset.id, serverId: asset.id, mimeType: asset.mimeType }
                        setMediaItems((prev) => prev.map((item) => item.fileName === row.fileName ? row : item))
                      } catch {
                        setUploadErr('Файл сохранён локально. Загрузка на сервер повторится при сохранении материала.')
                      }
                    }
                  } catch {
                    setUploadErr('Не удалось сохранить файл в локальной папке. Выберите папку и повторите попытку.')
                  } finally {
                    setUploadBusy(false)
                    e.target.value = ''
                  }
                })()
              }}
            />
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                disabled={uploadBusy}
                className="rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-sm hover:bg-[var(--bg)] disabled:opacity-50"
                onClick={() => fileInputRef.current?.click()}
              >
                {uploadBusy ? 'Загрузка…' : 'Загрузить файлы'}
              </button>
              <button
                type="button"
                className="rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-sm hover:border-[var(--accent)]"
                onClick={() => openStudio('image')}
              >
                Создать изображение
              </button>
            </div>
            {uploadErr ? <p className="text-xs text-red-600">{uploadErr}</p> : null}
            {mediaItems.length === 0 ? (
              <p className="text-xs text-[var(--muted)]">Пока нет вложений.</p>
            ) : (
              <ul className="max-h-96 space-y-3 overflow-y-auto">
                {mediaItems.map((m, idx) => (
                  <li key={m.fileName || m.id}>
                    <div className="flex gap-3 rounded-lg border border-[var(--border)] p-2">
                      {m.fileName ? (
                        <LocalMediaThumb localId={localIdRef.current} fileName={m.fileName} mimeType={m.mimeType} />
                      ) : m.id !== null ? (
                        <AuthenticatedMediaThumb mediaId={m.id} mimeType={m.mimeType} />
                      ) : null}
                      <div className="min-w-0 flex-1 text-sm">
                        <span className="font-mono text-xs text-[var(--muted)]">{m.id ? `#${m.id}` : 'локально'}</span>
                        <p className="truncate text-[var(--text)]">{m.mimeType ?? 'файл'}</p>
                      </div>
                      <div className="flex shrink-0 flex-col gap-1">
                        <button
                          type="button"
                          className="rounded border border-[var(--border)] px-2 py-0.5 text-xs hover:bg-[var(--surface)] disabled:opacity-40"
                          disabled={idx === 0}
                          onClick={() => {
                            setMediaItems((prev) => {
                              const copy = [...prev]
                              ;[copy[idx - 1], copy[idx]] = [copy[idx], copy[idx - 1]]
                              return copy
                            })
                          }}
                          aria-label="Выше"
                        >
                          ↑
                        </button>
                        <button
                          type="button"
                          className="rounded border border-[var(--border)] px-2 py-0.5 text-xs hover:bg-[var(--surface)] disabled:opacity-40"
                          disabled={idx >= mediaItems.length - 1}
                          onClick={() => {
                            setMediaItems((prev) => {
                              const copy = [...prev]
                              ;[copy[idx], copy[idx + 1]] = [copy[idx + 1], copy[idx]]
                              return copy
                            })
                          }}
                          aria-label="Ниже"
                        >
                          ↓
                        </button>
                        <button
                          type="button"
                          className="rounded border border-[var(--border)] px-2 py-0.5 text-xs text-red-600 hover:bg-[var(--surface)]"
                          onClick={() => {
                            setMediaItems((prev) => prev.filter((_, i) => i !== idx))
                          }}
                        >
                          Убрать
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
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
                      {channelFullName(c.channelType)}
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
              опубликованный публичный пост. Ниже — фактический статус по каждому выбранному каналу и метрики.
            </p>
            {!isNew && existing.data && (existing.data.outbound?.length ?? 0) > 0 ? (
              <div className="mt-2 border-t border-[var(--border)] pt-3">
                <p className="text-sm font-medium">Статус публикации на площадках</p>
                <ul className="mt-2 space-y-2 text-xs">
                  {(existing.data.outbound ?? []).map((o) => {
                    const tone = deliveryStatusTone(o.deliveryStatus)
                    const border =
                      tone === 'ok'
                        ? 'border-emerald-500/35'
                        : tone === 'bad'
                          ? 'border-red-400/40'
                          : tone === 'warn'
                            ? 'border-amber-500/40'
                            : 'border-[var(--border)]'
                    return (
                      <li
                        key={o.channelType}
                        className={`rounded-md border bg-[var(--surface)] p-2 ${border}`}
                      >
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium">{channelFullName(o.channelType)}</span>
                          <span
                            className={
                              tone === 'ok'
                                ? 'text-emerald-600 dark:text-emerald-400'
                                : tone === 'bad'
                                  ? 'text-red-600 dark:text-red-400'
                                  : tone === 'warn'
                                    ? 'text-amber-700 dark:text-amber-400'
                                    : 'text-[var(--muted)]'
                            }
                          >
                            {deliveryStatusLabel(o.deliveryStatus)}
                          </span>
                          {o.externalUrl && o.deliveryStatus === 'SENT' ? (
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
                        {o.lastError && (o.deliveryStatus === 'FAILED' || o.deliveryStatus === 'REJECTED') ? (
                          <p className="mt-1 text-red-600 dark:text-red-400">{o.lastError}</p>
                        ) : null}
                        {o.deliveryStatus === 'SENT' ? (
                          <p className="mt-1 text-[var(--muted)]">
                            Лайки {o.likes} · Репосты {o.reposts} · Просмотры {o.views}
                            {o.comments > 0 ? ` · Комментарии ${o.comments}` : ''}
                            {o.shares > 0 && o.shares !== o.reposts ? ` · Шары ${o.shares}` : ''}
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
                    )
                  })}
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
              disabled={mutation.isPending || uploadBusy}
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
