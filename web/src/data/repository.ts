import { apiClient } from '../api/config.ts'
import { sessionStore } from '../api/session.ts'
import { createHttpAdminRepository } from './httpAdminRepository.ts'

export const adminRepository = createHttpAdminRepository(apiClient)
sessionStore.subscribe(() => { if (!sessionStore.read()) adminRepository.clear() })
