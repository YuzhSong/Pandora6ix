import { createApiClient } from './client.ts'
import { createHealthClient } from './health.ts'

// Business repositories will use this client after the DTO and authentication contract is agreed.
export const apiClient = createApiClient({ baseUrl: import.meta.env.VITE_API_BASE_URL || '/api/v1' })
export const healthClient = createHealthClient({ baseUrl: import.meta.env.VITE_BACKEND_BASE_URL || '/backend', timeoutMs: 5000 })
