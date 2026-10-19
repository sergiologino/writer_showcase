import { useEffect, useState } from 'react'
import {
  changePostFolder, choosePostFolder, ensurePostFolderWriteAccess, folderPickerAvailable, postFolderStatus,
} from '../lib/localPosts'

export function LocalFolderSettings() {
  const [folder, setFolder] = useState<{ name: string | null; permission: PermissionState | null }>({ name: null, permission: null })
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => { void postFolderStatus().then(setFolder).catch(() => setError('Не удалось прочитать настройку папки.')) }, [])

  const perform = async (action: () => Promise<string>) => {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      setMessage(await action())
      setFolder(await postFolderStatus())
    } catch (reason) {
      if (!(reason instanceof DOMException && reason.name === 'AbortError')) {
        setError(reason instanceof Error ? reason.message : 'Не удалось изменить папку.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="space-y-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Папка материалов</h2>
      <p className="text-sm text-[var(--muted)]">
        Здесь сохраняются текст и медиа всех материалов: выбранная папка / publisher / ID материала.
        Выбор запоминается в этом браузере. После его перезапуска браузер может попросить подтвердить доступ снова.
      </p>
      <p className="text-sm">{folder.name ? `Текущая папка: ${folder.name}/publisher` : 'Папка пока не выбрана'}</p>
      <div className="flex flex-wrap gap-2">
        {!folder.name ? (
          <button type="button" disabled={busy || !folderPickerAvailable()}
            className="rounded-lg bg-[var(--accent)] px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
            onClick={() => void perform(async () => `Выбрана папка ${await choosePostFolder()}/publisher.`)}>
            Выбрать папку
          </button>
        ) : (
          <>
            {folder.permission !== 'granted' ? (
              <button type="button" disabled={busy}
                className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm disabled:opacity-50"
                onClick={() => void perform(async () => `Доступ к ${await ensurePostFolderWriteAccess()}/publisher восстановлен.`)}>
                Восстановить доступ
              </button>
            ) : null}
            <button type="button" disabled={busy || !folderPickerAvailable()}
              className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm disabled:opacity-50"
              onClick={() => void perform(async () => {
                const result = await changePostFolder()
                return result.previousRetained
                  ? `Новая папка ${result.name}/publisher подключена. Старую копию браузер не смог удалить — проверьте её вручную.`
                  : `Материалы перенесены в ${result.name}/publisher.`
              })}>
              Сменить папку и перенести материалы
            </button>
          </>
        )}
      </div>
      {folder.name && folder.permission !== 'granted' ? (
        <p className="text-xs text-amber-700">Перед записью требуется восстановить доступ к сохранённой папке.</p>
      ) : null}
      {message ? <p role="status" className="text-sm text-emerald-700">{message}</p> : null}
      {error ? <p role="alert" className="text-sm text-red-600">{error}</p> : null}
    </section>
  )
}
