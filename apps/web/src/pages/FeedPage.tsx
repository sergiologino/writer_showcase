import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchPostPage } from '../api/posts'
import type { PostStatus } from '../api/types'
import { useState } from 'react'

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
        <p className="mt-1 text-sm text-[var(--muted)]">Черновики и опубликованные материалы в одном месте</p>
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
        <p className="text-sm text-red-600">Не удалось загрузить ленту</p>
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
              </Link>
            </li>
          ))}
        </ul>
      )}

      {query.data && query.data.content.length === 0 ? (
        <p className="text-sm text-[var(--muted)]">
          Пока пусто.{' '}
          <Link className="text-[var(--accent)] hover:underline" to="/app/posts/new">
            Создайте первый материал
          </Link>
        </p>
      ) : null}
    </div>
  )
}
