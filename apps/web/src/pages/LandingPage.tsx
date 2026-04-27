import { Link } from 'react-router-dom'
import { PublisherWordmark } from '../components/PublisherWordmark'
import { Seo } from '../components/Seo'

const heroPhoto =
  'https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=1400&q=85'
const deskPhoto =
  'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1000&q=85'

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

export function LandingPage() {
  return (
    <main className="min-h-screen bg-[#f7f3ec] text-zinc-950">
      <Seo
        title="Платформа для блогеров и писателей"
        description="Altacod Publisher — спокойное рабочее место для авторов: блог, редактор, медиа, AI-помощник и публикация материалов в свои каналы."
        keywords="платформа для блогеров, платформа для писателей, авторский блог, редактор текстов, публикация в Telegram, POSSE, AI помощник для автора"
        canonicalPath="/"
        image={heroPhoto}
      />

      <header className="mx-auto flex max-w-6xl items-center justify-between px-5 py-6">
        <PublisherWordmark to="/" size="md" />
        <nav className="flex items-center gap-4 text-sm">
          <Link className="text-zinc-600 hover:text-zinc-950" to="/login">
            Войти
          </Link>
          <Link
            className="rounded-full border border-zinc-900/15 bg-white/70 px-4 py-2 font-medium shadow-sm hover:bg-white"
            to="/register"
          >
            Начать
          </Link>
        </nav>
      </header>

      <section className="mx-auto grid max-w-6xl gap-10 px-5 pb-20 pt-10 lg:grid-cols-[1.02fr_0.98fr] lg:items-center">
        <div>
          <p className="mb-4 text-sm font-medium uppercase tracking-[0.24em] text-zinc-500">
            Для блогеров, писателей и независимых редакций
          </p>
          <h1 className="font-brand text-5xl font-semibold leading-[0.98] tracking-tight sm:text-6xl lg:text-7xl">
            Ваш текст сначала принадлежит вам.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-8 text-zinc-700">
            Altacod Publisher помогает вести авторский блог, хранить материалы как первоисточник и спокойно
            распространять их в Telegram, соцсети и другие каналы.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              className="rounded-full bg-zinc-950 px-6 py-3 text-sm font-semibold text-white shadow-sm hover:bg-zinc-800"
              to="/register"
            >
              Создать авторское пространство
            </Link>
            <Link
              className="rounded-full border border-zinc-900/15 bg-white/60 px-6 py-3 text-sm font-semibold hover:bg-white"
              to="/login"
            >
              У меня уже есть аккаунт
            </Link>
          </div>
        </div>

        <div className="relative">
          <div className="absolute -left-6 top-10 hidden rounded-2xl bg-white/80 p-4 text-sm shadow-xl ring-1 ring-zinc-900/10 sm:block">
            <p className="font-medium">Сегодня</p>
            <p className="text-zinc-600">3 черновика, 1 публикация, 4 канала</p>
          </div>
          <img
            src={heroPhoto}
            alt="Рабочий стол автора с ноутбуком и заметками"
            className="aspect-[4/5] w-full rounded-[2rem] object-cover shadow-2xl ring-1 ring-zinc-900/10"
            width={700}
            height={875}
            fetchPriority="high"
          />
        </div>
      </section>

      <section className="border-y border-zinc-900/10 bg-white/65">
        <div className="mx-auto grid max-w-6xl gap-8 px-5 py-14 md:grid-cols-3">
          {features.map((feature) => (
            <article key={feature.title} className="rounded-3xl border border-zinc-900/10 bg-white p-6 shadow-sm">
              <h2 className="font-brand text-2xl font-semibold">{feature.title}</h2>
              <p className="mt-3 text-sm leading-6 text-zinc-600">{feature.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-10 px-5 py-20 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
        <img
          src={deskPhoto}
          alt="Минималистичный рабочий стол с ноутбуком"
          className="aspect-[5/4] w-full rounded-[2rem] object-cover shadow-xl ring-1 ring-zinc-900/10"
          width={900}
          height={720}
          loading="lazy"
        />
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.24em] text-zinc-500">Спокойный процесс</p>
          <h2 className="mt-3 font-brand text-4xl font-semibold tracking-tight">От заметки до публичного архива</h2>
          <p className="mt-5 text-base leading-8 text-zinc-700">
            Публикуйте без ощущения, что алгоритмы забирают у вас контроль: собственный блог становится центром,
            а внешние площадки — каналами доставки.
          </p>
          <ol className="mt-8 grid gap-3 sm:grid-cols-5">
            {workflow.map((step, index) => (
              <li key={step} className="rounded-2xl border border-zinc-900/10 bg-white/70 p-4">
                <span className="text-xs text-zinc-500">0{index + 1}</span>
                <p className="mt-1 text-sm font-medium">{step}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>
    </main>
  )
}
