import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { fetchPublicPost } from '../api/posts'
import { getStoredAccessToken, resolveApiUrl } from '../api/client'
import { fetchPublicEngagement, postPublicComment, togglePostLike } from '../api/publicEngagement'
import { PublicAuthorChip } from '../components/PublicAuthorChip'
import { PublicSiteNav } from '../components/PublicSiteNav'
import { Seo } from '../components/Seo'

export function PublicPostPage() {
  const { workspaceSlug, postSlug } = useParams<{ workspaceSlug: string; postSlug: string }>()
  const location = useLocation()
  const ws = workspaceSlug ?? ''
  const ps = postSlug ?? ''
  const qc = useQueryClient()
  const token = getStoredAccessToken()
  const [commentText, setCommentText] = useState('')

  const redirectToLogin = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
  const redirectToRegister = `/register?redirect=${encodeURIComponent(location.pathname + location.search)}`

  const q = useQuery({
    queryKey: ['public-post', ws, ps],
    queryFn: () => fetchPublicPost(ws, ps),
    enabled: !!ws && !!ps,
  })

  const engQ = useQuery({
    queryKey: ['public-engagement', ws, ps],
    queryFn: () => fetchPublicEngagement(ws, ps),
    enabled: !!ws && !!ps,
  })

  const likeMut = useMutation({
    mutationFn: () => togglePostLike(ws, ps),
    onSuccess: (data) => {
      qc.setQueryData(['public-engagement', ws, ps], data)
    },
  })

  const commentMut = useMutation({
    mutationFn: (body: string) => postPublicComment(ws, ps, body),
    onSuccess: (data) => {
      qc.setQueryData(['public-engagement', ws, ps], data)
      setCommentText('')
    },
  })

  const sharePost = async () => {
    const url = window.location.href
    const title = q.data?.title ?? ''
    try {
      if (navigator.share) {
        await navigator.share({ title, url })
      } else {
        await navigator.clipboard.writeText(url)
        alert('Ссылка скопирована в буфер обмена')
      }
    } catch (e) {
      if ((e as Error).name !== 'AbortError') {
        try {
          await navigator.clipboard.writeText(url)
          alert('Ссылка скопирована в буфер обмена')
        } catch {
          /* ignore */
        }
      }
    }
  }

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
  const eng = engQ.data

  return (
    <article className="mx-auto max-w-2xl space-y-6 py-10">
      <Seo
        title={post.title}
        description={post.excerpt || `Статья ${post.title} в публичном блоге ${ws}.`}
        keywords={[
          post.title,
          post.authorDisplayName,
          post.category?.name,
          ...post.tags.map((tag) => tag.name),
          'авторский блог',
          'статья',
        ]
          .filter(Boolean)
          .join(', ')}
        canonicalPath={`/blog/${ws}/p/${post.slug}`}
        image={post.media?.find((m) => m.mimeType?.startsWith('image/'))?.url ?? post.authorAvatarUrl}
        type="article"
      />
      <PublicSiteNav />
      <Link className="text-sm text-[var(--muted)] hover:text-[var(--accent)]" to={`/blog/${ws}`}>
        ← все материалы
      </Link>
      <header className="space-y-2 border-b border-[var(--border)] pb-6">
        {post.authorDisplayName ? (
          <PublicAuthorChip name={post.authorDisplayName} avatarUrl={post.authorAvatarUrl} className="pb-1" />
        ) : null}
        <h1 className="text-3xl font-semibold tracking-tight">{post.title}</h1>
        {post.publishedAt ? (
          <p className="text-xs text-[var(--muted)]">{new Date(post.publishedAt).toLocaleString()}</p>
        ) : null}
      </header>
      {post.excerpt ? <p className="text-lg text-[var(--muted)]">{post.excerpt}</p> : null}

      <div className="flex flex-wrap items-center gap-3 border-b border-[var(--border)] pb-4">
        {token ? (
          <button
            type="button"
            disabled={likeMut.isPending || engQ.isLoading}
            onClick={() => likeMut.mutate()}
            className={`rounded-lg border px-3 py-1.5 text-sm ${
              eng?.likedByMe
                ? 'border-[var(--accent)] bg-[var(--accent)]/10 text-[var(--accent)]'
                : 'border-[var(--border)] hover:bg-[var(--surface)]'
            }`}
          >
            {eng?.likedByMe ? '♥ Нравится' : '♡ Нравится'}
            {eng != null ? ` (${eng.likeCount})` : ''}
          </button>
        ) : (
          <span className="text-sm text-[var(--muted)]">
            <Link className="text-[var(--accent)] hover:underline" to={redirectToLogin}>
              Войдите
            </Link>
            , чтобы ставить лайки
          </span>
        )}
        <button
          type="button"
          onClick={() => void sharePost()}
          className="rounded-lg border border-[var(--border)] px-3 py-1.5 text-sm hover:bg-[var(--surface)]"
        >
          Поделиться…
        </button>
        {!token ? (
          <span className="text-xs text-[var(--muted)]">
            Нет аккаунта?{' '}
            <Link className="text-[var(--accent)] hover:underline" to={redirectToRegister}>
              Регистрация
            </Link>
          </span>
        ) : null}
      </div>

      {post.media?.length ? (
        <div className="space-y-4">
          {post.media.map((m) =>
            m.mimeType?.startsWith('image/') ? (
              <figure key={m.id} className="space-y-2">
                <img
                  src={resolveApiUrl(m.url)}
                  alt={m.altText ?? ''}
                  className="max-h-[480px] w-full rounded-lg object-contain"
                />
                {m.caption ? (
                  <figcaption className="text-sm text-[var(--muted)]">{m.caption}</figcaption>
                ) : null}
              </figure>
            ) : (
              <p key={m.id}>
                <a
                  className="text-sm text-[var(--accent)] hover:underline"
                  href={resolveApiUrl(m.url)}
                  target="_blank"
                  rel="noreferrer"
                >
                  Скачать вложение #{m.id}
                </a>
              </p>
            ),
          )}
        </div>
      ) : null}
      <div
        className="space-y-4 leading-relaxed [&_a]:text-[var(--accent)] [&_p]:my-3"
        dangerouslySetInnerHTML={{ __html: post.bodyHtml ?? '' }}
      />

      <section className="space-y-4 border-t border-[var(--border)] pt-6">
        <h2 className="text-lg font-medium">Комментарии</h2>
        {engQ.isLoading ? (
          <p className="text-sm text-[var(--muted)]">Загрузка…</p>
        ) : eng ? (
          <ul className="space-y-3 text-sm">
            {eng.comments.length === 0 ? (
              <li className="text-[var(--muted)]">Пока нет комментариев.</li>
            ) : (
              eng.comments.map((c) => (
                <li key={c.id} className="rounded-lg border border-[var(--border)] bg-[var(--surface)] p-3">
                  <p className="font-medium text-[var(--text)]">{c.authorDisplayName}</p>
                  <p className="mt-1 whitespace-pre-wrap text-[var(--text)]">{c.body}</p>
                  <p className="mt-2 text-xs text-[var(--muted)]">
                    {new Date(c.createdAt).toLocaleString()}
                  </p>
                </li>
              ))
            )}
          </ul>
        ) : null}

        {token ? (
          <form
            className="space-y-2"
            onSubmit={(e) => {
              e.preventDefault()
              const t = commentText.trim()
              if (!t || commentMut.isPending) {
                return
              }
              commentMut.mutate(t)
            }}
          >
            <label className="block text-sm font-medium">
              Ваш комментарий
              <textarea
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                rows={3}
                className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-[var(--accent)]"
                maxLength={8000}
              />
            </label>
            <button
              type="submit"
              disabled={commentMut.isPending || !commentText.trim()}
              className="rounded-lg bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--accent-hover)] disabled:opacity-50"
            >
              {commentMut.isPending ? 'Отправка…' : 'Отправить'}
            </button>
            {commentMut.isError ? (
              <p className="text-sm text-red-600">Не удалось отправить комментарий</p>
            ) : null}
          </form>
        ) : (
          <p className="text-sm text-[var(--muted)]">
            <Link className="text-[var(--accent)] hover:underline" to={redirectToLogin}>
              Войдите
            </Link>{' '}
            или{' '}
            <Link className="text-[var(--accent)] hover:underline" to={redirectToRegister}>
              зарегистрируйтесь
            </Link>
            , чтобы комментировать.
          </p>
        )}
      </section>
    </article>
  )
}
