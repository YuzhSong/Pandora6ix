import type { ProxyOptions } from 'vite'

export function createBackendProxy(target: string): Record<string, ProxyOptions> {
  const url = new URL(target)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) {
    throw new Error('BACKEND_PROXY_TARGET 必须是无凭证、无路径的 HTTP(S) 服务地址。')
  }
  return {
    '^/api/v1(?:/|$)': { target: url.origin, changeOrigin: true },
    '^/backend/health(?:\\?|$)': {
      target: url.origin,
      changeOrigin: true,
      rewrite: path => path.replace(/^\/backend/, ''),
    },
  }
}
