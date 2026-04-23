'use client'

import Link from 'next/link'
import { useUIStore } from '@/store/uiStore'

function HamburgerIcon() {
  return (
    <div className="flex flex-col gap-[5px] cursor-pointer">
      <span className="block w-6 h-[2px] bg-white" />
      <span className="block w-6 h-[2px] bg-white" />
      <span className="block w-6 h-[2px] bg-white" />
    </div>
  )
}

function BookmarkIcon() {
  return (
    <svg aria-hidden="true" focusable="false" width="20" height="24" viewBox="0 0 20 24" fill="none" stroke="white" strokeWidth="1.5">
      <path d="M3 3h14v18l-7-4-7 4V3z" />
    </svg>
  )
}

function CartIcon() {
  return (
    <svg aria-hidden="true" focusable="false" width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="white" strokeWidth="1.5">
      <path d="M1 1h3l2.68 11.39a2 2 0 0 0 2 1.61h7.72a2 2 0 0 0 2-1.61L21 5H6" />
      <circle cx="9" cy="19" r="1.5" fill="white" stroke="none" />
      <circle cx="17" cy="19" r="1.5" fill="white" stroke="none" />
    </svg>
  )
}

function PersonIcon() {
  return (
    <svg aria-hidden="true" focusable="false" width="20" height="22" viewBox="0 0 20 22" fill="none" stroke="white" strokeWidth="1.5">
      <path d="M10 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />
      <path d="M1 21c0-4.97 4.03-9 9-9s9 4.03 9 9" />
    </svg>
  )
}

export default function Header() {
  const toggleSidebar = useUIStore((s) => s.toggleSidebar)

  return (
    <header className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-6 py-4 bg-black">
      <div className="flex-1">
        <button onClick={toggleSidebar} aria-label="Open menu" className="text-white">
          <HamburgerIcon />
        </button>
      </div>

      <Link href="/" className="font-logo italic text-accent text-3xl tracking-wide">
        rawr
      </Link>

      <div className="flex-1 flex items-center justify-end gap-5">
        <Link href="/bookmarks" aria-label="Bookmarks">
          <BookmarkIcon />
        </Link>
        <button aria-label="Cart" disabled className="opacity-50 cursor-not-allowed">
          <CartIcon />
        </button>
        <Link href="/mypage" aria-label="Profile">
          <PersonIcon />
        </Link>
      </div>
    </header>
  )
}
