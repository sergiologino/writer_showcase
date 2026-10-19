import { useEffect } from 'react'

/** Adds structured data for a public page and removes it on route change. */
export function StructuredData({ value }: { value: Record<string, unknown> }) {
  const json = JSON.stringify(value).replace(/</g, '\\u003c')
  useEffect(() => {
    const script = document.createElement('script')
    script.type = 'application/ld+json'
    script.textContent = json
    document.head.appendChild(script)
    return () => script.remove()
  }, [json])
  return null
}
