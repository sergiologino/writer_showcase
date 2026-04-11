import { marked } from 'marked'

marked.setOptions({ gfm: true, breaks: true })

/** Текст статьи (Markdown) → HTML для публичного блога. */
export function articleSourceToHtml(source: string): string {
  const s = source?.trim() ?? ''
  if (!s) {
    return ''
  }
  const out = marked.parse(s, { async: false })
  return typeof out === 'string' ? out : ''
}
