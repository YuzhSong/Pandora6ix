import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
import { randomUUID, randomBytes } from 'node:crypto'
import { existsSync } from 'node:fs'
import { mkdir } from 'node:fs/promises'
import { createServer as createNetServer } from 'node:net'
import type { AddressInfo } from 'node:net'
import path from 'node:path'
import { createServer as createViteServer } from 'vite'
import { ApiError, createApiClient } from '../src/api/client.ts'
import { createAuthService } from '../src/api/auth.ts'
import { createSessionStore } from '../src/api/session.ts'
import { createHttpAdminRepository } from '../src/data/httpAdminRepository.ts'
import { createBackendProxy } from '../dev/backendProxy.ts'
import { bulkPersonnelTemplate, createBulkPersonnel, previewBulkPersonnel } from '../src/data/bulkPersonnel.ts'

// Uses an explicitly supplied backend artifact or the documented source-copy artifact,
// with a fresh database under web/.integration. No team server or credentials are used.
const revision = execFileSync('git', ['log', '-1', '--format=%h', '--', 'backend'], { cwd: path.resolve('..'), encoding: 'utf8' }).trim()
const jar = process.env.PANDORA_INTEGRATION_JAR ? path.resolve(process.env.PANDORA_INTEGRATION_JAR) : path.resolve(`.integration/backend-${revision}/target/backend-0.0.1-SNAPSHOT.jar`)
if (!existsSync(jar)) throw new Error('请先按 README 在 .integration/backend 构建后端原样副本。')
const probe = createNetServer()
await new Promise<void>(resolve => probe.listen(0, '127.0.0.1', resolve))
const port = (probe.address() as AddressInfo).port
await new Promise<void>(resolve => probe.close(() => resolve()))
const runtime = path.resolve('.integration/runs', randomUUID())
await mkdir(runtime, { recursive: true })
const bootstrap = randomUUID(), password = randomUUID()
const domain = process.argv.includes('--preview') ? 'pandora-local' : `web-${randomUUID()}`
// Readable fixture password only for the disposable local preview.
const previewPassword = process.argv.includes('--preview') ? '1234567890' : undefined
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java.exe') : 'java'
const handoffKey = randomBytes(32).toString('base64')
let spawnError: Error | undefined
function startBackend() {
  const child = spawn(java, ['-jar', jar, `--server.port=${port}`, '--server.address=127.0.0.1'], {
    cwd: runtime, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true,
    env: { ...process.env, PANDORA_DB_URL: 'jdbc:h2:file:./pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0', PANDORA_DB_USER: 'sa', PANDORA_DB_PASSWORD: '', PANDORA_HANDOFF_KEY: handoffKey, PANDORA_BOOTSTRAP_TOKEN: bootstrap },
  })
  child.on('error', error => { spawnError = error })
  // Drain JVM logs without printing credentials, responses, or internal diagnostics.
  child.stdout.on('data', () => undefined); child.stderr.on('data', () => undefined)
  return child
}
let backend = startBackend()
let vite: Awaited<ReturnType<typeof createViteServer>> | undefined
try {
  async function ready() {
    let healthy = false
    for (let attempt = 0; attempt < 480; attempt++) {
      if (spawnError) throw spawnError
      if (backend.exitCode !== null) throw new Error('后端测试进程提前退出。')
      try { healthy = ((await (await fetch(`http://127.0.0.1:${port}/health`)).json()) as { status?: unknown }).status === 'UP' } catch { /* Startup retry. */ }
      if (healthy) break
      await new Promise(resolve => setTimeout(resolve, 250))
    }
    assert.ok(healthy, '真实后端应启动成功')
  }
  await ready()
  vite = await createViteServer({ configFile: false, logLevel: 'silent', server: { host: '127.0.0.1', port: 0, proxy: createBackendProxy(`http://127.0.0.1:${port}`) } })
  await vite.listen()
  const origin = `http://127.0.0.1:${(vite.httpServer!.address() as AddressInfo).port}`
  const values = new Map<string, string>()
  const sessions = createSessionStore({ getItem: key => values.get(key) ?? null, setItem: (key, value) => { values.set(key, value) }, removeItem: key => { values.delete(key) } })
  const client = createApiClient({ baseUrl: `${origin}/api/v1`, getAccessToken: () => sessions.read()?.accessToken ?? null, onUnauthorized: () => sessions.clear() })
  const auth = createAuthService(client, sessions), repository = createHttpAdminRepository(client)
  sessions.subscribe(() => { if (!sessions.read()) repository.clear() })
  const initialization = { loginDomain: domain, name: 'Web 联调测试公司', businessTimezone: 'Asia/Shanghai', adminLoginName: 'admin', adminPassword: password, adminDisplayName: '联调管理员' }
  const key = randomUUID()
  // Disposable test fixture only; company provisioning is absent from the Web app.
  const initialize = () => client.request('/admin/enterprise-initializations', { method: 'POST', body: initialization, authenticated: false, headers: { 'X-Bootstrap-Token': bootstrap, 'Idempotency-Key': key } })
  const initialized = await initialize()
  assert.deepEqual(await initialize(), initialized, '重复初始化应幂等')
  await auth.login({ enterpriseDomain: domain, loginName: 'admin', password })
  let data = (await repository.getWorkspace())!
  assert.equal(data.people.length, 1); assert.equal(data.enterprise.version, 0)
  const legacyAdmin = data.people[0]
  assert.equal(legacyAdmin.gender, null); assert.equal(legacyAdmin.idNumber, null)
  data = await repository.savePerson({ ...legacyAdmin, phoneNumber: '+86 (010) 12345678' }, legacyAdmin.id, legacyAdmin.version)
  data = await repository.load()
  assert.equal(data.people[0].phoneNumber, '+86 (010) 12345678')
  assert.equal(data.people[0].gender, null); assert.equal(data.people[0].idNumber, null)
  const oldVersion = data.enterprise.version
  data = await repository.saveEnterprise('Web 联调公司更新', oldVersion)
  await assert.rejects(repository.saveEnterprise('冲突覆盖', oldVersion), (error: unknown) => error instanceof ApiError && error.status === 409)
  data = await repository.saveUnit({ name: '研发部', kind: 'DEPARTMENT', parentId: data.units[0].id })
  const department = data.units.find(unit => unit.name === '研发部')!
  data = await repository.saveUnit({ name: 'Web 团队', kind: 'TEAM', parentId: department.id })
  const personnel = { gender: 'MALE' as const, idNumber: '11010119900101001X', phoneNumber: '13800000000' }
  await client.request('/admin/users', { method: 'POST', body: { displayName: '测试负责人', loginName: 'manager', jobTitle: '负责人', orgUnitId: data.units[0].id, supervisorUserId: null, roles: ['FOUNDER'], password: previewPassword ?? randomUUID(), ...personnel } })
  data = await repository.load()
  const manager = data.people.find(person => person.loginName === 'manager')!
  const staffPassword = previewPassword ?? randomUUID()
  data = await repository.savePerson({ displayName: '测试人员', loginName: 'staff', jobTitle: '前端', orgUnitId: department.id, supervisorId: manager.id, roles: ['EMPLOYEE'], ...personnel }, undefined, undefined, staffPassword)
  data = await repository.load()
  assert.equal(data.people.find(person => person.loginName === 'staff')?.gender, personnel.gender)
  assert.equal(data.people.find(person => person.loginName === 'staff')?.idNumber, personnel.idNumber)
  const handoffStaff = data.people.find(person => person.loginName === 'staff')!
  assert.equal((await repository.credentialHandoffs(handoffStaff.id))[0].initialPassword, staffPassword)
  const exitedForRestart = new Promise<void>(resolve => backend.once('exit', () => resolve()))
  backend.kill(); await exitedForRestart
  spawnError = undefined; backend = startBackend(); await ready()
  const reloadedRepository = createHttpAdminRepository(client)
  await reloadedRepository.load()
  assert.equal((await reloadedRepository.credentialHandoffs(handoffStaff.id))[0].initialPassword, staffPassword, '初始密码在后端重启和页面重新加载后仍可导出')
  const employeeSession = await client.request<{ accessToken: string; user: { roles: string[] } }>('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: 'staff', password: staffPassword } })
  assert.ok(employeeSession.user.roles.includes('EMPLOYEE'), '新增人员的账号应可真实登录')
  const employeeClient = createApiClient({ baseUrl: `${origin}/api/v1`, getAccessToken: () => employeeSession.accessToken })
  const employeeMe = await employeeClient.request<Record<string, unknown>>('/me')
  assert.ok(!Object.hasOwn(employeeMe, 'idNumber') && !Object.hasOwn(employeeMe, 'phoneNumber'), '非管理端投影不得泄露证件或电话')
  await assert.rejects(employeeClient.request('/admin/users'), (error: unknown) => error instanceof ApiError && error.status === 403)
  await employeeClient.request('/auth/logout', { method: 'POST' })
  await assert.rejects(auth.login({ enterpriseDomain: domain, loginName: 'staff', password: staffPassword }), (error: unknown) => error instanceof ApiError && error.status === 403)
  assert.ok(sessions.read(), '拒绝普通员工登录不能清除已有管理员身份')
  let staff = data.people.find(person => person.loginName === 'staff')!
  data = await repository.savePerson({ ...staff, displayName: '更新人员' }, staff.id, staff.version)
  staff = data.people.find(person => person.id === staff.id)!
  data = await repository.savePerson({ ...staff, loginName: 'zhang.san' }, staff.id, staff.version)
  staff = data.people.find(person => person.id === staff.id)!
  assert.equal(staff.loginName, 'zhang.san')
  const anonymous = createApiClient({ baseUrl: `${origin}/api/v1` })
  await assert.rejects(anonymous.request('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: 'staff', password: staffPassword } }), (error: unknown) => error instanceof ApiError && error.status === 401)
  const renamed = await anonymous.request<{ accessToken: string; user: { id: string } }>('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: 'zhang.san', password: staffPassword } })
  assert.equal(renamed.user.id, staff.id)
  await anonymous.request('/auth/logout', { method: 'POST', authenticated: false, headers: { Authorization: `Bearer ${renamed.accessToken}` } })
  await assert.rejects(repository.savePerson({ ...staff, loginName: 'admin' }, staff.id, staff.version), (error: unknown) => error instanceof ApiError && error.code === 'LOGIN_NAME_TAKEN')
  data = await repository.saveRoles(staff.id, ['EMPLOYEE', 'DEPARTMENT_MANAGER'], staff.version)
  assert.equal(data.people.find(person => person.id === staff.id)!.idNumber, personnel.idNumber)
  data = await repository.load()
  assert.equal(data.people.find(person => person.id === staff.id)!.phoneNumber, personnel.phoneNumber)
  const leaf = data.units.find(unit => unit.kind === 'TEAM')!
  data = await repository.saveUnit({ name: leaf.name, kind: 'DEPARTMENT', parentId: data.units.find(unit => unit.kind === 'COMPANY')!.id }, leaf.id, leaf.version)
  data = await repository.load()
  assert.equal(data.units.find(unit => unit.id === leaf.id)!.kind, 'DEPARTMENT')
  data = await repository.saveUnit({ name: leaf.name, kind: 'TEAM', parentId: department.id }, leaf.id, data.units.find(unit => unit.id === leaf.id)!.version)
  data = await repository.load()
  assert.equal(data.units.find(unit => unit.id === leaf.id)!.kind, 'TEAM')
  assert.equal(data.units.find(unit => unit.id === leaf.id)!.parentId, department.id)
  await assert.rejects(repository.deleteUnit(department.id, department.version), /下级组织/)
  data = await repository.saveUnit({ name: '删除联调部门', kind: 'DEPARTMENT', parentId: data.units.find(unit => unit.kind === 'COMPANY')!.id })
  const disposableDepartment = data.units.find(unit => unit.name === '删除联调部门')!
  data = await repository.saveUnit({ name: '删除联调团队', kind: 'TEAM', parentId: disposableDepartment.id })
  const disposableTeam = data.units.find(unit => unit.name === '删除联调团队')!
  await assert.rejects(repository.deleteUnit(disposableTeam.id, disposableTeam.version + 1), (error: unknown) => error instanceof ApiError && error.code === 'VERSION_CONFLICT')
  data = await repository.deleteUnit(disposableTeam.id, disposableTeam.version)
  assert.ok(!data.units.some(unit => unit.id === disposableTeam.id))
  data = await repository.load()
  assert.ok(!data.units.some(unit => unit.id === disposableTeam.id))
  data = await repository.deleteUnit(disposableDepartment.id, disposableDepartment.version)
  data = await repository.load()
  assert.ok(!data.units.some(unit => unit.id === disposableDepartment.id))
  const previousStaff = data.people.find(person => person.id === staff.id)!
  const oldStaffSession = await client.request<{ accessToken: string }>('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: previousStaff.loginName, password: staffPassword } })
  const resetPassword = randomUUID()
  data = await repository.resetPersonPassword(staff.id, resetPassword, previousStaff.version)
  assert.equal(data.people.find(person => person.id === staff.id)!.phoneNumber, personnel.phoneNumber)
  assert.equal(data.people.find(person => person.id === staff.id)!.version, previousStaff.version + 1)
  const oldStaffClient = createApiClient({ baseUrl: `${origin}/api/v1`, getAccessToken: () => oldStaffSession.accessToken })
  await assert.rejects(oldStaffClient.request('/me'), (reason: unknown) => reason instanceof ApiError && reason.status === 401)
  await assert.rejects(client.request('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: previousStaff.loginName, password: staffPassword } }), (reason: unknown) => reason instanceof ApiError && reason.status === 401)
  const newStaffSession = await client.request<{ accessToken: string }>('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: previousStaff.loginName, password: resetPassword } })
  assert.equal((await repository.credentialHandoffs(staff.id))[0].initialPassword, resetPassword)
  const changedClient = createApiClient({ baseUrl: `${origin}/api/v1`, getAccessToken: () => newStaffSession.accessToken })
  const selfPassword = randomUUID()
  await changedClient.request('/me/password', { method: 'POST', body: { currentPassword: resetPassword, newPassword: selfPassword } })
  const changedHandoff = (await repository.credentialHandoffs(staff.id))[0]
  assert.equal(changedHandoff.status, 'CHANGED'); assert.equal(changedHandoff.initialPassword, undefined)
  await assert.rejects(changedClient.request('/me'), (reason: unknown) => reason instanceof ApiError && reason.status === 401)
  const batchPreview = previewBulkPersonnel(bulkPersonnelTemplate() + ['批量甲,batch-one', '批量乙,batch-two'].map(prefix => `${prefix},女,000000200001010001,00000000001,员工,${department.name},manager`).join('\n'), data)
  assert.deepEqual(batchPreview.errors, [])
  let batchCreated = 0
  await createBulkPersonnel(batchPreview.rows, repository, { stopped: () => false, onRow: (_, status, message) => { assert.notEqual(status, 'failed', message) }, onCreated: (next, account) => { data = next; batchCreated++; assert.equal(account.initialPassword, batchPreview.rows.find(row => row.input.loginName === account.loginName)!.initialPassword) } })
  assert.equal(batchCreated, 2)
  const deliveredBatch = (await repository.credentialHandoffs()).find(account => account.loginName === 'batch-one')!
  assert.equal(deliveredBatch.initialPassword, batchPreview.rows[0].initialPassword)
  const deliveredLogin = await client.request<{ accessToken: string; user: { id: string } }>('/auth/login', { method: 'POST', authenticated: false, body: { enterpriseDomain: domain, loginName: deliveredBatch.loginName, password: deliveredBatch.initialPassword } })
  assert.equal(deliveredLogin.user.id, deliveredBatch.personId, '导出的初始密码原文必须能让对应员工真实登录')
  await client.request('/auth/logout', { method: 'POST', authenticated: false, headers: { Authorization: `Bearer ${deliveredLogin.accessToken}` } })
  data = await repository.load()
  assert.ok(data.people.some(person => person.loginName === 'batch-one'))
  assert.ok(data.people.some(person => person.loginName === 'batch-two'))
  await assert.rejects(repository.saveTag({ name: '不可管理', colorHex: '#112233' }), /不提供标签管理/)
  const promotionTarget = data.people.find(person => person.loginName === 'batch-one')!
  const targetTeam = data.units.find(unit => unit.kind === 'TEAM')!
  data = await repository.changePersonJob(promotionTarget.id, { jobTitle: '团队负责人', orgUnitId: targetTeam.id, supervisorId: staff.id, roles: ['TEAM_LEAD'] }, promotionTarget.version)
  const promoted = data.people.find(person => person.id === promotionTarget.id)!
  assert.equal(promoted.loginName, 'batch-one'); assert.equal(promoted.idNumber, promotionTarget.idNumber)
  assert.deepEqual(promoted.roles, ['TEAM_LEAD'])
  assert.equal((await repository.personnelChanges(promoted.id))[0].kind, 'JOB_CHANGE')
  const departureTarget = data.people.find(person => person.loginName === 'batch-two')!
  const departureDate = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' })
  data = await repository.departPerson(departureTarget.id, departureDate, departureTarget.version)
  const departed = data.people.find(person => person.id === departureTarget.id)!
  assert.equal(departed.employmentStatus, 'DEPARTED'); assert.equal(departed.status, 'DISABLED')
  assert.ok(!(await repository.credentialHandoffs()).some(account => account.personId === departed.id && account.initialPassword))
  assert.equal((await repository.personnelChanges(departed.id))[0].kind, 'DEPARTURE')
  const successorPassword = previewPassword ?? randomUUID()
  data = await repository.savePerson({ displayName: '接任员工', loginName: 'new-manager', jobTitle: '员工', orgUnitId: department.id, supervisorId: staff.id, roles: ['EMPLOYEE'], ...personnel }, undefined, undefined, successorPassword)
  const successor = data.people.find(person => person.loginName === 'new-manager')!
  const formerManager = data.people.find(person => person.id === staff.id)!
  data = await repository.replaceManager(formerManager.id, successor.id, formerManager.version, successor.version, '部门负责人', '员工')
  assert.deepEqual(data.people.find(p => p.id === formerManager.id)!.roles, ['EMPLOYEE'])
  assert.ok(data.people.find(p => p.id === successor.id)!.roles.includes('DEPARTMENT_MANAGER'))
  assert.equal(data.people.find(p => p.id === promotionTarget.id)!.supervisorId, successor.id)
  assert.equal(data.people.find(p => p.id === formerManager.id)!.supervisorId, successor.id)
  const itemKey = randomUUID(), command = { title: '真实联调事项', content: '<script>纯文本正文</script>', displayOrder: 1 }
  data = await repository.saveCompanyItem(command, undefined, undefined, itemKey)
  data = await repository.saveCompanyItem(command, undefined, undefined, itemKey)
  assert.equal(data.companyItems.length, 1)
  const important = data.companyItems[0]
  data = await repository.saveCompanyItem({ ...command, title: '修改后的事项' }, important.id, important.version)
  data = (await repository.getWorkspace())!
  assert.equal(data.tags.length, 0); assert.equal(data.enterprise.name, 'Web 联调公司更新')
  assert.equal(data.people.find(person => person.id === staff.id)?.displayName, '更新人员')
  assert.equal(data.people.find(person => person.id === staff.id)?.loginName, 'zhang.san')
  assert.ok(data.people.find(person => person.id === successor.id)?.roles.includes('DEPARTMENT_MANAGER'))
  assert.equal(data.companyItems[0].title, '修改后的事项')
  assert.equal(data.companyItems[0].publisherUserId, data.currentUserId)
  await assert.rejects(auth.changePassword('wrong-current-password', randomUUID()), (error: unknown) => error instanceof ApiError && error.status >= 400)
  assert.ok(sessions.read(), '错误当前密码不能清除有效会话')
  const newPassword = randomUUID(), oldToken = sessions.read()!.accessToken
  await auth.changePassword(password, newPassword)
  assert.equal(sessions.read(), null)
  const revoked = createApiClient({ baseUrl: `${origin}/api/v1`, getAccessToken: () => oldToken })
  await assert.rejects(revoked.request('/me'), (error: unknown) => error instanceof ApiError && error.status === 401)
  await auth.login({ enterpriseDomain: domain, loginName: 'admin', password: newPassword })
  await auth.logout(); assert.equal(sessions.read(), null)
  console.log('真实 Spring + H2 + Vite + Web repository 联调通过：人员创建与资料持久化、权限隔离、角色资料保留、组织类型转换及删除、员工密码重置与旧会话撤销、CSV 批量创建、标签关闭、事项幂等与版本、改密及退出。')
  if (process.argv.includes('--preview')) {
    // Disposable random account in a loopback-only database, solely for browser QA.
    console.log(JSON.stringify({ previewUrl: origin, enterpriseDomain: domain, loginName: 'admin', temporaryPassword: newPassword }))
    await new Promise<void>(resolve => { process.once('SIGINT', resolve); process.once('SIGTERM', resolve) })
  }
} finally {
  await vite?.close()
  if (backend.exitCode === null && backend.pid) {
    const exited = new Promise<void>(resolve => backend.once('exit', () => resolve()))
    backend.kill()
    await exited
  }
}
