import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, createApiClient } from '../src/api/client.ts'
import { createHealthClient } from '../src/api/health.ts'
import { createBackendProxy } from '../dev/backendProxy.ts'

test('health accepts the actual Spring plain JSON response without relaxing business envelopes', async () => {
  const fetcher: typeof fetch = async (url, init) => {
    assert.equal(url, '/backend/health')
    assert.equal(init?.method, 'GET')
    return new Response('{"status":"UP"}')
  }
  assert.deepEqual(await createHealthClient({ baseUrl: '/backend', fetcher }).check(), { status: 'UP' })
  await assert.rejects(createApiClient({ baseUrl: '/backend', fetcher }).request('/health'),
    (error: unknown) => error instanceof ApiError && error.code === 'INVALID_RESPONSE')
})

test('health rejects HTML fallback, missing status, nested business data and non-UP status', async () => {
  for (const [body, code] of [
    ['<html>Vite fallback</html>', 'INVALID_RESPONSE'],
    ['{}', 'INVALID_RESPONSE'],
    ['{"data":{"status":"UP"}}', 'INVALID_RESPONSE'],
    ['{"status":"DOWN"}', 'BACKEND_NOT_READY'],
    ['{"status":1}', 'INVALID_RESPONSE'],
  ]) {
    const client = createHealthClient({ baseUrl: '/backend', fetcher: async () => new Response(body) })
    await assert.rejects(client.check(), (error: unknown) => error instanceof ApiError && error.code === code)
  }
})

test('health retains backend HTTP error and request ID instead of claiming connectivity', async () => {
  const client = createHealthClient({ baseUrl: '/backend', fetcher: async () => new Response(
    '{"code":"RESOURCE_NOT_FOUND","message":"Resource was not found.","requestId":"health-404"}', { status: 404 },
  ) })
  await assert.rejects(client.check(), (error: unknown) => error instanceof ApiError && error.status === 404 && error.requestId === 'health-404')
})

test('development proxy keeps business paths and rewrites only the health route', () => {
  const proxy = createBackendProxy('http://127.0.0.1:8080')
  const api = proxy['^/api/v1(?:/|$)']
  assert.equal(api.target, 'http://127.0.0.1:8080')
  assert.equal(api.rewrite, undefined)
  assert.equal(new RegExp('^/api/v1(?:/|$)').test('/api/v10/users'), false)
  const health = proxy['^/backend/health(?:\\?|$)']
  assert.equal(health.rewrite!('/backend/health?probe=1'), '/health?probe=1')
  assert.equal(new RegExp('^/backend/health(?:\\?|$)').test('/backend/health/private'), false)
  for (const target of ['file:///tmp', 'http://user:pass@localhost:8080', 'http://localhost:8080/api', 'http://localhost:8080/?token=secret']) {
    assert.throws(() => createBackendProxy(target))
  }
  const cloud = createBackendProxy('https://www.rainycode.cn', '/backend/health')
  assert.equal(cloud['^/api/v1(?:/|$)'].target, 'https://www.rainycode.cn')
  assert.equal(cloud['^/backend/health(?:\\?|$)'].rewrite!('/backend/health?probe=1'), '/backend/health?probe=1')
  for (const path of ['https://other.example/health', '//other.example/health', '/health?token=secret']) {
    assert.throws(() => createBackendProxy('https://www.rainycode.cn', path))
  }
})
