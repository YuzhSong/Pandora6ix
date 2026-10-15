import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, createApiClient } from '../src/api/client.ts'
import { createHttpAdminRepository } from '../src/data/httpAdminRepository.ts'

const company = { id: 'company', name: '公司', loginDomain: 'team', businessTimezone: 'Asia/Shanghai', version: 0 }
const employee = { id: 'employee', displayName: '员工', loginName: 'staff', jobTitle: null, orgUnitId: null, supervisorUserId: 'admin', roles: ['EMPLOYEE'], status: 'DISABLED', version: 0 }
const administrator = { ...employee, id: 'admin', displayName: '管理员', loginName: 'admin', supervisorUserId: null, status: 'ACTIVE', roles: ['ADMIN'] }
function reader(url: string) {
  if (url.endsWith('/me')) return { id: 'admin', roles: ['ADMIN'] }
  if (url.endsWith('/admin/enterprise')) return company
  if (url.includes('/admin/users')) return { items: [administrator, employee], nextCursor: null }
  if (url.includes('/admin/org-units')) return { items: [{ id: 'root', name: '公司', kind: 'COMPANY', parentId: null, version: 0 }], nextCursor: null }
  return { items: [], nextCursor: null }
}
test('maps null organization and supervisor fields; retrieves every opaque cursor page', async () => {
  const calls: string[] = []
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async url => {
    calls.push(String(url))
    if (String(url).includes('/tags')) return Response.json({ data: { items: [{ id: calls.length.toString(), name: '标签', colorHex: '#112233', version: 0 }], nextCursor: String(url).includes('cursor=') ? null : 'opaque+/=' } })
    return Response.json({ data: reader(String(url)) })
  } }))
  const data = (await repository.getWorkspace())!
  assert.equal(data.people[1].orgUnitId, ''); assert.equal(data.people[1].supervisorId, 'admin')
  assert.equal(data.people[1].status, 'DISABLED'); assert.equal(data.tags.length, 2)
  assert.ok(calls.some(url => url.includes('cursor=opaque%2B%2F%3D')))
})
test('role patch sends full server command while preserving profile, status and stale version', async () => {
  let submitted: Record<string, unknown> | undefined
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') { submitted = JSON.parse(options.body as string); return Response.json({ data: { ...employee, roles: ['EMPLOYEE', 'ADMIN'], version: 1 } }) }
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  const data = await repository.saveRoles('employee', ['EMPLOYEE', 'ADMIN'], 0)
  assert.deepEqual(submitted, { expectedVersion: 0, displayName: '员工', jobTitle: '', orgUnitId: null, supervisorUserId: 'admin', roles: ['EMPLOYEE', 'ADMIN'], status: 'DISABLED' })
  assert.equal(data.people[1].version, 1)
})
test('409 does not mutate snapshot or retry the conflicting command', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') { writes++; return Response.json({ error: { code: 'VERSION_CONFLICT', message: '版本冲突' } }, { status: 409 }) }
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  await assert.rejects(repository.saveEnterprise('覆盖公司', 0), (error: unknown) => error instanceof ApiError && error.status === 409)
  assert.equal(writes, 1); assert.equal((await repository.load()).enterprise.name, '公司')
})
test('clearing identity while a write is pending prevents an old-company snapshot from returning', async () => {
  let release!: (value: Response) => void
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') return new Promise(resolve => { release = resolve })
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  const pending = repository.saveEnterprise('旧公司返回', 0)
  repository.clear(); release(Response.json({ data: { ...company, name: '旧公司返回', version: 1 } }))
  await assert.rejects(pending, (error: unknown) => error instanceof ApiError && error.code === 'REQUEST_CANCELLED')
})
