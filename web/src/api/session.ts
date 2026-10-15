interface Session { accessToken: string; expiresAt: string }
interface Storage { getItem(key: string): string | null; setItem(key: string, value: string): void; removeItem(key: string): void }
export const AUTH_KEY = 'pandora-auth-session-v1'

export function createSessionStore(storage: Storage, now = Date.now) {
  const listeners = new Set<() => void>()
  const notify = () => listeners.forEach(listener => listener())
  const clear = () => { storage.removeItem(AUTH_KEY); notify() }
  return {
    read(): Session | null {
      const raw = storage.getItem(AUTH_KEY)
      if (!raw) return null
      try {
        const value = JSON.parse(raw)
        if (typeof value.accessToken !== 'string' || !value.accessToken || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= now()) {
          clear(); return null
        }
        return { accessToken: value.accessToken, expiresAt: value.expiresAt }
      } catch { clear(); return null }
    },
    save(session: Session) {
      if (!session.accessToken || Date.parse(session.expiresAt) <= now() || !Number.isFinite(Date.parse(session.expiresAt))) throw new Error('登录响应中的会话信息无效。')
      storage.setItem(AUTH_KEY, JSON.stringify({ accessToken: session.accessToken, expiresAt: session.expiresAt }))
      notify()
    },
    clear,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener) } },
  }
}

export const sessionStore = createSessionStore({
  getItem: key => sessionStorage.getItem(key),
  setItem: (key, value) => sessionStorage.setItem(key, value),
  removeItem: key => sessionStorage.removeItem(key),
})
