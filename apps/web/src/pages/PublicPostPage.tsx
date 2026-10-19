import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { fetchPublicPost } from '../api/posts'
import { getStoredAccessToken, resolveApiUrl } from '../api/client'
import { fetchPublicEngagement, postPublicComment, togglePostLike } from '../api/publicEngagement'
import { PublicAuthorChip } from '../components/PublicAuthorChip'
import { PublicSiteNav } from '../components/PublicSiteNav'
import { PublicPostOwnerActions } from '../components/PublicPostOwnerActions'
import { Seo } from '../components/Seo'
import { StructuredData } from '../components/StructuredData'
import { initialArticleData } from '../lib/publicBootstrap'

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
    initialData: () => initialArticleData(ws, ps),
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
    return <p className="mx-auto max-w-3xl px-4 py-10 text-sm text-[var(--muted)]" role="status">Загрузка статьи…</p>
  }

  if (q.isError || !q.data) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <Seo title="Материал не найден" canonicalPath={`/blog/${ws}/p/${ps}`} noIndex />
        <p className="text-sm text-[var(--danger)]" role="alert">Материал не найден</p>
        <Link className="mt-4 inline-block text-sm text-[var(--accent)]" to={`/blog/${ws}`}>
          ← к ленте
        </Link>
      </div>
    )
  }

  const post = q.data
  const eng = engQ.data
  const articleUrl = new URL(`/blog/${ws}/p/${post.slug}`, window.location.origin).toString()
  const firstImage = post.media?.find((media) => media.mimeType?.startsWith('image/'))

  return (
    <article className="mx-auto max-w-4xl space-y-7 px-4 py-6 sm:py-8 lg:py-10">
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
        image={firstImage?.url ?? post.authorAvatarUrl}
        type="article"
      />
      <StructuredData value={{
        '@context': 'https://schema.org',
        '@type': 'BlogPosting',
        headline: post.title,
        mainEntityOfPage: articleUrl,
        ...(post.excerpt ? { description: post.excerpt } : {}),
        ...(post.publishedAt ? { datePublished: post.publishedAt } : {}),
        ...(post.updatedAt ? { dateModified: post.updatedAt } : {}),
        ...(post.authorDisplayName ? { author: { '@type': 'Person', name: post.authorDisplayName } } : {}),
        ...(firstImage ? { image: new URL(resolveApiUrl(firstImage.url), window.location.origin).toString() } : {}),
      }} />
      <PublicSiteNav />
      <Link className="inline-block text-sm text-[var(--muted)] hover:text-[var(--accent)]" to={`/blog/${ws}`}>
        ← все материалы
      </Link>
      <header className="mx-auto max-w-[72ch] space-y-4 border-b border-[var(--border)] pb-7">
        {post.category ? <span className="ui-badge">{post.category.name}</span> : null}
        <h1 className="font-editorial break-words text-3xl font-semibold leading-tight tracking-tight sm:text-4xl lg:text-5xl">{post.title}</h1>
        {post.authorDisplayName ? (
          <PublicAuthorChip name={post.authorDisplayName} avatarUrl={post.authorAvatarUrl} />
        ) : null}
        {post.publishedAt ? (
          <time className="block text-xs text-[var(--muted)]" dateTime={post.publishedAt}>
            {new Date(post.publishedAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })}
          </time>
        ) : null}
      </header>
      <div className="mx-auto max-w-[72ch]">
        <PublicPostOwnerActions postId={post.id} authorId={post.authorId} workspaceSlug={ws}
          title={post.title} openEditorAfter />
        {post.excerpt ? <p className="font-editorial mt-5 break-words text-lg leading-relaxed text-[var(--muted)]">{post.excerpt}</p> : null}
      </div>

      <div className="mx-auto flex max-w-[72ch] flex-wrap items-center gap-3 border-b border-[var(--border)] pb-4">
        {token ? (
          <button
            type="button"
            disabled={likeMut.isPending || engQ.isLoading}
            onClick={() => likeMut.mutate()}
            className={`ui-button text-sm ${
              eng?.likedByMe
                ? 'border-[var(--accent)] bg-[var(--accent)]/10 text-[var(--accent)]'
                : 'ui-button--secondary'
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
          className="ui-button ui-button--secondary"
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
        <div className="mx-auto max-w-[72ch] space-y-4">
          {post.media.map((m) =>
            m.mimeType?.startsWith('image/') ? (
              <figure key={m.id} className="space-y-2">
                <img
                  src={resolveApiUrl(m.url)}
                  alt={m.altText ?? ''}
                  loading="lazy"
                  decoding="async"
                  className="max-h-[36rem] w-full rounded-[var(--radius-control)] object-contain"
                />
                {m.caption ? (
                  <figcaption className="text-center text-sm text-[var(--muted)]">{m.caption}</figcaption>
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
        className="article-prose min-w-0"
        dangerouslySetInnerHTML={{ __html: post.bodyHtml ?? '' }}
      />

      <section className="mx-auto max-w-[72ch] space-y-4 border-t border-[var(--border)] pt-7">
        <h2 className="font-editorial text-xl font-semibold">Комментарии</h2>
        {engQ.isLoading ? (
          <p className="text-sm text-[var(--muted)]">Загрузка…</p>
        ) : eng ? (
          <ul className="space-y-3 text-sm">
            {eng.comments.length === 0 ? (
              <li className="text-[var(--muted)]">Пока нет комментариев.</li>
            ) : (
              eng.comments.map((c) => (
                <li key={c.id} className="ui-card p-4">
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
                className="ui-field mt-1 w-full text-sm"
                maxLength={8000}
              />
            </label>
            <button
              type="submit"
              disabled={commentMut.isPending || !commentText.trim()}
              className="ui-button ui-button--accent"
            >
              {commentMut.isPending ? 'Отправка…' : 'Отправить'}
            </button>
            {commentMut.isError ? (
              <p className="text-sm text-[var(--danger)]" role="alert">Не удалось отправить комментарий</p>
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
