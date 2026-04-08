import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { fetchPublicPosts } from '../api/posts'

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
      <header className="space-y-1 border-b border-[var(--border)] pb-6">
        <p className="text-xs font-medium uppercase tracking-widest text-[var(--muted)]">Публичная лента</p>
        <h1 className="text-2xl font-semibold tracking-tight">{slug}</h1>
      </header>

      {q.isLoading ? (
        <p className="text-sm text-[var(--muted)]">Загрузка…</p>
      ) : q.isError ? (
        <p className="text-sm text-red-600">Не удалось загрузить материалы</p>
      ) : (
        <ul className="space-y-6">
          {q.data?.content.map((post) => (
            <li key={post.id}>
              <article>
                <h2 className="text-xl font-medium">
                  <Link
                    className="hover:text-[var(--accent)]"
                    to={`/blog/${slug}/p/${post.slug}`}
                  >
                    {post.title}
                  </Link>
                </h2>
                {post.excerpt ? <p className="mt-2 text-[var(--muted)]">{post.excerpt}</p> : null}
                {post.publishedAt ? (
                  <p className="mt-3 text-xs text-[var(--muted)]">
                    {new Date(post.publishedAt).toLocaleString()}
                  </p>
                ) : null}
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
