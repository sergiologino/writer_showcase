/**
 * Имя в ленте/посте: короткие инициалы для круга-заглушки.
 */
export function publicAuthorInitials(displayName: string | null | undefined): string {
  const t = (displayName ?? '').trim()
  if (!t) {
    return '?'
  }
  const parts = t.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    return (parts[0]![0]! + parts[1]![0]!).toUpperCase()
  }
  return t.slice(0, 2).toUpperCase()
}
