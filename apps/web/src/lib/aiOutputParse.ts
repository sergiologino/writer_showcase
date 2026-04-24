/** Достаёт «человекочитаемый» фрагмент из сырого ответа интеграции (JSON или текст). */
export function extractAssistantText(raw: string | null | undefined): string {
  if (!raw) {
    return ''
  }
  const t = raw.trim()
  if (!t) {
    return ''
  }
  try {
    const j = JSON.parse(t) as unknown
    if (typeof j === 'string') {
      return j.trim()
    }
    if (typeof j === 'object' && j !== null) {
      const o = j as Record<string, unknown>
      const choices = o.choices
      if (Array.isArray(choices) && choices[0] && typeof choices[0] === 'object' && choices[0] !== null) {
        const c0 = choices[0] as Record<string, unknown>
        const msg = c0.message
        if (msg && typeof msg === 'object' && msg !== null) {
          const m = msg as Record<string, unknown>
          if (typeof m.content === 'string') {
            return m.content
          }
        }
      }
      if (typeof o.content === 'string') {
        return o.content
      }
      if (typeof o.text === 'string') {
        return o.text
      }
      if (typeof o.output === 'string') {
        return o.output
      }
    }
  } catch {
    /* не JSON */
  }
  return t
}
