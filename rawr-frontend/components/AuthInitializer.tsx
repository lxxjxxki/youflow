'use client'

import { useEffect } from 'react'
import { getToken } from '@/lib/auth'
import { useUIStore } from '@/store/uiStore'
import { getMe } from '@/lib/api'

export default function AuthInitializer() {
  const { setUser, setToken } = useUIStore()

  useEffect(() => {
    const token = getToken()
    if (!token) return
    setToken(token)
    getMe().then(setUser).catch(() => {})
  }, [setUser, setToken])

  return null
}
