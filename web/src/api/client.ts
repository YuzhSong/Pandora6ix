export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly fieldErrors: unknown[]
  constructor(message: string, status: number, code: string, requestId?: string, fieldErrors: unknown[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.requestId = requestId
    this.fieldErrors = fieldErrors
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
  // Only unwrapped infrastructure endpoints (such as /health) opt into JSON.
  responseFormat?: 'envelope' | 'json'
  headers?: Record<string, string>
  authenticated?: boolean
}
interface ClientOptions {
  baseUrl: string
  fetcher?: typeof fetch
  timeoutMs?: number
  onUnauthorized?: () => void
  getAccessToken?: () => string | null
}
const messages: Record<number, string> = {
  401: '登录状态已失效，请重新登录。',
  403: '没有权限执行此操作。',
  404: '请求的资源不存在。',
  409: '数据已被更新，请刷新后重试。',
  422: '提交的信息不符合要求，请检查后重试。',
}

export function createApiClient({ baseUrl, fetcher = fetch, timeoutMs = 15000, onUnauthorized, getAccessToken }: ClientOptions) {
  const base = baseUrl.replace(/\/+$/, '')
  if (!base || (!base.startsWith('/') && !/^https?:\/\//.test(base)) || base.startsWith('//')) throw new Error('API 地址必须是站内路径或 HTTP(S) 地址。')
  return {
    async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
      if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) throw new Error('API 路径必须从 / 开始。')
      const controller = new AbortController()
      let timedOut = false
      const timer = setTimeout(() => { timedOut = true; controller.abort() }, timeoutMs)
      const signal = options.signal ? AbortSignal.any([controller.signal, options.signal]) : controller.signal
      try {
        const headers = new Headers({ Accept: 'application/json', ...options.headers })
        if (options.body !== undefined) headers.set('Content-Type', 'application/json')
        const token = options.authenticated === false ? null : getAccessToken?.()
        if (token) headers.set('Authorization', `Bearer ${token}`)
        const response = await fetcher(`${base}${path}`, {
          method: options.method ?? 'GET',
          headers,
          body: options.body === undefined ? undefined : JSON.stringify(options.body),
          credentials: 'same-origin',
          cache: 'no-store',
          signal,
        })
        if (response.status === 204) return undefined as T
        const raw = await response.text()
        let payload: Record<string, unknown> | null = null
        try {
          const parsed: unknown = JSON.parse(raw)
          if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) payload = parsed as Record<string, unknown>
        } catch { /* Non-JSON errors are reported without exposing server HTML. */ }
        const requestId = typeof payload?.requestId === 'string' ? payload.requestId : response.headers.get('x-request-id') ?? undefined
        if (!response.ok) {
          const nested = payload?.error
          const error = nested && typeof nested === 'object' ? nested as Record<string, unknown> : payload
          if (response.status === 401 && options.authenticated !== false) onUnauthorized?.()
          throw new ApiError(
            typeof error?.message === 'string' ? error.message : messages[response.status] ?? '服务器暂时无法完成请求，请稍后重试。',
            response.status,
            typeof error?.code === 'string' ? error.code : `HTTP_${response.status}`,
            requestId,
            Array.isArray(error?.fieldErrors) ? error.fieldErrors : [],
          )
        }
        if (options.responseFormat === 'json') {
          if (!payload) throw new ApiError('服务器返回的数据格式不符合接口约定。', response.status, 'INVALID_RESPONSE', requestId)
          return payload as T
        }
        if (!payload || !Object.hasOwn(payload, 'data')) throw new ApiError('服务器返回的数据格式不符合接口约定。', response.status, 'INVALID_RESPONSE', requestId)
        return payload.data as T
      } catch (error) {
        if (error instanceof ApiError) throw error
        if (signal.aborted) throw new ApiError(timedOut ? '请求超时，请稍后重试。' : '请求已取消。', 0, timedOut ? 'REQUEST_TIMEOUT' : 'REQUEST_CANCELLED')
        throw new ApiError('无法连接服务器，请检查网络连接。', 0, 'NETWORK_ERROR')
      } finally { clearTimeout(timer) }
    },
  }
}
