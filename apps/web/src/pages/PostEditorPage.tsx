import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { createPost, fetchPost, updatePost, type PostPayload } from '../api/posts'
import type { PostStatus, PostVisibility } from '../api/types'

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
    }
  }, [existing.data, form])

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
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
      }
      if (isNew) {
        return createPost(payload)
      }
      return updatePost(Number(id), payload)
    },
    onSuccess: (post) => {
      qc.invalidateQueries({ queryKey: ['posts'] })
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
