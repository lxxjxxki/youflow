'use client'

import { useEffect, useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { useUIStore } from '@/store/uiStore'
import { getMyBookmarks } from '@/lib/api'
import type { ArticleResponse } from '@/types'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081'

export default function MyPage() {
  const { user, logout } = useUIStore()
  const [bookmarks, setBookmarks] = useState<ArticleResponse[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!user) return
    setLoading(true)
    getMyBookmarks()
      .then(setBookmarks)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (!user) {
    return (
      <div className="pt-20 flex items-center justify-center min-h-screen px-6">
        <div className="text-center">
          <h1 className="text-white uppercase tracking-[0.3em] text-sm font-bold mb-10">Sign In</h1>
          <div className="flex flex-col gap-4 w-64">
            <a
              href={`${API_BASE}/oauth2/authorization/google`}
              className="block py-3 px-6 bg-white text-black uppercase text-xs font-bold tracking-widest hover:bg-accent transition-colors text-center"
            >
              Continue with Google
            </a>
            <a
              href={`${API_BASE}/oauth2/authorization/kakao`}
              className="block py-3 px-6 bg-[#FEE500] text-black uppercase text-xs font-bold tracking-widest hover:opacity-90 transition-opacity text-center"
            >
              Continue with Kakao
            </a>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="pt-20 max-w-3xl mx-auto px-6 pb-24">
      {/* Profile */}
      <div className="flex items-center gap-6 py-10 border-b border-zinc-800">
        {user.profileImage ? (
          <Image
            src={user.profileImage}
            alt={user.username}
            width={64}
            height={64}
            className="rounded-full object-cover w-16 h-16"
            unoptimized
          />
        ) : (
          <div className="w-16 h-16 rounded-full bg-zinc-800 flex items-center justify-center flex-shrink-0">
            <span className="text-white text-xl font-bold uppercase">{user.username[0]}</span>
          </div>
        )}
        <div className="flex-1">
          <p className="text-white font-bold text-xl">{user.username}</p>
          <p className="text-zinc-500 text-sm mt-1">{user.email}</p>
          <span className="text-accent text-xs uppercase tracking-widest font-bold mt-1 inline-block">
            {user.role}
          </span>
        </div>
        <button
          onClick={logout}
          className="text-zinc-600 text-xs uppercase tracking-widest hover:text-white transition-colors"
        >
          Sign Out
        </button>
      </div>

      {/* Bookmarks */}
      <div className="mt-10">
        <h2 className="text-white uppercase tracking-[0.3em] text-sm font-bold mb-6">Bookmarks</h2>
        {loading ? (
          <p className="text-zinc-600 uppercase tracking-widest text-xs">Loading...</p>
        ) : bookmarks.length === 0 ? (
          <p className="text-zinc-600 uppercase tracking-widest text-xs">No bookmarks yet</p>
        ) : (
          <div className="flex flex-col divide-y divide-zinc-800">
            {bookmarks.map((article) => (
              <Link
                key={article.id}
                href={`/articles/${article.slug}`}
                className="flex gap-4 py-4 hover:opacity-70 transition-opacity"
              >
                {article.coverImage && (
                  <div className="relative w-20 h-20 flex-shrink-0">
                    <Image
                      src={article.coverImage}
                      alt={article.title}
                      fill
                      className="object-cover"
                      unoptimized
                    />
                  </div>
                )}
                <div>
                  <span className="text-accent text-xs uppercase tracking-widest font-bold">
                    {article.category}
                  </span>
                  <p className="text-white text-sm font-bold mt-1 leading-tight">{article.title}</p>
                  <p className="text-zinc-500 text-xs mt-1">{article.authorName}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
