import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchPostPage } from '../api/posts'
import type { PostOutboundInfo, PostResponse, PostStatus, PostVisibility } from '../api/types'
import { Seo } from '../components/Seo'
import { channelShortName, deliveryStatusLabel, deliveryStatusTone } from '../lib/channelPublish'
import { listLocalPosts, type LocalPost } from '../lib/localPosts'

const statusLabel: Record<PostStatus, string> = {
  DRAFT: 'Черновик', REVIEW: 'На review', PUBLISHED: 'Опубликован', ARCHIVED: 'Архив',
}
const visibilityLabel: Record<PostVisibility, string> = {
  PUBLIC: 'Публичная', PRIVATE: 'Приватная', UNLISTED: 'По ссылке',
}
const statusStyle: Record<PostStatus, string> = {
  DRAFT: 'border-[var(--border)] bg-[var(--surface-muted)] text-[var(--muted)]',
  REVIEW: 'border-[var(--warning)]/40 bg-[var(--warning)]/10 text-[var(--warning)]',
  PUBLISHED: 'border-[var(--success)]/40 bg-[var(--success)]/10 text-[var(--success)]',
  ARCHIVED: 'border-[var(--border)] bg-[var(--surface-muted)] text-[var(--muted)]',
}

function formatDate(value: string | null | undefined): string | null {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }).format(date)
}

function toneClasses(tone: ReturnType<typeof deliveryStatusTone>): string {
  switch (tone) {
    case 'ok': return 'border-[var(--success)]/40 bg-[var(--success)]/10'
    case 'bad': return 'border-[var(--danger)]/40 bg-[var(--danger)]/10'
    case 'warn': return 'border-[var(--warning)]/40 bg-[var(--warning)]/10'
    default: return 'border-[var(--border)] bg-[var(--surface-muted)]'
  }
}

function OutboundChips({ outbound }: { outbound: PostOutboundInfo[] }) {
  return (
    <ul className="mt-4 flex flex-wrap gap-2 text-xs">
      {outbound.map((item) => {
        const hasMetrics = item.deliveryStatus === 'SENT' &&
          (item.views > 0 || item.likes > 0 || item.reposts > 0 || item.comments > 0)
        return (
          <li key={item.channelType} className={`max-w-full rounded-[var(--radius-control)] border px-2.5 py-1.5 ${toneClasses(deliveryStatusTone(item.deliveryStatus))}`}>
            <span className="font-medium">{channelShortName(item.channelType)}</span>
            <span className="ml-1 text-[var(--muted)]">· {deliveryStatusLabel(item.deliveryStatus)}</span>
            {item.externalUrl && item.deliveryStatus === 'SENT' ? (
              <a className="ml-2 text-[var(--accent)] underline-offset-2 hover:underline" href={item.externalUrl} target="_blank" rel="noreferrer">Открыть</a>
            ) : null}
            {hasMetrics ? (
              <span className="ml-2 text-[var(--muted)]">
                Просмотры: {item.views} · Нравится: {item.likes}
                {item.reposts > 0 ? ` · Репосты: ${item.reposts}` : ''}
                {item.comments > 0 ? ` · Комментарии: ${item.comments}` : ''}
              </span>
            ) : null}
            {item.lastError && (item.deliveryStatus === 'FAILED' || item.deliveryStatus === 'REJECTED') ? (
              <p className="mt-1 break-words text-[var(--muted)]">{item.lastError}</p>
            ) : null}
          </li>
        )
      })}
    </ul>
  )
}

function PostCard({ post }: { post: PostResponse }) {
  const date = formatDate(post.publishedAt ?? post.updatedAt)
  const mediaCount = post.media?.length ?? 0
  const summary = post.excerpt?.trim() || post.bodySource?.replace(/\s+/g, ' ').trim().slice(0, 200)
  return (
    <article className="ui-card min-w-0 p-4 transition-colors hover:border-[var(--border-strong)] sm:p-5">
      <div className="flex flex-wrap items-center gap-2 text-xs">
        <span className={`rounded-full border px-2.5 py-1 font-medium ${statusStyle[post.status]}`}>{statusLabel[post.status]}</span>
        <span className="ui-badge">{visibilityLabel[post.visibility]}</span>
        {date ? <time className="text-[var(--muted)]" dateTime={post.publishedAt ?? post.updatedAt}>{date}</time> : null}
      </div>
      <h2 className="font-editorial mt-3 break-words text-lg font-semibold leading-snug sm:text-xl">
        <Link className="hover:text-[var(--accent)]" to={`/app/posts/${post.id}`}>{post.title || 'Без заголовка'}</Link>
      </h2>
      {summary ? <p className="mt-2 line-clamp-2 break-words text-sm leading-relaxed text-[var(--muted)]">{summary}</p> : null}
      <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-[var(--border)] pt-3 text-xs text-[var(--muted)]">
        {mediaCount > 0 ? <span>Вложения: {mediaCount}</span> : null}
        {(post.aiTokensTotal ?? 0) > 0 ? <span title="Накопительно по вызовам ИИ для статьи">ИИ: {post.aiTokensTotal} ток.</span> : null}
        <Link className="ml-auto font-medium text-[var(--accent)] hover:underline" to={`/app/posts/${post.id}`}>Открыть материал →</Link>
      </div>
      {(post.outbound?.length ?? 0) > 0 ? <OutboundChips outbound={post.outbound} /> : null}
    </article>
  )
}

export function FeedPage() {
  const [status, setStatus] = useState<PostStatus | ''>('')
  const [search, setSearch] = useState('')
  const [queryText, setQueryText] = useState('')
  const [page, setPage] = useState(0)
  const [localPosts, setLocalPosts] = useState<LocalPost[]>([])
  const [localError, setLocalError] = useState<string | null>(null)
  const [localLoading, setLocalLoading] = useState(true)
  const workspaceId = localStorage.getItem('workspaceId') ?? 'default'

  useEffect(() => {
    const timer = window.setTimeout(() => setQueryText(search.trim()), 250)
    return () => window.clearTimeout(timer)
  }, [search])

  useEffect(() => {
    let active = true
    void listLocalPosts(workspaceId).then((posts) => {
      if (active) setLocalPosts(posts)
    }).catch(() => {
      if (active) setLocalError('Не удалось прочитать локальную базу материалов.')
    }).finally(() => {
      if (active) setLocalLoading(false)
    })
    return () => { active = false }
  }, [workspaceId])

  const query = useQuery({
    queryKey: ['posts', status || 'all', queryText, page],
    queryFn: () => fetchPostPage({ status: status || undefined, q: queryText || undefined, page }),
  })

  return (
    <div className="space-y-6">
      <Seo title="Лента материалов" description="Лента материалов в рабочем пространстве Altacod Publisher: черновики, публикации и статусы отправки в каналы." keywords="лента материалов, редактор блога, черновики автора, публикация в каналы" canonicalPath="/app/feed" noIndex />
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-editorial text-2xl font-semibold tracking-tight sm:text-3xl">Лента</h1>
          <p className="mt-1 max-w-2xl text-sm text-[var(--muted)]">Черновики, публикации и отправка в подключённые каналы в одном месте.</p>
        </div>
        <Link className="ui-button ui-button--primary" to="/app/posts/new">Новый материал</Link>
      </header>

      <section className="ui-card p-4 sm:p-5" aria-labelledby="local-posts-heading">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 id="local-posts-heading" className="font-editorial text-lg font-semibold">Локальные материалы</h2>
            <p className="mt-1 text-xs text-[var(--muted)]">Копии хранятся в этом браузере на этом устройстве. Файлы можно посмотреть в локальном хранилище.</p>
          </div>
          <Link className="text-sm font-medium text-[var(--accent)] hover:underline" to="/app/local-storage">Открыть хранилище →</Link>
        </div>
        {localError ? <p className="ui-notice mt-3 text-sm text-[var(--danger)]" role="alert">{localError}</p> : null}
        {localLoading ? <p className="mt-3 text-sm text-[var(--muted)]" role="status">Загрузка локальных материалов…</p> : localPosts.length > 0 ? (
          <ul className="mt-4 divide-y divide-[var(--border)] border-t border-[var(--border)]">
            {localPosts.map((post) => (
              <li key={post.key} className="flex min-w-0 flex-wrap items-center justify-between gap-2 py-3">
                <div className="min-w-0">
                  <Link className="break-words text-sm font-medium hover:text-[var(--accent)]" to={`/app/posts/${post.serverId ?? `local-${post.localId}`}`}>{post.payload.title || 'Без заголовка'}</Link>
                  <p className="mt-0.5 text-xs text-[var(--muted)]">
                    {post.serverId ? 'Локальная копия' : 'Только на этом устройстве'}
                    {formatDate(post.savedAt) ? ` · сохранено ${formatDate(post.savedAt)}` : ''}
                    {(post.media?.length ?? 0) > 0 ? ` · вложений: ${post.media?.length}` : ''}
                  </p>
                </div>
                <span className="ui-badge">{statusLabel[post.payload.status]}</span>
              </li>
            ))}
          </ul>
        ) : !localError ? <p className="mt-3 text-sm text-[var(--muted)]">Локальных материалов пока нет.</p> : null}
      </section>

      <section aria-labelledby="server-posts-heading" className="space-y-4">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 id="server-posts-heading" className="font-editorial text-xl font-semibold">Публикации</h2>
          {query.data ? <p className="text-xs text-[var(--muted)]">Найдено: {query.data.totalElements}</p> : null}
        </div>
        <div className="ui-card flex flex-col gap-3 p-4 sm:flex-row sm:items-end">
          <label className="block min-w-0 flex-1 text-sm font-medium">
            Поиск по публикациям
            <input className="ui-field mt-1 w-full text-sm" type="search" placeholder="Заголовок или отрывок" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} />
          </label>
          <label className="block text-sm font-medium sm:w-48">
            Статус
            <select className="ui-field mt-1 w-full text-sm" value={status} onChange={(event) => { setStatus(event.target.value as PostStatus | ''); setPage(0) }}>
              <option value="">Все статусы</option>
              <option value="DRAFT">Черновики</option>
              <option value="PUBLISHED">Опубликованные</option>
              <option value="REVIEW">Review</option>
              <option value="ARCHIVED">Архив</option>
            </select>
          </label>
        </div>

        {query.isLoading ? <p className="ui-notice text-sm text-[var(--muted)]" role="status">Загрузка публикаций…</p> : query.isError ? (
          <div className="ui-notice text-sm" role="alert">
            <p className="font-medium text-[var(--danger)]">Не удалось загрузить ленту</p>
            <p className="mt-1 text-xs text-[var(--muted)]">{query.error instanceof ApiError ? `${query.error.message} (код ${query.error.status})` : query.error instanceof Error ? query.error.message : 'Проверьте соединение и повторите попытку.'}</p>
            <button className="ui-button ui-button--secondary mt-3" type="button" onClick={() => void query.refetch()}>Повторить</button>
          </div>
        ) : query.data?.content.length ? (
          <ul className="space-y-3">
            {query.data.content.map((post) => <li key={post.id}><PostCard post={post} /></li>)}
          </ul>
        ) : (
          <div className="ui-empty-state">
            <p>Материалы не найдены.</p>
            {status || search ? <p className="mt-1 text-sm">Измените поиск или статус.</p> : <Link className="mt-2 inline-block text-sm text-[var(--accent)] hover:underline" to="/app/posts/new">Создать материал</Link>}
          </div>
        )}

        {query.data && query.data.totalPages > 1 ? (
          <nav className="flex flex-wrap items-center justify-between gap-3" aria-label="Страницы публикаций">
            <button className="ui-button ui-button--secondary" type="button" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>← Назад</button>
            <span className="text-sm text-[var(--muted)]">Страница {page + 1} из {query.data.totalPages}</span>
            <button className="ui-button ui-button--secondary" type="button" disabled={page + 1 >= query.data.totalPages} onClick={() => setPage((current) => current + 1)}>Далее →</button>
          </nav>
        ) : null}
      </section>
    </div>
  )
}
