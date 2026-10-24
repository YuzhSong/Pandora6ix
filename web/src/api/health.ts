import { ApiError, createApiClient } from './client.ts'

export interface BackendHealth { status: 'UP' }

// Backend liveness uses plain JSON, while business requests keep their envelope.
export function createHealthClient(options: Parameters<typeof createApiClient>[0]) {
  const client = createApiClient(options)
  return {
    async check(signal?: AbortSignal): Promise<BackendHealth> {
      const health = await client.request<unknown>('/health', { signal, responseFormat: 'json' })
      if (!health || typeof health !== 'object' || !('status' in health) || typeof health.status !== 'string') {
        throw new ApiError('后端健康检查返回格式不正确。', 200, 'INVALID_RESPONSE')
      }
      if (health.status !== 'UP') throw new ApiError('后端尚未就绪，请稍后重试。', 200, 'BACKEND_NOT_READY')
      return { status: 'UP' }
    },
  }
}
