import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchPostPage } from '../api/posts'
import type { ChannelType, PostStatus } from '../api/types'
import { useState } from 'react'

function channelShort(t: ChannelType): string {
  const m: Record<ChannelType, string> = {
    TELEGRAM: 'TG',
    VK: 'ВК',
    ODNOKLASSNIKI: 'ОК',
  }
  return m[t] ?? t
}

const statusLabel: Record<PostStatus, string> = {
  DRAFT: 'Черновик',
  REVIEW: 'На review',
  PUBLISHED: 'Опубликован',
  ARCHIVED: 'Архив',
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
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Лента</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Черновики и опубликованные материалы в одном месте. В{' '}
          <span className="text-[var(--text)]">публичном блоге</span> показываются только посты со статусом «Опубликован»
          и публичной видимостью.
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
                </div>
                <h2 className="mt-2 text-lg font-medium text-[var(--text)]">{post.title}</h2>
                {post.excerpt ? (
                  <p className="mt-2 line-clamp-2 text-sm text-[var(--muted)]">{post.excerpt}</p>
                ) : null}
                {(post.outbound?.length ?? 0) > 0 ? (
                  <ul className="mt-3 flex flex-wrap gap-2 text-[11px] text-[var(--muted)]">
                    {(post.outbound ?? []).map((o) => (
                      <li
                        key={o.channelType}
                        className="rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1"
                      >
                        <span className="font-medium text-[var(--text)]">{channelShort(o.channelType)}</span>
                        {o.deliveryStatus === 'SENT' ? (
                          <span className="ml-1 text-emerald-600 dark:text-emerald-400">✓</span>
                        ) : (
                          <span className="ml-1 text-amber-600 dark:text-amber-400">!</span>
                        )}
                        {o.deliveryStatus === 'SENT' && (o.views > 0 || o.likes > 0) ? (
                          <span className="ml-1 opacity-90">
                            👁 {o.views} · ♥ {o.likes}
                            {o.reposts > 0 ? ` · ↻ ${o.reposts}` : ''}
                          </span>
                        ) : null}
                      </li>
                    ))}
                  </ul>
                ) : null}
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
