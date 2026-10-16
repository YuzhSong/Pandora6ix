import type { ProxyOptions } from 'vite'

export function createBackendProxy(target: string, healthPath = '/health'): Record<string, ProxyOptions> {
  const url = new URL(target)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) {
    throw new Error('BACKEND_PROXY_TARGET 必须是无凭证、无路径的 HTTP(S) 服务地址。')
  }
  if (!['/health', '/backend/health'].includes(healthPath)) {
    throw new Error('BACKEND_HEALTH_PATH 必须是 /health（直连后端）或 /backend/health（云端网关）。')
  }
  return {
    '^/api/v1(?:/|$)': { target: url.origin, changeOrigin: true },
    '^/backend/health(?:\\?|$)': {
      target: url.origin,
      changeOrigin: true,
      rewrite: path => path.replace(/^\/backend\/health/, healthPath),
    },
  }
}
