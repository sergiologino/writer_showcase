import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { fetchPublicPost } from '../api/posts'

export function PublicPostPage() {
  const { workspaceSlug, postSlug } = useParams<{ workspaceSlug: string; postSlug: string }>()
  const ws = workspaceSlug ?? ''
  const ps = postSlug ?? ''

  const q = useQuery({
    queryKey: ['public-post', ws, ps],
    queryFn: () => fetchPublicPost(ws, ps),
    enabled: !!ws && !!ps,
  })

  if (!ws || !ps) {
    return null
  }

  if (q.isLoading) {
    return <p className="mx-auto max-w-2xl py-10 text-sm text-[var(--muted)]">Загрузка…</p>
  }

  if (q.isError || !q.data) {
    return (
      <div className="mx-auto max-w-2xl py-10">
        <p className="text-sm text-red-600">Материал не найден</p>
        <Link className="mt-4 inline-block text-sm text-[var(--accent)]" to={`/blog/${ws}`}>
          ← к ленте
        </Link>
      </div>
    )
  }

  const post = q.data

  return (
    <article className="mx-auto max-w-2xl space-y-6 py-10">
      <Link className="text-sm text-[var(--muted)] hover:text-[var(--accent)]" to={`/blog/${ws}`}>
        ← все материалы
      </Link>
      <header className="space-y-2 border-b border-[var(--border)] pb-6">
        <h1 className="text-3xl font-semibold tracking-tight">{post.title}</h1>
        {post.publishedAt ? (
          <p className="text-xs text-[var(--muted)]">{new Date(post.publishedAt).toLocaleString()}</p>
        ) : null}
      </header>
      {post.excerpt ? <p className="text-lg text-[var(--muted)]">{post.excerpt}</p> : null}
      <div
        className="space-y-4 leading-relaxed [&_a]:text-[var(--accent)] [&_p]:my-3"
        dangerouslySetInnerHTML={{ __html: post.bodyHtml ?? '' }}
      />
    </article>
  )
}
