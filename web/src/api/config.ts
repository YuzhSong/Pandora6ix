import { createApiClient } from './client.ts'

// Business repositories will use this client after the DTO and authentication contract is agreed.
export const apiClient = createApiClient({ baseUrl: import.meta.env.VITE_API_BASE_URL || '/api/v1' })
