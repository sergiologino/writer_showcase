import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchPostPage } from '../api/posts'
import type { PostOutboundInfo, PostStatus } from '../api/types'
import {
  channelShortName,
  deliveryStatusLabel,
  deliveryStatusTone,
} from '../lib/channelPublish'
import { useState } from 'react'
import { Seo } from '../components/Seo'

const statusLabel: Record<PostStatus, string> = {
  DRAFT: 'Черновик',
  REVIEW: 'На review',
  PUBLISHED: 'Опубликован',
  ARCHIVED: 'Архив',
}

function toneClasses(tone: ReturnType<typeof deliveryStatusTone>): string {
  switch (tone) {
    case 'ok':
      return 'border-emerald-500/40 bg-emerald-500/5 text-[var(--text)]'
    case 'wait':
      return 'border-[var(--border)] bg-[var(--bg)] text-[var(--muted)]'
    case 'bad':
      return 'border-red-400/50 bg-red-500/5 text-[var(--text)]'
    case 'warn':
      return 'border-amber-500/50 bg-amber-500/5 text-[var(--text)]'
    default:
      return 'border-[var(--border)] bg-[var(--bg)]'
  }
}

function OutboundChips({ outbound }: { outbound: PostOutboundInfo[] }) {
  return (
    <ul className="mt-3 flex flex-col gap-2 text-[11px] sm:flex-row sm:flex-wrap">
      {outbound.map((o) => {
        const tone = deliveryStatusTone(o.deliveryStatus)
        const label = deliveryStatusLabel(o.deliveryStatus)
        const hasMetrics =
          o.deliveryStatus === 'SENT' &&
          (o.views > 0 || o.likes > 0 || o.reposts > 0 || o.comments > 0)
        return (
          <li
            key={o.channelType}
            className={`rounded-lg border px-2.5 py-1.5 ${toneClasses(tone)}`}
          >
            <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
              <span className="font-semibold text-[var(--text)]">{channelShortName(o.channelType)}</span>
              <span className="opacity-90">{label}</span>
              {o.externalUrl && o.deliveryStatus === 'SENT' ? (
                <a
                  className="text-[var(--accent)] hover:underline"
                  href={o.externalUrl}
                  target="_blank"
                  rel="noreferrer"
                  onClick={(e) => e.stopPropagation()}
                >
                  ссылка
                </a>
              ) : null}
            </div>
            {o.lastError && (o.deliveryStatus === 'FAILED' || o.deliveryStatus === 'REJECTED') ? (
              <p className="mt-1 max-w-prose text-[10px] leading-snug opacity-90">{o.lastError}</p>
            ) : null}
            {o.deliveryStatus === 'SENT' ? (
              <p className="mt-1 text-[10px] opacity-90">
                {hasMetrics ? (
                  <>
                    👁 {o.views} · ♥ {o.likes}
                    {o.reposts > 0 ? ` · ↻ ${o.reposts}` : ''}
                    {o.comments > 0 ? ` · 💬 ${o.comments}` : ''}
                  </>
                ) : (
                  <span className="text-[var(--muted)]">метрики появятся после синхронизации</span>
                )}
                {o.metricsFetchedAt ? (
                  <span className="ml-1 text-[var(--muted)]">
                    · {new Date(o.metricsFetchedAt).toLocaleString()}
                  </span>
                ) : null}
              </p>
            ) : null}
          </li>
        )
      })}
    </ul>
  )
}

export function FeedPage() {
  const [status, setStatus] = useState<PostStatus | ''>('')
  const [q, setQ] = useState('')

  const query = useQuery({
    queryKey: ['posts', status || 'all', q],
    queryFn: () => fetchPostPage({ status: status || undefined, q: q || undefined, page: 0 }),
  })

  return (
    <div className="space-y-6">
      <Seo
        title="Лента материалов"
        description="Лента материалов в рабочем пространстве Altacod Publisher: черновики, публикации и статусы отправки в каналы."
        keywords="лента материалов, редактор блога, черновики автора, публикация в каналы"
        canonicalPath="/app/feed"
        noIndex
      />
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Лента</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Черновики и опубликованные материалы в одном месте. В{' '}
          <span className="text-[var(--text)]">публичном блоге</span> показываются только посты со статусом «Опубликован»
          и публичной видимостью. Для каждого материала с соцпубликацией ниже видно статус по каждому выбранному каналу
          (ожидание, успех, ошибка, отклонение модератором) и метрики с площадок.
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:flex-row sm:items-end">
        <label className="block flex-1 text-sm">
          Поиск
          <input
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
            placeholder="Заголовок или отрывок"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </label>
        <label className="block w-full text-sm sm:w-44">
          Статус
          <select
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm"
            value={status}
            onChange={(e) => setStatus(e.target.value as PostStatus | '')}
          >
            <option value="">Все</option>
            <option value="DRAFT">Черновики</option>
            <option value="PUBLISHED">Опубликованные</option>
            <option value="REVIEW">Review</option>
            <option value="ARCHIVED">Архив</option>
          </select>
        </label>
      </div>

      {query.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка…</p>
      ) : query.isError ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          <p className="font-medium">Не удалось загрузить ленту</p>
          <p className="mt-1 text-xs opacity-90">
            {query.error instanceof ApiError
              ? `${query.error.message} (код ${query.error.status})`
              : query.error instanceof Error
                ? query.error.message
                : 'Проверьте, что API запущен, вы вошли в аккаунт и сессия не сброшена.'}
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {query.data?.content.map((post) => (
            <li key={post.id}>
              <Link
                to={`/app/posts/${post.id}`}
                className="block rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm transition hover:border-[var(--accent)]"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-medium uppercase tracking-wide text-[var(--muted)]">
                    {statusLabel[post.status]}
                  </span>
                  <span className="text-xs text-[var(--muted)]">{post.visibility}</span>
                  <span className="text-xs text-[var(--muted)]" title="Накопительно по вызовам AI для этой статьи">
                    ИИ: {post.aiTokensTotal ?? 0} ток.
                  </span>
                </div>
                <h2 className="mt-2 text-lg font-medium text-[var(--text)]">{post.title}</h2>
                {post.excerpt ? (
                  <p className="mt-2 line-clamp-2 text-sm text-[var(--muted)]">{post.excerpt}</p>
                ) : null}
                {(post.outbound?.length ?? 0) > 0 ? <OutboundChips outbound={post.outbound ?? []} /> : null}
              </Link>
            </li>
          ))}
        </ul>
      )}

      {query.data && query.data.content.length === 0 ? (
        <p className="text-sm text-[var(--muted)]">
          Пока пусто (или сбросьте фильтры выше). Черновик сохраняется со статусом «Черновик» — он виден здесь, но не в
          публичном блоге.{' '}
          <Link className="text-[var(--accent)] hover:underline" to="/app/posts/new">
            Создать материал
          </Link>
        </p>
      ) : null}
    </div>
  )
}
