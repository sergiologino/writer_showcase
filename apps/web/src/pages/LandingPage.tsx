import { Link } from 'react-router-dom'
import { PublisherWordmark } from '../components/PublisherWordmark'
import { Seo } from '../components/Seo'
import { StructuredData } from '../components/StructuredData'
import { ThemeToggleButton } from '../components/ThemeToggleButton'

const imageUrl = (id: string, width: number, quality = 78) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=${quality}`

const heroImage = 'photo-1499750310107-5fef28a66643'
const deskImage = 'photo-1516321318423-f06f85e504b3'
const heroPhoto = imageUrl(heroImage, 1200)

const features = [
  {
    title: 'Пишите в одном месте',
    text: 'Черновики, медиа, категории и теги живут в вашем рабочем пространстве, а не теряются между сервисами.',
  },
  {
    title: 'Публикуйте дальше',
    text: 'Материал остаётся первоисточником у вас, а сервис помогает отправлять его в каналы и соцсети.',
  },
  {
    title: 'Ускоряйте редактуру',
    text: 'AI-студия и шаблоны промптов помогают подготовить структуру, варианты и адаптацию текста.',
  },
]

const workflow = ['Идея', 'Черновик', 'Публичный блог', 'Каналы', 'Аналитика']

function FeatureIcon({ index }: { index: number }) {
  const paths = [
    <path key="pen" d="m4 20 4.5-1 11-11a2.1 2.1 0 0 0-3-3l-11 11L4 20Zm10.5-13.5 3 3M4 20h16" />,
    <path key="send" d="m3 11 17-8-6 18-3.5-7.5L3 11Zm7.5 2.5L20 3" />,
    <path key="spark" d="m12 2 2.1 7.9L22 12l-7.9 2.1L12 22l-2.1-7.9L2 12l7.9-2.1L12 2Z" />,
  ]
  return (
    <span className="flex h-10 w-10 items-center justify-center rounded-[var(--radius-control)] bg-[var(--surface-muted)] text-[var(--text)]" aria-hidden>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
        {paths[index]}
      </svg>
    </span>
  )
}

export function LandingPage() {
  return (
    <main className="min-h-screen bg-[var(--bg)] text-[var(--text)]">
      <Seo
        title="Платформа для блогеров и писателей"
        description="Altacod Publisher — спокойное рабочее место для авторов: блог, редактор, медиа, AI-помощник и публикация материалов в свои каналы."
        keywords="платформа для блогеров, платформа для писателей, авторский блог, редактор текстов, публикация в Telegram, POSSE, AI помощник для автора"
        canonicalPath="/"
        image={heroPhoto}
      />
      <StructuredData value={{
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: 'Altacod Publisher',
        url: window.location.origin,
        inLanguage: 'ru',
      }} />

      <header className="border-b border-[var(--border)] bg-[var(--surface)]/80">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6">
          <PublisherWordmark to="/" size="md" />
          <nav className="flex flex-wrap items-center gap-2 text-sm" aria-label="Навигация сайта">
            <a className="hidden rounded-[var(--radius-control)] px-3 py-2 text-[var(--muted)] hover:text-[var(--text)] md:inline-flex" href="#features">Возможности</a>
            <a className="hidden rounded-[var(--radius-control)] px-3 py-2 text-[var(--muted)] hover:text-[var(--text)] md:inline-flex" href="#process">Как это работает</a>
            <ThemeToggleButton />
            <Link className="px-2 py-2 text-[var(--muted)] hover:text-[var(--text)]" to="/login">Войти</Link>
            <Link className="ui-button ui-button--primary" to="/register">Начать</Link>
          </nav>
        </div>
      </header>

      <section className="mx-auto grid max-w-6xl items-center gap-8 px-4 pb-12 pt-10 sm:px-6 sm:pb-16 sm:pt-14 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] lg:gap-12 lg:pb-20 lg:pt-16">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--muted)]">
            Для блогеров, писателей и независимых редакций
          </p>
          <h1 className="font-editorial mt-5 text-[clamp(2.6rem,4.5vw,3.6rem)] font-semibold leading-[1.08] tracking-tight">
            Ваш текст сначала принадлежит вам.
          </h1>
          <p className="mt-6 max-w-[58ch] text-base leading-7 text-[var(--muted)] sm:text-lg sm:leading-8">
            Altacod Publisher помогает вести авторский блог, хранить материалы как первоисточник и спокойно
            распространять их в Telegram, соцсети и другие каналы.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link className="ui-button ui-button--primary min-h-12 px-5" to="/register">
              Создать авторское пространство <span aria-hidden>→</span>
            </Link>
            <Link className="ui-button ui-button--secondary min-h-12 px-5" to="/login">
              У меня уже есть аккаунт
            </Link>
          </div>
        </div>

        <div className="relative min-w-0">
          <img
            src={heroPhoto}
            srcSet={[640, 960, 1200].map((width) => `${imageUrl(heroImage, width)} ${width}w`).join(', ')}
            sizes="(max-width: 1023px) calc(100vw - 2rem), 520px"
            alt="Рабочий стол автора с ноутбуком и заметками"
            className="aspect-[4/3] w-full rounded-[var(--radius-card)] object-cover shadow-[var(--shadow-card)] lg:aspect-[4/5]"
            width={1200}
            height={1500}
            decoding="async"
            fetchPriority="high"
          />
          <div className="absolute right-4 top-4 max-w-[11rem] rounded-[var(--radius-control)] border border-[var(--border)] bg-[var(--surface)]/95 p-3 text-xs leading-relaxed shadow-[var(--shadow-popover)] backdrop-blur-sm sm:right-6 sm:top-6">
            <p className="font-semibold">Свой блог</p>
            <p className="mt-1 text-[var(--muted)]">Ваш материал остаётся первоисточником.</p>
          </div>
        </div>
      </section>

      <section id="features" className="border-y border-[var(--border)] bg-[var(--surface-muted)]/55" aria-label="Возможности Publisher">
        <div className="mx-auto grid max-w-6xl gap-4 px-4 py-12 sm:px-6 sm:py-16 md:grid-cols-3">
          {features.map((feature, index) => (
            <article key={feature.title} className="ui-card min-w-0 p-6">
              <FeatureIcon index={index} />
              <h2 className="font-editorial mt-5 text-xl font-semibold leading-snug">{feature.title}</h2>
              <p className="mt-3 text-sm leading-6 text-[var(--muted)]">{feature.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="process" className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid items-center gap-8 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] lg:gap-12">
          <img
            src={imageUrl(deskImage, 960, 76)}
            srcSet={[480, 720, 960].map((width) => `${imageUrl(deskImage, width, 76)} ${width}w`).join(', ')}
            sizes="(max-width: 1023px) calc(100vw - 2rem), 480px"
            alt="Минималистичный рабочий стол с ноутбуком"
            className="aspect-[5/4] w-full rounded-[var(--radius-card)] object-cover shadow-[var(--shadow-card)]"
            width={1000}
            height={800}
            loading="lazy"
            decoding="async"
          />
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--muted)]">Спокойный процесс</p>
            <h2 className="font-editorial mt-4 max-w-[17ch] text-3xl font-semibold leading-tight tracking-tight sm:text-4xl">От заметки до публичного архива</h2>
            <p className="mt-5 max-w-[60ch] text-base leading-8 text-[var(--muted)]">
              Публикуйте без ощущения, что алгоритмы забирают у вас контроль: собственный блог становится центром,
              а внешние площадки — каналами доставки.
            </p>
          </div>
        </div>
        <ol className="mt-8 grid gap-3 sm:grid-cols-2 lg:mt-10 lg:grid-cols-5">
          {workflow.map((step, index) => (
            <li key={step} className="ui-card min-w-0 p-4">
              <span className="text-xs font-medium text-[var(--muted)]">0{index + 1}</span>
              <p className="mt-2 text-sm font-semibold">{step}</p>
            </li>
          ))}
        </ol>
      </section>
    </main>
  )
}
