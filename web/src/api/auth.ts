import { ApiError } from './client.ts'
import type { createApiClient } from './client.ts'
import type { createSessionStore } from './session.ts'

export interface LoginInput { accountIdentifier: string; password: string }
export function validatePassword(password: string) {
  if (password.length < 10 || new TextEncoder().encode(password).length > 72) throw new Error('密码至少 10 个字符，UTF-8 编码后不能超过 72 字节。')
}
export function requireAdmin(value: unknown): { id: string } {
  if (!value || typeof value !== 'object' || !('id' in value) || typeof value.id !== 'string' || !('roles' in value) || !Array.isArray(value.roles)) throw new ApiError('本人资料的返回格式不正确。', 200, 'INVALID_RESPONSE')
  if (!value.roles.includes('ADMIN')) throw new ApiError('此账号没有 Web 管理员权限，请使用管理员账号登录。', 403, 'FORBIDDEN')
  return { id: value.id }
}
export function createAuthService(client: ReturnType<typeof createApiClient>, sessions: ReturnType<typeof createSessionStore>) {
  return {
    async login(input: LoginInput) {
      const result = await client.request<{ accessToken: string; tokenType: string; expiresAt: string; user: unknown }>('/auth/login', { method: 'POST', body: input, authenticated: false })
      if (!result || result.tokenType !== 'Bearer' || typeof result.accessToken !== 'string' || typeof result.expiresAt !== 'string') throw new ApiError('登录响应的返回格式不正确。', 200, 'INVALID_RESPONSE')
      try { requireAdmin(result.user) }
      catch (error) {
        await client.request('/auth/logout', { method: 'POST', authenticated: false, headers: { Authorization: `Bearer ${result.accessToken}` } }).catch(() => undefined)
        throw error
      }
      try { sessions.save(result) }
      catch {
        await client.request('/auth/logout', { method: 'POST', authenticated: false, headers: { Authorization: `Bearer ${result.accessToken}` } }).catch(() => undefined)
        throw new Error('无法保存登录会话，请允许本站点使用会话存储后重试。')
      }
    },
    async logout() {
      try { await client.request('/auth/logout', { method: 'POST' }) }
      catch (error) { if (!(error instanceof ApiError && error.status === 401)) throw error }
      sessions.clear()
    },
    async changePassword(currentPassword: string, newPassword: string) {
      validatePassword(newPassword)
      await client.request('/me/password', { method: 'POST', body: { currentPassword, newPassword } })
      sessions.clear()
    },
  }
}
