import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import {
  TOKEN_KEY,
  fetchMe,
  login as loginApi,
  register as registerApi,
} from '../../lib/api'
import type { AuthPayload, User } from '../../lib/api'

type AuthContextValue = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (input: {
    email: string
    password: string
    fullName: string
    phone?: string
  }) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setLoading(false)
      return
    }
    fetchMe()
      .then(setUser)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const persist = useCallback((payload: AuthPayload) => {
    localStorage.setItem(TOKEN_KEY, payload.accessToken)
    setUser(payload.user)
    return payload.user
  }, [])

  const login = useCallback(
    async (email: string, password: string) => persist(await loginApi(email, password)),
    [persist],
  )

  const register = useCallback(
    async (input: { email: string; password: string; fullName: string; phone?: string }) =>
      persist(await registerApi(input)),
    [persist],
  )

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phai nam trong <AuthProvider>')
  return ctx
}
