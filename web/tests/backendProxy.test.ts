import assert from 'node:assert/strict'
import { createServer as createHttpServer } from 'node:http'
import type { AddressInfo } from 'node:net'
import test from 'node:test'
import { createServer as createViteServer } from 'vite'
import { createBackendProxy } from '../dev/backendProxy.ts'
import { createHealthClient } from '../src/api/health.ts'
import { ApiError, createApiClient } from '../src/api/client.ts'

for (const healthPath of ['/health', '/backend/health']) test(`real Vite proxy forwards ${healthPath} and preserves backend business errors`, async () => {
  // Contract fixture only: this does not claim that Spring business APIs exist.
  const paths: string[] = []
  const backend = createHttpServer((request, response) => {
    paths.push(request.url!)
    response.setHeader('Content-Type', 'application/json')
    if (request.url === healthPath) response.end('{"status":"UP"}')
    else {
      response.statusCode = 404
      response.setHeader('X-Request-Id', 'fixture-404')
      response.end('{"code":"RESOURCE_NOT_FOUND","message":"Resource was not found."}')
    }
  })
  let vite: Awaited<ReturnType<typeof createViteServer>> | undefined
  try {
    await new Promise<void>((resolve, reject) => {
      backend.once('error', reject)
      backend.listen(0, '127.0.0.1', resolve)
    })
    const backendPort = (backend.address() as AddressInfo).port
    vite = await createViteServer({
      configFile: false, logLevel: 'silent',
      server: { host: '127.0.0.1', port: 0, proxy: createBackendProxy(`http://127.0.0.1:${backendPort}`, healthPath) },
    })
    await vite.listen()
    const origin = `http://127.0.0.1:${(vite.httpServer!.address() as AddressInfo).port}`
    assert.deepEqual(await createHealthClient({ baseUrl: `${origin}/backend` }).check(), { status: 'UP' })
    await assert.rejects(createApiClient({ baseUrl: `${origin}/api/v1` }).request('/me'),
      (error: unknown) => error instanceof ApiError && error.status === 404 && error.code === 'RESOURCE_NOT_FOUND' && error.requestId === 'fixture-404')
    assert.deepEqual(paths, [healthPath, '/api/v1/me'])
  } finally {
    await vite?.close()
    backend.closeAllConnections()
    await new Promise<void>(resolve => backend.close(() => resolve()))
  }
})
