import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, createApiClient } from '../src/api/client.ts'
import { createAuthService, validatePassword } from '../src/api/auth.ts'
import { AUTH_KEY, createSessionStore } from '../src/api/session.ts'

function memory() {
  const values = new Map<string, string>()
  return { values, getItem: (key: string) => values.get(key) ?? null, setItem: (key: string, value: string) => { values.set(key, value) }, removeItem: (key: string) => { values.delete(key) } }
}
test('stores only tab session token and expiry; reloads, expires and clears session', () => {
  const storage = memory(), sessions = createSessionStore(storage, () => 1000)
  sessions.save({ accessToken: 'test-session', expiresAt: new Date(2000).toISOString() })
  assert.deepEqual(Object.keys(JSON.parse(storage.values.get(AUTH_KEY)!)).sort(), ['accessToken', 'expiresAt'])
  assert.equal(createSessionStore(storage, () => 1000).read()?.accessToken, 'test-session')
  assert.equal(createSessionStore(storage, () => 3000).read(), null)
  assert.equal(storage.values.size, 0)
})
test('Bearer headers are sent only for authenticated requests; failed login does not expire a session', async () => {
  let expired = 0
  const client = createApiClient({ baseUrl: '/api/v1', getAccessToken: () => 'test-session', onUnauthorized: () => expired++, fetcher: async (url, options) => {
    assert.equal(new Headers(options?.headers).get('Authorization'), String(url).endsWith('/me') ? 'Bearer test-session' : null)
    return new Response('{"error":{"code":"INVALID_CREDENTIALS"}}', { status: 401 })
  } })
  await assert.rejects(client.request('/auth/login', { method: 'POST', authenticated: false }))
  assert.equal(expired, 0)
  await assert.rejects(client.request('/me'))
  assert.equal(expired, 1)
})
test('non-admin login revokes its minted session and refuses Web access', async () => {
  const sessions = createSessionStore(memory())
  let revoked = false
  const client = createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (String(url).endsWith('/auth/logout')) { revoked = true; assert.equal(new Headers(options?.headers).get('Authorization'), 'Bearer employee-token'); return new Response(null, { status: 204 }) }
    return Response.json({ data: { accessToken: 'employee-token', tokenType: 'Bearer', expiresAt: new Date(Date.now() + 60000).toISOString(), user: { id: 'employee', roles: ['EMPLOYEE'] } } })
  } })
  await assert.rejects(createAuthService(client, sessions).login({ enterpriseDomain: 'test', loginName: 'employee', password: 'test-only-password' }), (error: unknown) => error instanceof ApiError && error.status === 403)
  assert.equal(revoked, true); assert.equal(sessions.read(), null)
})
test('logout failure retains the active token; password failure retains it, success clears it', async () => {
  const sessions = createSessionStore(memory())
  sessions.save({ accessToken: 'test-session', expiresAt: new Date(Date.now() + 60000).toISOString() })
  let fail = true
  const auth = createAuthService(createApiClient({ baseUrl: '/api/v1', fetcher: async () => fail ? new Response('{}', { status: 503 }) : new Response(null, { status: 204 }) }), sessions)
  await assert.rejects(auth.logout()); assert.ok(sessions.read())
  await assert.rejects(auth.changePassword('wrong', 'valid-new-password')); assert.ok(sessions.read())
  fail = false
  await auth.changePassword('old-password', 'valid-new-password'); assert.equal(sessions.read(), null)
})
test('password validation honors BCrypt UTF-8 byte bound', () => {
  assert.throws(() => validatePassword('short'))
  assert.throws(() => validatePassword('密'.repeat(25)))
  assert.doesNotThrow(() => validatePassword('密'.repeat(24)))
})

test('failed session persistence revokes the newly minted token and reports storage failure', async () => {
  const sessions = createSessionStore({ getItem: () => null, setItem: () => { throw new Error('denied') }, removeItem: () => undefined })
  let revoked = false
  const client = createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (String(url).endsWith('/auth/logout')) { revoked = true; assert.equal(new Headers(options?.headers).get('Authorization'), 'Bearer unpersisted-token'); return new Response(null, { status: 204 }) }
    return Response.json({ data: { accessToken: 'unpersisted-token', tokenType: 'Bearer', expiresAt: new Date(Date.now() + 60000).toISOString(), user: { id: 'admin', roles: ['ADMIN'] } } })
  } })
  await assert.rejects(createAuthService(client, sessions).login({ enterpriseDomain: 'test', loginName: 'admin', password: 'test-only-password' }), /无法保存登录会话/)
  assert.equal(revoked, true)
})
