import { useQuery } from '@tanstack/react-query'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { resolveApiUrl } from '../api/client'
import { fetchPublicPosts } from '../api/posts'
import type { PublicPostSummary } from '../api/types'
import { PublicAuthorChip } from '../components/PublicAuthorChip'
import { PublicPostOwnerActions } from '../components/PublicPostOwnerActions'
import { PublicSiteNav } from '../components/PublicSiteNav'
import { Seo } from '../components/Seo'
import { StructuredData } from '../components/StructuredData'
import { initialBlogData } from '../lib/publicBootstrap'

function pageNumber(value: string | null): number {
  const parsed = Number(value)
  return value && Number.isSafeInteger(parsed) && parsed > 0 ? parsed - 1 : 0
}

function summaryText(post: PublicPostSummary): string | null {
  const text = post.excerpt?.trim() || post.bodyPreviewPlain?.trim()
  if (!text) return null
  return text.length > 220 ? `${text.slice(0, 220).trimEnd()}…` : text
}

function FirstMediaThumb({ post }: { post: PublicPostSummary }) {
  if (!post.firstMediaUrl) return null
  const src = resolveApiUrl(post.firstMediaUrl)
  if (post.firstMediaMimeType?.startsWith('image/')) {
    return <img src={src} alt="" width={640} height={400} loading="lazy" decoding="async"
      className="aspect-[8/5] w-full rounded-t-[var(--radius-card)] object-cover" />
  }
  if (post.firstMediaMimeType?.startsWith('video/')) {
    return <video src={src} aria-label={`Видео к статье «${post.title}»`} muted preload="none" playsInline
      className="aspect-[8/5] w-full rounded-t-[var(--radius-card)] object-cover" />
  }
  return null
}

function BlogCard({ post, slug }: { post: PublicPostSummary; slug: string }) {
  const path = `/blog/${slug}/p/${post.slug}`
  const publishedAt = post.publishedAt ? new Date(post.publishedAt) : null
  const hasDate = publishedAt && !Number.isNaN(publishedAt.getTime())
  return (
    <article className="ui-card flex min-w-0 flex-col overflow-hidden">
      {post.firstMediaUrl && post.firstMediaMimeType?.startsWith('image/') ? (
        <Link to={path} aria-label={`Читать «${post.title}»`}><FirstMediaThumb post={post} /></Link>
      ) : <FirstMediaThumb post={post} />}
      <div className="flex min-w-0 flex-1 flex-col p-5">
        {hasDate ? (
          <time className="text-xs text-[var(--muted)]" dateTime={post.publishedAt!}>
            {publishedAt.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })}
          </time>
        ) : null}
        <h2 className="font-editorial mt-2 break-words text-xl font-semibold leading-snug">
          <Link className="hover:text-[var(--accent)]" to={path}>{post.title}</Link>
        </h2>
        {summaryText(post) ? <p className="mt-3 line-clamp-4 break-words text-sm leading-relaxed text-[var(--muted)]">{summaryText(post)}</p> : null}
        <div className="mt-auto pt-5">
          {post.authorDisplayName ? <PublicAuthorChip name={post.authorDisplayName} avatarUrl={post.authorAvatarUrl} /> : null}
          <Link className="mt-4 inline-block text-sm font-medium text-[var(--accent)] hover:underline" to={path}>Читать дальше →</Link>
        </div>
        <div className="mt-3"><PublicPostOwnerActions postId={post.id} authorId={post.authorId} workspaceSlug={slug} title={post.title} /></div>
      </div>
    </article>
  )
}

export function PublicBlogPage() {
  const { workspaceSlug } = useParams<{ workspaceSlug: string }>()
  const slug = workspaceSlug ?? ''
  const [searchParams] = useSearchParams()
  const page = pageNumber(searchParams.get('page'))

  const q = useQuery({
    queryKey: ['public-posts', slug, page],
    queryFn: () => fetchPublicPosts(slug, page),
    enabled: !!slug,
    initialData: () => initialBlogData(slug, page),
  })

  if (!slug) return <p className="text-sm text-[var(--muted)]">Не указан блог</p>

  const pageSuffix = page > 0 ? `?page=${page + 1}` : ''
  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-6 sm:py-8 lg:py-10">
      <Seo title={`Блог ${slug}${page > 0 ? ` — страница ${page + 1}` : ''}`}
        description={`Публичный блог ${slug}: статьи и материалы авторского пространства.`}
        keywords={`блог ${slug}, статьи ${slug}, авторский блог, публичные материалы`}
        canonicalPath={`/blog/${slug}${pageSuffix}`}
        noIndex={q.isError || (page > 0 && !!q.data && q.data.content.length === 0)} />
      {q.data && q.data.totalElements > 0 ? <StructuredData value={{
        '@context': 'https://schema.org',
        '@type': 'Blog',
        name: slug,
        url: new URL(`/blog/${slug}`, window.location.origin).toString(),
      }} /> : null}
      <PublicSiteNav />
      <header className="border-b border-[var(--border)] pb-7 pt-4 sm:pb-9">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[var(--muted)]">Авторский блог</p>
        <h1 className="font-editorial mt-3 break-words text-3xl font-semibold tracking-tight sm:text-5xl">{slug}</h1>
        <p className="mt-3 text-sm text-[var(--muted)]">
          {q.data ? `Публичные материалы: ${q.data.totalElements}` : 'Статьи и заметки авторского пространства'}
        </p>
      </header>

      {q.isLoading ? <p className="ui-notice text-sm text-[var(--muted)]" role="status">Загрузка материалов…</p> : q.isError ? (
        <div className="ui-notice text-sm" role="alert">
          <p className="text-[var(--danger)]">Не удалось загрузить материалы.</p>
          <button type="button" className="ui-button ui-button--secondary mt-3" onClick={() => void q.refetch()}>Повторить</button>
        </div>
      ) : q.data?.content.length ? (
        <ul className="grid items-start gap-5 md:grid-cols-2 xl:grid-cols-3">
          {q.data.content.map((post) => <li key={post.id} className="min-w-0"><BlogCard post={post} slug={slug} /></li>)}
        </ul>
      ) : <p className="ui-empty-state text-sm">Публичных материалов пока нет.</p>}

      {q.data && q.data.totalPages > 1 ? (
        <nav className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--border)] pt-5" aria-label="Страницы блога">
          {page > 0 ? <Link className="ui-button ui-button--secondary" to={page === 1 ? `/blog/${slug}` : `/blog/${slug}?page=${page}`}>← Назад</Link> : <span />}
          <span className="text-sm text-[var(--muted)]">Страница {page + 1} из {q.data.totalPages}</span>
          {page + 1 < q.data.totalPages ? <Link className="ui-button ui-button--secondary" to={`/blog/${slug}?page=${page + 2}`}>Далее →</Link> : <span />}
        </nav>
      ) : null}
    </div>
  )
}
