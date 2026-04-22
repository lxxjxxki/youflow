import { notFound } from 'next/navigation'
import ArticleGrid from '@/components/ArticleGrid'
import { getArticles } from '@/lib/api'
import type { ArticleResponse } from '@/types'

// Categories connected to backend
const BACKEND_CATEGORIES: Record<string, string> = {
  fashion: 'FASHION',
  music: 'MUSIC',
  art: 'ART',
  etc: 'ETC',
}

export const revalidate = 60

// All valid routes
const VALID_CATEGORIES = ['music', 'fashion', 'art', 'etc']

interface Props {
  params: { category: string }
}

export function generateStaticParams() {
  return VALID_CATEGORIES.map((category) => ({ category }))
}

export default async function CategoryPage({ params }: Props) {
  const slug = params.category.toLowerCase()

  if (!VALID_CATEGORIES.includes(slug)) notFound()

  let articles: ArticleResponse[] = []

  if (BACKEND_CATEGORIES[slug]) {
    try {
      const data = await getArticles(BACKEND_CATEGORIES[slug], 0, 24)
      articles = data.content
    } catch {
      articles = []
    }
  }

  return (
    <div className="pt-20">
      {/* Category title */}
      <h1 className="text-center text-white uppercase tracking-[0.3em] text-lg font-bold py-8 border-b border-zinc-800">
        {slug.toUpperCase()}
      </h1>

      <ArticleGrid articles={articles} />
    </div>
  )
}
