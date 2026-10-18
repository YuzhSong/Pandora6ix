import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, createApiClient } from '../src/api/client.ts'
import { createHttpAdminRepository } from '../src/data/httpAdminRepository.ts'
import { errorMessage } from '../src/api/feedback.ts'
import type { PersonInput } from '../src/data/types.ts'

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

test('workspace does not request tags or employee business records; tag writes are unavailable', async () => {
  const calls: string[] = []
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async url => {
    calls.push(String(url)); return Response.json({ data: reader(String(url)) })
  } }))
  const data = await repository.getWorkspace()
  assert.deepEqual(data.tags, [])
  await assert.rejects(repository.saveTag({ name: '标签', colorHex: '#112233' }), /不提供标签管理/)
  assert.ok(calls.every(url => !/tags|journals|tasks|statistics/.test(url)))
})

test('maps null organization and supervisor fields; retrieves every opaque user cursor page', async () => {
  const calls: string[] = []
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async url => {
    const path = String(url); calls.push(path)
    if (path.includes('/admin/users')) return Response.json({ data: { items: path.includes('cursor=') ? [employee] : [administrator], nextCursor: path.includes('cursor=') ? null : 'opaque+/=' } })
    return Response.json({ data: reader(path) })
  } }))
  const data = await repository.getWorkspace()
  assert.equal(data.people[1].orgUnitId, ''); assert.equal(data.people[1].supervisorId, 'admin')
  assert.equal(data.people[1].status, 'DISABLED'); assert.equal(data.people.length, 2)
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

const personal = { gender: 'FEMALE' as const, idNumber: '00000020000101000X', phoneNumber: '00000000000' }
const newPerson: PersonInput = { displayName: '测试员工', loginName: 'new-staff', jobTitle: '测试', orgUnitId: 'root', supervisorId: null, roles: ['EMPLOYEE'], ...personal }

test('employee password reset sends only version and replacement password and preserves profile/status', async () => {
  let submitted: unknown
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'POST') { assert.ok(String(url).endsWith('/admin/users/employee/password-reset')); submitted = JSON.parse(options.body as string); return Response.json({ data: { ...employee, version: 1 } }) }
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.resetPersonPassword('admin', 'new-password-fixture', 0), /管理员账号/)
  await assert.rejects(repository.resetPersonPassword('employee', 'short', 0), /至少/)
  const data = await repository.resetPersonPassword('employee', 'new-password-fixture', 0)
  assert.deepEqual(submitted, { expectedVersion: 0, newPassword: 'new-password-fixture' })
  assert.equal(data.people.find(person => person.id === 'employee')!.status, 'DISABLED')
  assert.equal(data.people.find(person => person.id === 'employee')!.version, 1)
  assert.ok(!JSON.stringify(data).includes('new-password-fixture'))
})

test('reset failure or mismatched account response never claims success or updates the snapshot', async () => {
  let mode = 'denied'
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'POST') {
      if (mode === 'denied') return Response.json({ error: { code: 'VERSION_CONFLICT', message: 'Conflict' } }, { status: 409 })
      return Response.json({ data: { ...employee, id: mode === 'wrong' ? 'wrong-id' : employee.id, version: 1 } })
    }
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.resetPersonPassword('employee', 'new-password-fixture', 0), (reason: unknown) => reason instanceof ApiError && reason.status === 409)
  mode = 'wrong'
  await assert.rejects(repository.resetPersonPassword('employee', 'new-password-fixture', 0), (reason: unknown) => reason instanceof ApiError && reason.code === 'INVALID_RESPONSE')
  mode = 'ok'
  assert.equal((await repository.resetPersonPassword('employee', 'new-password-fixture', 0)).people.find(person => person.id === 'employee')!.version, 1)
})

const removableUnit = { id: 'empty/dept', name: '空部门', kind: 'DEPARTMENT', parentId: 'root', version: 4 }
function deleteReader(url: string) {
  return url.includes('/admin/org-units') ? { items: [{ id: 'root', name: '公司', kind: 'COMPANY', parentId: null, version: 0 }, removableUnit], nextCursor: null } : reader(url)
}

test('deletion sends escaped identifier and displayed version; removes unit only after 204', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'DELETE') {
      writes++; assert.ok(String(url).endsWith('/admin/org-units/empty%2Fdept'))
      assert.deepEqual(JSON.parse(options.body as string), { expectedVersion: 4 })
      return new Response(null, { status: 204 })
    }
    return Response.json({ data: deleteReader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.deleteUnit('root', 0), /根组织/)
  const data = await repository.deleteUnit(removableUnit.id, 4)
  assert.equal(writes, 1); assert.equal(data.units.length, 1); assert.equal(data.people.length, 2)
  await assert.rejects(repository.deleteUnit(removableUnit.id, 4), /不存在/)
})

test('failed deletion preserves snapshot; clearing identity prevents a delayed delete updating a new workspace', async () => {
  let rejectWrite = true
  let release!: (value: Response) => void
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'DELETE') {
      if (rejectWrite) return Response.json({ error: { code: 'ORG_HAS_MEMBERS', message: 'Members remain' } }, { status: 409 })
      return new Promise(resolve => { release = resolve })
    }
    return Response.json({ data: deleteReader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.deleteUnit(removableUnit.id, 4), (error: unknown) => error instanceof ApiError && error.code === 'ORG_HAS_MEMBERS')
  // A second DELETE proves the failed write did not remove the unit locally.
  rejectWrite = false
  const pending = repository.deleteUnit(removableUnit.id, 4)
  repository.clear(); await repository.load()
  release(new Response(null, { status: 204 }))
  await assert.rejects(pending, (error: unknown) => error instanceof ApiError && error.code === 'REQUEST_CANCELLED')
  assert.equal((await repository.load()).units.length, 2)
})

test('unexpected successful delete payload is rejected without removing the unit', async () => {
  let valid = false
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'DELETE') return valid ? new Response(null, { status: 204 }) : Response.json({ data: {} })
    return Response.json({ data: deleteReader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.deleteUnit(removableUnit.id, 4), (error: unknown) => error instanceof ApiError && error.code === 'INVALID_RESPONSE')
  valid = true
  assert.equal((await repository.deleteUnit(removableUnit.id, 4)).units.length, 1)
})

test('an older in-flight read cannot restore a deleted unit', async () => {
  let hold = false
  let release!: (value: Response) => void
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'DELETE') return new Response(null, { status: 204 })
    if (hold && String(url).includes('/admin/org-units')) return new Promise(resolve => { release = resolve })
    return Response.json({ data: deleteReader(String(url)) })
  } }))
  await repository.load(); hold = true
  const pending = repository.load()
  // GET awaits /me before requesting directory pages.
  await new Promise(resolve => setTimeout(resolve, 0))
  const next = await repository.deleteUnit(removableUnit.id, 4)
  release(Response.json({ data: deleteReader('/admin/org-units') }))
  await assert.rejects(pending, (error: unknown) => error instanceof ApiError && error.code === 'REQUEST_CANCELLED')
  assert.equal(next.units.length, 1)
})

test('legacy null profiles can be renamed or supplemented one field at a time; populated fields cannot be cleared', async () => {
  let stored = { ...employee, gender: null as PersonInput['gender'], idNumber: null as string | null, phoneNumber: null as string | null }
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') {
      writes++
      stored = { ...stored, ...JSON.parse(options.body as string), version: stored.version + 1 }
      return Response.json({ data: stored })
    }
    if (String(url).includes('/admin/users')) return Response.json({ data: { items: [{ ...administrator, gender: null, idNumber: null, phoneNumber: null }, stored], nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  let data = await repository.load()
  let person = data.people.find(value => value.id === 'employee')!
  data = await repository.savePerson({ ...person, displayName: '历史员工改名' }, person.id, person.version)
  person = data.people.find(value => value.id === 'employee')!
  assert.equal(person.gender, null)
  data = await repository.savePerson({ ...person, phoneNumber: '+86 010 12345678' }, person.id, person.version)
  person = data.people.find(value => value.id === 'employee')!
  assert.equal(person.idNumber, null); assert.equal(person.phoneNumber, '+86 010 12345678')
  await assert.rejects(repository.savePerson({ ...person, phoneNumber: null }, person.id, person.version), /不支持清空/)
  assert.equal(writes, 2)
})

test('old server blocks incomplete employee creation and personal edits before sending a write', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'POST' || options?.method === 'PATCH') writes++
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  await assert.rejects(repository.savePerson(newPerson, undefined, undefined, 'valid-password'), /尚未支持/)
  await assert.rejects(repository.savePerson({ ...newPerson, loginName: 'staff' }, 'employee', 0), /尚未支持/)
  assert.equal(writes, 0)
})

test('supported employee creation uses one request including login and personal fields', async () => {
  const writes: Record<string, unknown>[] = []
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'POST') {
      const body = JSON.parse(options.body as string); writes.push(body)
      assert.ok(String(url).endsWith('/admin/users'))
      return Response.json({ data: { ...employee, ...body, id: 'new', status: 'ACTIVE', version: 0 } })
    }
    if (String(url).includes('/admin/users')) return Response.json({ data: { items: [administrator, employee].map(value => ({ ...value, gender: null, idNumber: null, phoneNumber: null })), nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  const data = await repository.savePerson(newPerson, undefined, undefined, 'valid-password')
  assert.equal(writes.length, 1)
  assert.deepEqual(writes[0], { loginName: 'new-staff', password: 'valid-password', displayName: '测试员工', jobTitle: '测试', orgUnitId: 'root', supervisorUserId: null, roles: ['EMPLOYEE'], ...personal })
  assert.equal(data.people.find(value => value.id === 'new')?.idNumber, personal.idNumber)
  assert.ok(data.people.every(value => !('password' in value)))
})

test('role-only and ordinary edits preserve supported personal fields and status', async () => {
  let submitted: Record<string, unknown> = {}
  const staff = { ...employee, ...personal }
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') {
      submitted = JSON.parse(options.body as string)
      return Response.json({ data: { ...staff, ...submitted, version: 1 } })
    }
    if (String(url).includes('/admin/users')) return Response.json({ data: { items: [{ ...administrator, ...personal }, staff], nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  await repository.saveRoles('employee', ['EMPLOYEE', 'ADMIN'], 0)
  assert.equal(submitted.idNumber, personal.idNumber); assert.equal(submitted.status, 'DISABLED')
  await repository.savePerson({ displayName: '新姓名', loginName: 'staff', jobTitle: '', orgUnitId: '', supervisorId: 'admin', roles: ['EMPLOYEE', 'ADMIN'] }, 'employee', 1)
  assert.equal(submitted.phoneNumber, personal.phoneNumber)
})

test('login rename sends its version and preserves nullable historical profiles after reload', async () => {
  let staff = { ...employee, gender: null, idNumber: null, phoneNumber: null }
  let submitted: Record<string, unknown> = {}
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') {
      submitted = JSON.parse(options.body as string)
      staff = { ...staff, loginName: submitted.loginName as string, version: 1 }
      return Response.json({ data: staff })
    }
    if (String(url).includes('/admin/users')) return Response.json({ data: { items: [{ ...administrator, gender: null, idNumber: null, phoneNumber: null }, staff], nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  const before = (await repository.getWorkspace()).people.find(person => person.id === 'employee')!
  const saved = await repository.savePerson({ ...before, loginName: 'zhang.san' }, before.id, before.version)
  assert.equal(submitted.loginName, 'zhang.san'); assert.equal(submitted.expectedVersion, 0)
  assert.equal(submitted.status, 'DISABLED'); assert.equal(submitted.phoneNumber, null)
  assert.equal(Object.hasOwn(submitted, 'password'), false)
  assert.equal(saved.people.find(person => person.id === before.id)?.loginName, 'zhang.san')
  assert.equal((await repository.load()).people.find(person => person.id === before.id)?.loginName, 'zhang.san')
})

test('duplicate login-name errors keep the original account snapshot and explain the conflict', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') {
      writes++
      return Response.json({ error: { code: 'LOGIN_NAME_TAKEN', message: 'Already used' } }, { status: 409 })
    }
    return Response.json({ data: reader(String(url)) })
  } }))
  const before = (await repository.getWorkspace()).people.find(person => person.id === 'employee')!
  await assert.rejects(repository.savePerson({ ...before, loginName: 'admin' }, before.id, before.version), (reason: unknown) => {
    assert.ok(reason instanceof ApiError)
    assert.equal(reason.code, 'LOGIN_NAME_TAKEN')
    assert.match(errorMessage(reason), /登录名.*其他账号/)
    return true
  })
  assert.equal(writes, 1)
  assert.equal((await repository.load()).people.find(person => person.id === before.id)?.loginName, 'staff')
})

test('surrounding whitespace cannot create a login name that the login form cannot submit', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') writes++
    return Response.json({ data: reader(String(url)) })
  } }))
  const before = (await repository.getWorkspace()).people.find(person => person.id === 'employee')!
  for (const loginName of [' zhang.san', 'zhang.san ', '\u2003zhang.san']) {
    await assert.rejects(repository.savePerson({ ...before, loginName }, before.id, before.version), /前后不能有空白/)
  }
  assert.equal(writes, 0)
})

test('an older backend ignoring loginName cannot report a successful rename or trigger a duplicate retry', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') { writes++; return Response.json({ data: { ...employee, version: 1 } }) }
    return Response.json({ data: reader(String(url)) })
  } }))
  const before = (await repository.getWorkspace()).people.find(person => person.id === 'employee')!
  await assert.rejects(repository.savePerson({ ...before, loginName: 'zhang.san' }, before.id, before.version), /核实账号/)
  await assert.rejects(repository.savePerson({ ...before, loginName: 'zhang.san' }, before.id, before.version), /勿重复提交/)
  assert.equal(writes, 1)
})

test('missing personal fields in a write response never report success or allow duplicate retry', async () => {
  let writes = 0
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'POST') { writes++; return Response.json({ data: { ...employee, id: 'new' } }) }
    if (String(url).includes('/admin/users')) return Response.json({ data: { items: [{ ...administrator, gender: null, idNumber: null, phoneNumber: null }], nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.getWorkspace()
  await assert.rejects(repository.savePerson(newPerson, undefined, undefined, 'valid-password'), /核实账号/)
  await assert.rejects(repository.savePerson(newPerson, undefined, undefined, 'valid-password'), /勿重复提交/)
  assert.equal(writes, 1)
})

test('organization kind is submitted; backend policy rejection leaves the original type available', async () => {
  let submitted: Record<string, unknown> = {}
  const repository = createHttpAdminRepository(createApiClient({ baseUrl: '/api/v1', fetcher: async (url, options) => {
    if (options?.method === 'PATCH') { submitted = JSON.parse(options.body as string); return Response.json({ error: { code: 'WORKFLOW_POLICY_UNCONFIRMED', message: '后端暂不支持修改组织类型' } }, { status: 409 }) }
    if (String(url).includes('/admin/org-units')) return Response.json({ data: { items: [{ id: 'root', name: '公司', kind: 'COMPANY', parentId: null, version: 0 }, { id: 'leaf', name: '部门', kind: 'DEPARTMENT', parentId: 'root', version: 0 }, { id: 'other', name: '其他部门', kind: 'DEPARTMENT', parentId: 'root', version: 0 }], nextCursor: null } })
    return Response.json({ data: reader(String(url)) })
  } }))
  await repository.load()
  await assert.rejects(repository.saveUnit({ name: '团队', kind: 'TEAM', parentId: 'other' }, 'leaf', 0), (error: unknown) => error instanceof ApiError && error.code === 'WORKFLOW_POLICY_UNCONFIRMED' && error.status === 409)
  assert.equal(submitted.kind, 'TEAM'); assert.equal(submitted.expectedVersion, 0)
  assert.equal((await repository.load()).units.find(unit => unit.id === 'leaf')?.kind, 'DEPARTMENT')
})
