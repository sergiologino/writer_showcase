import { useEffect } from 'react'

const SITE_NAME = 'Altacod Publisher'
const DEFAULT_DESCRIPTION =
  'Altacod Publisher помогает блогерам и писателям вести авторский блог, готовить тексты и публиковать их в свои каналы.'
const DEFAULT_KEYWORDS =
  'платформа для блогеров, платформа для писателей, авторский блог, POSSE, публикация в соцсети, редактор текстов, AI для авторов'

type SeoProps = {
  title: string
  description?: string | null
  keywords?: string | null
  canonicalPath?: string
  image?: string | null
  type?: 'website' | 'article'
  noIndex?: boolean
}

function absoluteUrl(pathOrUrl: string): string {
  if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
    return pathOrUrl
  }
  if (typeof window === 'undefined') {
    return pathOrUrl
  }
  return new URL(pathOrUrl, window.location.origin).toString()
}

function ensureMeta(selector: string, attrs: Record<string, string>): HTMLMetaElement {
  let el = document.head.querySelector<HTMLMetaElement>(selector)
  if (!el) {
    el = document.createElement('meta')
    Object.entries(attrs).forEach(([key, value]) => el?.setAttribute(key, value))
    document.head.appendChild(el)
  }
  return el
}

function setMetaName(name: string, content: string) {
  const el = ensureMeta(`meta[name="${name}"]`, { name })
  el.setAttribute('content', content)
}

function setMetaProperty(property: string, content: string) {
  const el = ensureMeta(`meta[property="${property}"]`, { property })
  el.setAttribute('content', content)
}

function setCanonical(url: string) {
  let el = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', 'canonical')
    document.head.appendChild(el)
  }
  el.setAttribute('href', url)
}

export function Seo({
  title,
  description = DEFAULT_DESCRIPTION,
  keywords = DEFAULT_KEYWORDS,
  canonicalPath,
  image,
  type = 'website',
  noIndex = false,
}: SeoProps) {
  useEffect(() => {
    const fullTitle = title.includes(SITE_NAME) ? title : `${title} | ${SITE_NAME}`
    const desc = description?.trim() || DEFAULT_DESCRIPTION
    const kw = keywords?.trim() || DEFAULT_KEYWORDS
    const canonical = absoluteUrl(canonicalPath ?? window.location.pathname)
    const imageUrl = image ? absoluteUrl(image) : absoluteUrl('/favicon.svg')

    document.title = fullTitle
    setMetaName('description', desc)
    setMetaName('keywords', kw)
    setMetaName('robots', noIndex ? 'noindex,nofollow' : 'index,follow,max-image-preview:large')
    setCanonical(canonical)

    setMetaProperty('og:site_name', SITE_NAME)
    setMetaProperty('og:title', fullTitle)
    setMetaProperty('og:description', desc)
    setMetaProperty('og:type', type)
    setMetaProperty('og:url', canonical)
    setMetaProperty('og:image', imageUrl)

    setMetaName('twitter:card', 'summary_large_image')
    setMetaName('twitter:title', fullTitle)
    setMetaName('twitter:description', desc)
    setMetaName('twitter:image', imageUrl)
  }, [canonicalPath, description, image, keywords, noIndex, title, type])

  return null
}
