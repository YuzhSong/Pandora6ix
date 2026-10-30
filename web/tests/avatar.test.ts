import assert from 'node:assert/strict'
import test from 'node:test'
import { createApiClient, ApiError } from '../src/api/client.ts'
import { createAvatarService, validateAvatarFile, validateAvatarDataUrl } from '../src/api/avatar.ts'

const dataUrl = 'data:image/png;base64,aGVsbG8='
test('avatar uses authenticated binary reads and JSON PUT; fresh reader obtains persisted bytes', async () => {
  let stored: string | null = null
  const client = createApiClient({ baseUrl: '/api/v1', getAccessToken: () => 'test-session', fetcher: async (url, init) => {
    assert.equal(url, '/api/v1/users/person%2F1/avatar')
    assert.equal(new Headers(init?.headers).get('Authorization'), 'Bearer test-session')
    assert.equal(init?.cache, 'no-store')
    if (init?.method === 'PUT') { stored = JSON.parse(String(init.body)).dataUrl; return new Response(null, { status: 204 }) }
    return stored ? new Response(new Uint8Array([1, 2, 3]), { headers: { 'Content-Type': 'image/png' } }) : new Response(null, { status: 404 })
  } })
  assert.equal(await createAvatarService(client).load('person/1'), null)
  await createAvatarService(client).save('person/1', dataUrl)
  assert.equal(stored, dataUrl)
  assert.deepEqual([...new Uint8Array(await (await createAvatarService(client).load('person/1'))!.arrayBuffer())], [1, 2, 3])
})

test('unsupported, empty and oversized images are rejected before upload; failures and expired sessions surface', async () => {
  for (const file of [{ type: 'image/svg+xml', size: 10 }, { type: 'image/png', size: 0 }, { type: 'image/jpeg', size: 6 * 1024 * 1024 }]) assert.throws(() => validateAvatarFile(file))
  validateAvatarFile({ type: 'image/webp', size: 200 })
  for (const url of ['https://example.invalid/image.png', 'data:image/svg+xml;base64,aGVsbG8=', `data:image/png;base64,${'a'.repeat(768000)}`]) assert.throws(() => validateAvatarDataUrl(url))
  let expired = false
  const service = createAvatarService(createApiClient({ baseUrl: '/api/v1', onUnauthorized: () => { expired = true }, fetcher: async () => Response.json({ code: 'UNAUTHENTICATED' }, { status: 401 }) }))
  await assert.rejects(service.save('u', dataUrl), (error: unknown) => error instanceof ApiError && error.status === 401)
  assert.equal(expired, true)
})
