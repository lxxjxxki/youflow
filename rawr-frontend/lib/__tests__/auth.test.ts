import { describe, it, expect, beforeEach } from 'vitest'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, val: string) => { store[key] = val },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
})()

Object.defineProperty(global, 'localStorage', { value: localStorageMock })
Object.defineProperty(global, 'window', { value: global })

import { getToken, setToken, clearToken } from '../auth'

describe('auth', () => {
  beforeEach(() => localStorageMock.clear())

  it('getToken returns null when not set', () => {
    expect(getToken()).toBeNull()
  })

  it('setToken stores token', () => {
    setToken('abc123')
    expect(getToken()).toBe('abc123')
  })

  it('clearToken removes token', () => {
    setToken('abc123')
    clearToken()
    expect(getToken()).toBeNull()
  })
})
