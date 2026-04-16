import { create } from 'zustand'
import type { User } from '@/types'

interface UIStore {
  sidebarOpen: boolean
  user: User | null
  token: string | null
  openSidebar: () => void
  closeSidebar: () => void
  toggleSidebar: () => void
  setUser: (user: User | null) => void
  setToken: (token: string | null) => void
  logout: () => void
}

export const useUIStore = create<UIStore>((set) => ({
  sidebarOpen: false,
  user: null,
  token: null,
  openSidebar: () => set({ sidebarOpen: true }),
  closeSidebar: () => set({ sidebarOpen: false }),
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  setUser: (user) => set({ user }),
  setToken: (token) => set({ token }),
  logout: () => {
    if (typeof window !== 'undefined') localStorage.removeItem('rawr_token')
    set({ user: null, token: null })
  },
}))
