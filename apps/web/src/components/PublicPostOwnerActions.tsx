import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { fetchMe } from '../api/account'
import { getStoredAccessToken } from '../api/client'
import { unpublishPost } from '../api/posts'
import { loadLocalPost, localPostKey, saveLocalPost } from '../lib/localPosts'

export function PublicPostOwnerActions({ postId, authorId, workspaceSlug, title, openEditorAfter = false }: {
  postId: number
  authorId: number
  workspaceSlug: string
  title: string
  openEditorAfter?: boolean
}) {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe, enabled: !!getStoredAccessToken(), retry: false })
  const workspace = me.data?.workspaces.find((item) => item.slug === workspaceSlug)
  const openEditor = () => {
    if (!workspace) return
    if (localStorage.getItem('workspaceId') !== String(workspace.id)) {
      localStorage.setItem('workspaceId', String(workspace.id))
      qc.removeQueries({ queryKey: ['post', String(postId)] })
      qc.removeQueries({ queryKey: ['posts'] })
      qc.removeQueries({ queryKey: ['categories'] })
      qc.removeQueries({ queryKey: ['channels'] })
    }
    navigate(`/app/posts/${postId}`)
  }
  const unpublish = useMutation({
    mutationFn: async () => {
      if (!workspace) throw new Error('Рабочая область не найдена.')
      return unpublishPost(postId, workspace.id)
    },
    onSuccess: async () => {
      if (workspace) {
        try {
          const key = localPostKey(String(workspace.id), postId)
          const local = await loadLocalPost(key)
          if (local) {
            await saveLocalPost({
              ...local,
              savedAt: new Date().toISOString(),
              payload: { ...local.payload, status: 'DRAFT', scheduledPublishAt: null },
              publication: {
                ...(local.publication ?? { channels: [] }), publishedAt: null, publicUrl: null,
              },
            })
          }
        } catch {
          window.alert('Пост снят с публикации, но локальная копия не обновилась. Откройте редактор и восстановите доступ к папке.')
        }
      }
      if (openEditorAfter) openEditor()
      await Promise.all([
        qc.invalidateQueries({ queryKey: ['public-posts', workspaceSlug] }),
        qc.invalidateQueries({ queryKey: ['public-post', workspaceSlug] }),
        qc.invalidateQueries({ queryKey: ['posts'] }),
        qc.invalidateQueries({ queryKey: ['post', String(postId)] }),
      ])
    },
  })

  if (!workspace || me.data?.user.id !== authorId) return null

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3 text-sm">
        <Link className="text-[var(--accent)] hover:underline" to={`/app/posts/${postId}`}
          onClick={(event) => { event.preventDefault(); openEditor() }}>Редактировать</Link>
        <button type="button" disabled={unpublish.isPending}
          className="text-red-700 hover:underline disabled:opacity-50 dark:text-red-400"
          onClick={() => {
            if (window.confirm(`Снять «${title}» с публикации в блоге? Материал останется черновиком в ленте. Уже отправленные посты в соцсетях сохранятся.`)) {
              unpublish.mutate()
            }
          }}>
          {unpublish.isPending ? 'Снимаем…' : 'Снять с публикации'}
        </button>
      </div>
      {unpublish.isError ? <p role="alert" className="text-xs text-red-600">Не удалось снять материал с публикации.</p> : null}
    </div>
  )
}
