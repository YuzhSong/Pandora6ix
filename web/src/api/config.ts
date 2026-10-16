import { createApiClient } from './client.ts'
import { createHealthClient } from './health.ts'
import { sessionStore } from './session.ts'
import { createAuthService } from './auth.ts'

export const apiClient = createApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  getAccessToken: () => sessionStore.read()?.accessToken ?? null,
  onUnauthorized: () => sessionStore.clear(),
})
export const authService = createAuthService(apiClient, sessionStore)
export const healthClient = createHealthClient({ baseUrl: import.meta.env.VITE_BACKEND_BASE_URL || '/backend', timeoutMs: 5000 })
