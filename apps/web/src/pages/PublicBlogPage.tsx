import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { resolveApiUrl } from '../api/client'
import { PublicAuthorChip } from '../components/PublicAuthorChip'
import { PublicSiteNav } from '../components/PublicSiteNav'
import { fetchPublicPosts } from '../api/posts'
import { Seo } from '../components/Seo'

function FirstMediaThumb({
  url,
  mimeType,
}: {
  url: string
  mimeType: string | null
}) {
  const src = resolveApiUrl(url)
  if (mimeType?.startsWith('image/')) {
    return (
      <img
        src={src}
        alt=""
        className="aspect-[4/3] w-full rounded-lg object-cover sm:aspect-square sm:h-36 sm:w-36 sm:max-w-[40%]"
        loading="lazy"
      />
    )
  }
  if (mimeType?.startsWith('video/')) {
    return (
      <video
        src={src}
        className="aspect-video w-full rounded-lg object-cover sm:h-36 sm:w-auto sm:max-w-[40%] sm:object-cover"
        muted
        preload="metadata"
        playsInline
      />
    )
  }
  return (
    <div className="flex aspect-[4/3] w-full items-center justify-center rounded-lg border border-[var(--border)] bg-[var(--bg)] text-xs text-[var(--muted)] sm:aspect-square sm:h-36 sm:w-36">
      Вложение
    </div>
  )
}

export function PublicBlogPage() {
  const { workspaceSlug } = useParams<{ workspaceSlug: string }>()
  const slug = workspaceSlug ?? ''

  const q = useQuery({
    queryKey: ['public-posts', slug],
    queryFn: () => fetchPublicPosts(slug, 0),
    enabled: !!slug,
  })

  if (!slug) {
    return <p className="text-sm text-[var(--muted)]">Не указан блог</p>
  }

  return (
    <div className="mx-auto max-w-2xl space-y-8 py-10">
      <Seo
        title={`Блог ${slug}`}
        description={`Публичный блог ${slug}: новые статьи, заметки и материалы автора.`}
        keywords={`блог ${slug}, статьи ${slug}, авторский блог, публичные материалы`}
        canonicalPath={`/blog/${slug}`}
      />
      <PublicSiteNav />
      <header className="space-y-1 border-b border-[var(--border)] pb-6">
        <p className="text-xs font-medium uppercase tracking-widest text-[var(--muted)]">Публичная лента</p>
        <h1 className="text-2xl font-semibold tracking-tight">{slug}</h1>
      </header>

      {q.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка…</p>
      ) : q.isError ? (
        <p className="text-sm text-red-600">Не удалось загрузить материалы</p>
      ) : (
        <ul className="space-y-8">
          {q.data?.content.map((post) => (
            <li key={post.id}>
              <article className="overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--surface)] shadow-sm">
                <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-start">
                  {post.firstMediaUrl ? (
                    <div className="shrink-0 sm:max-w-[42%]">
                      <FirstMediaThumb url={post.firstMediaUrl} mimeType={post.firstMediaMimeType ?? null} />
                    </div>
                  ) : null}
                  <div className="min-w-0 flex-1 space-y-3">
                    {post.authorDisplayName ? (
                      <PublicAuthorChip
                        name={post.authorDisplayName}
                        avatarUrl={post.authorAvatarUrl}
                      />
                    ) : null}
                    <h2 className="text-xl font-medium leading-snug">
                      <Link
                        className="text-[var(--text)] hover:text-[var(--accent)]"
                        to={`/blog/${slug}/p/${post.slug}`}
                      >
                        {post.title}
                      </Link>
                    </h2>
                    {post.excerpt ? (
                      <p className="text-sm leading-relaxed text-[var(--muted)]">{post.excerpt}</p>
                    ) : null}
                    {post.bodyPreviewPlain ? (
                      <div className="relative">
                        <div className="max-h-[min(78vh,calc(1.65em*88))] overflow-hidden text-sm leading-relaxed text-[var(--text)] [overflow-wrap:anywhere] whitespace-pre-wrap">
                          {post.bodyPreviewPlain}
                        </div>
                        {post.bodyPreviewPlain.endsWith('…') ? (
                          <div
                            className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-[var(--surface)] via-[var(--surface)]/80 to-transparent"
                            aria-hidden
                          />
                        ) : null}
                      </div>
                    ) : null}
                    {post.publishedAt ? (
                      <p className="text-xs text-[var(--muted)]">
                        {new Date(post.publishedAt).toLocaleString()}
                      </p>
                    ) : null}
                    <p>
                      <Link
                        className="text-sm font-medium text-[var(--accent)] hover:underline"
                        to={`/blog/${slug}/p/${post.slug}`}
                      >
                        Читать дальше →
                      </Link>
                    </p>
                  </div>
                </div>
              </article>
            </li>
          ))}
        </ul>
      )}

      {q.data?.content.length === 0 ? (
        <p className="text-sm text-[var(--muted)]">Публичных материалов пока нет.</p>
      ) : null}
    </div>
  )
}
