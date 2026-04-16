'use client'

import Link from 'next/link'
import { useUIStore } from '@/store/uiStore'

const NAV_ITEMS = [
  { label: 'MUSIC', href: '/music' },
  { label: 'FASHION', href: '/fashion' },
  { label: 'ART', href: '/art' },
  { label: 'ETC', href: '/etc' },
  { label: 'ABOUT', href: '/about' },
]

export default function Sidebar() {
  const { sidebarOpen, closeSidebar } = useUIStore()

  return (
    <>
      {/* Backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/40"
          onClick={closeSidebar}
        />
      )}

      {/* Drawer */}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-[260px] bg-white text-black
          transform transition-transform duration-300 ease-in-out flex flex-col
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        {/* Close button */}
        <div className="flex justify-end p-4">
          <button onClick={closeSidebar} aria-label="Close menu" className="text-black text-xl font-bold">
            ✕
          </button>
        </div>

        {/* Search */}
        <div className="px-5 mb-6">
          <div className="flex items-center border border-black">
            <input
              type="text"
              placeholder="Search"
              className="flex-1 px-3 py-2 text-sm outline-none bg-transparent"
            />
            <button className="px-3 py-2" aria-label="Search">
              <svg aria-hidden="true" focusable="false" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
            </button>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-5">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.label}
              href={item.href}
              onClick={closeSidebar}
              className="block text-3xl font-bold tracking-tight py-2 hover:text-accent transition-colors"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        {/* Instagram */}
        <div className="px-5 pb-8">
          <a
            href="https://www.instagram.com/rawr.co.kr/"
            target="_blank"
            rel="noopener noreferrer"
            className="font-logo italic text-2xl text-black hover:opacity-70 transition-opacity"
          >
            Instagram
          </a>
        </div>
      </aside>
    </>
  )
}
