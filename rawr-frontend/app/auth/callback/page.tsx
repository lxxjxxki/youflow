'use client'

import { useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { setToken } from '@/lib/auth'
import { useUIStore } from '@/store/uiStore'
import { getMe } from '@/lib/api'

function CallbackInner() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { setToken: storeToken, setUser } = useUIStore()

  useEffect(() => {
    const token = searchParams.get('token')
    if (!token) {
      router.replace('/')
      return
    }
    setToken(token)
    storeToken(token)
    getMe()
      .then((user) => {
        setUser(user)
        router.replace('/')
      })
      .catch(() => router.replace('/'))
  }, [searchParams, router, storeToken, setUser])

  return (
    <div className="flex items-center justify-center min-h-screen">
      <p className="text-zinc-500 uppercase tracking-widest text-sm">Signing in...</p>
    </div>
  )
}

export default function AuthCallbackPage() {
  return (
    <Suspense>
      <CallbackInner />
    </Suspense>
  )
}
