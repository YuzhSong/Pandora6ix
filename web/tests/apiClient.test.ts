import assert from 'node:assert/strict'
import test from 'node:test'

test('timeout and explicit cancellation produce distinct failures', async () => {
  const fetcher: typeof fetch = async (_input, options) => new Promise((_resolve, reject) => {
    const signal = options!.signal!
    if (signal.aborted) reject(new Error('aborted'))
    else signal.addEventListener('abort', () => reject(new Error('aborted')), { once: true })
  })
  const client = createApiClient({ baseUrl: '/api/v1', fetcher, timeoutMs: 10 })
  await assert.rejects(client.request('/me'), (error: unknown) => error instanceof ApiError && error.code === 'REQUEST_TIMEOUT')
  const controller = new AbortController()
  controller.abort()
  await assert.rejects(client.request('/me', { signal: controller.signal }), (error: unknown) => error instanceof ApiError && error.code === 'REQUEST_CANCELLED')
})
import { ApiError, createApiClient } from '../src/api/client.ts'

test('unwraps data and serializes JSON without persisting credentials', async () => {
  const client = createApiClient({ baseUrl: '/api/v1/', fetcher: async (url, init) => {
    assert.equal(url, '/api/v1/tags')
    assert.equal(init?.body, '{"name":"标签"}')
    assert.equal(init?.credentials, 'same-origin')
    return new Response(JSON.stringify({ data: { id: '1' }, requestId: 'ok' }), { status: 201 })
  } })
  assert.deepEqual(await client.request('/tags', { method: 'POST', body: { name: '标签' } }), { id: '1' })
})

test('supports empty logout responses', async () => {
  const client = createApiClient({ baseUrl: '/api/v1', fetcher: async () => new Response(null, { status: 204 }) })
  assert.equal(await client.request('/auth/logout', { method: 'POST' }), undefined)
})

test('maps planned nested conflicts and current flat backend errors with request IDs', async () => {
  for (const payload of [{ error: { code: 'VERSION_CONFLICT', message: '版本冲突' }, requestId: 'nested' }, { code: 'VERSION_CONFLICT', message: '版本冲突', requestId: 'flat' }]) {
    const client = createApiClient({ baseUrl: '/api/v1', fetcher: async () => new Response(JSON.stringify(payload), { status: 409 }) })
    await assert.rejects(client.request('/tags/1'), (error: unknown) => error instanceof ApiError && error.status === 409 && error.code === 'VERSION_CONFLICT' && error.requestId === payload.requestId)
  }
})

test('401 signals session expiry and returns failure', async () => {
  let expired = 0
  const client = createApiClient({ baseUrl: '/api/v1', onUnauthorized: () => { expired++ }, fetcher: async () => new Response('{}', { status: 401 }) })
  await assert.rejects(client.request('/me'), (error: unknown) => error instanceof ApiError && error.status === 401)
  assert.equal(expired, 1)
})

test('does not render HTML error bodies and preserves header request ID', async () => {
  const client = createApiClient({ baseUrl: '/api/v1', fetcher: async () => new Response('<h1>internal stack trace</h1>', { status: 503, headers: { 'x-request-id': 'proxy-id' } }) })
  await assert.rejects(client.request('/me'), (error: unknown) => error instanceof ApiError && !error.message.includes('stack trace') && error.requestId === 'proxy-id')
})

test('network failure and unexpected successful response are not reported as success', async () => {
  const offline = createApiClient({ baseUrl: '/api/v1', fetcher: async () => { throw new TypeError('network') } })
  await assert.rejects(offline.request('/me'), (error: unknown) => error instanceof ApiError && error.code === 'NETWORK_ERROR')
  const invalid = createApiClient({ baseUrl: '/api/v1', fetcher: async () => new Response('<html>login</html>', { status: 200 }) })
  await assert.rejects(invalid.request('/me'), (error: unknown) => error instanceof ApiError && error.code === 'INVALID_RESPONSE')
})
