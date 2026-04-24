import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  addProfileGalleryPhoto,
  removeProfileGalleryPhoto,
  setProfileAvatar,
  updateProfile,
} from '../api/account'
import { ApiError, apiFetch, resolveApiUrl } from '../api/client'
import { uploadMedia } from '../api/media'
import type { MeResponse, UpdateProfilePayload } from '../api/types'
import { AiAdminSection } from '../components/AiAdminSection'
import { applyTheme, type Theme } from '../lib/theme'

function isTheme(v: string | null | undefined): v is Theme {
  return v === 'light' || v === 'dark' || v === 'system'
}

function ProfilePhotosSection({ me }: { me: MeResponse }) {
  const queryClient = useQueryClient()
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const u = me.user
  const avatarUrl = u.avatarUrl ?? null
  const photos = u.profilePhotos ?? []
  const workspaceId = localStorage.getItem('workspaceId')

  const refreshMe = () => void queryClient.invalidateQueries({ queryKey: ['me'] })

  const onFile = async (file: File | null) => {
    if (!file) {
      return
    }
    if (!file.type.startsWith('image/')) {
      setErr('Выберите файл изображения (JPEG, PNG, WebP…).')
      return
    }
    if (!workspaceId) {
      setErr('Сначала откройте ленту и выберите рабочую область, чтобы прикреплять X-Workspace-Id.')
      return
    }
    setErr(null)
    setBusy(true)
    try {
      const asset = await uploadMedia(file)
      await addProfileGalleryPhoto(asset.id)
      refreshMe()
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Не удалось загрузить фото')
    } finally {
      setBusy(false)
      if (fileRef.current) {
        fileRef.current.value = ''
      }
    }
  }

  const setAvatar = async (mediaAssetId: number) => {
    if (!workspaceId) {
      return
    }
    setErr(null)
    setBusy(true)
    try {
      await setProfileAvatar(mediaAssetId)
      refreshMe()
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Не удалось сменить аватар')
    } finally {
      setBusy(false)
    }
  }

  const clearAvatar = async () => {
    if (!workspaceId) {
      return
    }
    setErr(null)
    setBusy(true)
    try {
      await setProfileAvatar(null)
      refreshMe()
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Не удалось сбросить аватар')
    } finally {
      setBusy(false)
    }
  }

  const remove = async (mediaAssetId: number) => {
    if (!workspaceId) {
      return
    }
    setErr(null)
    setBusy(true)
    try {
      await removeProfileGalleryPhoto(mediaAssetId)
      refreshMe()
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Не удалось удалить фото')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="max-w-lg space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
      <div>
        <h2 className="text-lg font-medium">Фото и аватар</h2>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Фотографии хранятся в профиле. Аватар показывается в шапке и в публичной ленте. Позже появится личная
          страница автора.
        </p>
        {!workspaceId ? (
          <p className="mt-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100">
            Чтобы загружать фото, выберите рабочую область: откройте ленту или материал в этом воркспейсе.
          </p>
        ) : null}
      </div>

      {err ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
          {err}
        </p>
      ) : null}

      <div className="flex flex-wrap items-center gap-4">
        <div className="h-20 w-20 overflow-hidden rounded-full border border-[var(--border)] bg-[var(--bg)]">
          {avatarUrl ? (
            <img src={resolveApiUrl(avatarUrl)} alt="" className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-xs text-[var(--muted)]">нет</div>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={(e) => void onFile(e.target.files?.[0] ?? null)} />
          <button
            type="button"
            disabled={busy || !workspaceId}
            onClick={() => fileRef.current?.click()}
            className="rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm font-medium text-[var(--text)] transition hover:border-[var(--accent)] disabled:opacity-50"
          >
            {busy ? 'Загрузка…' : 'Добавить в галерею'}
          </button>
          {avatarUrl ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => void clearAvatar()}
              className="text-left text-sm text-[var(--muted)] underline decoration-dotted hover:text-[var(--text)]"
            >
              Сбросить аватар
            </button>
          ) : null}
        </div>
      </div>

      {photos.length > 0 ? (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {photos.map((p) => {
            const isAvatar = avatarUrl != null && p.url === avatarUrl
            return (
              <li key={p.mediaAssetId} className="space-y-2 rounded-lg border border-[var(--border)] bg-[var(--bg)] p-2">
                <div className="relative aspect-square overflow-hidden rounded-md">
                  <img
                    src={resolveApiUrl(p.url)}
                    alt=""
                    className="h-full w-full object-cover"
                  />
                  {isAvatar ? (
                    <span className="absolute bottom-1 left-1 rounded bg-[var(--accent)] px-1.5 py-0.5 text-[10px] font-medium text-white">
                      Аватар
                    </span>
                  ) : null}
                </div>
                <div className="flex flex-col gap-1">
                  <button
                    type="button"
                    disabled={busy || isAvatar}
                    onClick={() => void setAvatar(p.mediaAssetId)}
                    className="rounded border border-[var(--border)] px-2 py-1 text-xs text-[var(--text)] hover:bg-[var(--surface)] disabled:opacity-50"
                  >
                    Сделать аватаром
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => {
                      if (window.confirm('Удалить фото из профиля?')) {
                        void remove(p.mediaAssetId)
                      }
                    }}
                    className="text-xs text-red-600 hover:underline dark:text-red-400"
                  >
                    Удалить
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      ) : (
        <p className="text-sm text-[var(--muted)]">Пока нет фотографий в галерее.</p>
      )}
    </div>
  )
}

export function ProfilePage() {
  const queryClient = useQueryClient()
  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => apiFetch<MeResponse>('/api/me'),
  })

  const [displayName, setDisplayName] = useState('')
  const [locale, setLocale] = useState('')
  const [timezone, setTimezone] = useState('')
  const [theme, setTheme] = useState<'light' | 'dark' | 'system' | ''>('')

  useEffect(() => {
    if (!me.data) {
      return
    }
    const u = me.data.user
    setDisplayName(u.displayName)
    setLocale(u.locale ?? '')
    setTimezone(u.timezone ?? '')
    setTheme(isTheme(u.theme) ? u.theme : '')
  }, [me.data])

  const mutation = useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateProfile(payload),
    onSuccess: (data) => {
      queryClient.setQueryData(['me'], data)
      const t = data.user.theme
      if (isTheme(t)) {
        applyTheme(t)
      }
    },
  })

  const error =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error ? 'Не удалось сохранить' : null

  if (me.isLoading) {
    return <p className="text-sm text-[var(--muted)]">Загрузка профиля…</p>
  }
  if (me.isError) {
    return (
      <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
        Не удалось загрузить профиль
      </p>
    )
  }

  const profile = me.data
  if (!profile) {
    return null
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Профиль</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">Имя, локаль и отображение. Email меняется отдельно (пока недоступно).</p>
        <p className="mt-3">
          <Link
            className="rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-sm font-medium text-[var(--text)] hover:border-[var(--accent)]"
            to="/app/channels"
          >
            Каналы публикации (ВК, ОК, Telegram) — инструкции и поля
          </Link>
        </p>
      </div>

      <ProfilePhotosSection me={profile} />

      <form
        className="max-w-lg space-y-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm"
        onSubmit={(ev) => {
          ev.preventDefault()
          const payload: UpdateProfilePayload = {
            displayName: displayName.trim(),
            locale: locale.trim() || null,
            timezone: timezone.trim() || null,
            theme: theme === '' ? null : theme,
          }
          if (!payload.displayName) {
            return
          }
          mutation.mutate(payload)
        }}
      >
        {error ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200">
            {error}
          </p>
        ) : null}
        {mutation.isSuccess ? (
          <p className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-100">
            Сохранено
          </p>
        ) : null}

        <label className="block text-sm font-medium">
          Email
          <input
            type="email"
            readOnly
            className="mt-1 w-full cursor-not-allowed rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--muted)]"
            value={profile.user.email}
          />
        </label>

        <label className="block text-sm font-medium">
          Отображаемое имя
          <input
            type="text"
            required
            maxLength={255}
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Локаль (например ru, en-US)
          <input
            type="text"
            maxLength={32}
            placeholder="ru"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={locale}
            onChange={(e) => setLocale(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Часовой пояс (IANA, например Europe/Moscow)
          <input
            type="text"
            maxLength={64}
            placeholder="Europe/Moscow"
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
          />
        </label>

        <label className="block text-sm font-medium">
          Тема интерфейса в аккаунте
          <select
            className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)] outline-none focus:ring-2 focus:ring-[var(--accent)]"
            value={theme}
            onChange={(e) => setTheme(e.target.value as typeof theme)}
          >
            <option value="">Не задано (как в шапке)</option>
            <option value="system">Как в системе</option>
            <option value="light">Светлая</option>
            <option value="dark">Тёмная</option>
          </select>
        </label>
        <p className="text-xs text-[var(--muted)]">
          Если задано, при сохранении тема применяется сразу. Переключатель «Тема» в шапке по-прежнему хранится локально в браузере.
        </p>

        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-lg bg-[var(--accent)] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-[var(--accent-hover)] disabled:opacity-60"
        >
          {mutation.isPending ? 'Сохранение…' : 'Сохранить'}
        </button>
      </form>

      {profile.user.isAdmin ? <AiAdminSection /> : null}
    </div>
  )
}
