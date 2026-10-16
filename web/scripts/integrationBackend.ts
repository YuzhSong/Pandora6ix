import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
import { randomUUID } from 'node:crypto'
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

// Uses an unchanged copy of backend source and a fresh database under web/.integration.
// No team server, team credentials, or original backend output is touched.
const revision = execFileSync('git', ['log', '-1', '--format=%h', '--', 'backend'], { cwd: path.resolve('..'), encoding: 'utf8' }).trim()
const jar = path.resolve(`.integration/backend-${revision}/target/backend-0.0.1-SNAPSHOT.jar`)
if (!existsSync(jar)) throw new Error('请先按 README 在 .integration/backend 构建后端原样副本。')
const probe = createNetServer()
await new Promise<void>(resolve => probe.listen(0, '127.0.0.1', resolve))
const port = (probe.address() as AddressInfo).port
await new Promise<void>(resolve => probe.close(() => resolve()))
const runtime = path.resolve('.integration/runs', randomUUID())
await mkdir(runtime, { recursive: true })
const bootstrap = randomUUID(), password = randomUUID(), domain = `web-${randomUUID()}`
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java.exe') : 'java'
const backend = spawn(java, ['-jar', jar, `--server.port=${port}`, '--server.address=127.0.0.1'], {
  cwd: runtime, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true,
  env: { ...process.env, PANDORA_DB_URL: 'jdbc:h2:file:./pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0', PANDORA_DB_USER: 'sa', PANDORA_DB_PASSWORD: '', PANDORA_BOOTSTRAP_TOKEN: bootstrap },
})
let spawnError: Error | undefined
backend.on('error', error => { spawnError = error })
// Drain JVM logs without printing credentials, responses, or internal diagnostics.
backend.stdout.on('data', () => undefined); backend.stderr.on('data', () => undefined)
let vite: Awaited<ReturnType<typeof createViteServer>> | undefined
try {
  let healthy = false
  for (let attempt = 0; attempt < 120; attempt++) {
    if (spawnError) throw spawnError
    if (backend.exitCode !== null) throw new Error('后端测试进程提前退出。')
    try { healthy = ((await (await fetch(`http://127.0.0.1:${port}/health`)).json()) as { status?: unknown }).status === 'UP' } catch { /* Startup retry. */ }
    if (healthy) break
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  assert.ok(healthy, '真实后端应启动成功')
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
  const initialized = await auth.initialize(initialization, bootstrap, key)
  assert.deepEqual(await auth.initialize(initialization, bootstrap, key), initialized, '重复初始化应幂等')
  await auth.login({ enterpriseDomain: domain, loginName: 'admin', password })
  let data = (await repository.getWorkspace())!
  assert.equal(data.people.length, 1); assert.equal(data.enterprise.version, 0)
  const oldVersion = data.enterprise.version
  data = await repository.saveEnterprise('Web 联调公司更新', oldVersion)
  await assert.rejects(repository.saveEnterprise('冲突覆盖', oldVersion), (error: unknown) => error instanceof ApiError && error.status === 409)
  data = await repository.saveUnit({ name: '研发部', kind: 'DEPARTMENT', parentId: data.units[0].id })
  const department = data.units.find(unit => unit.name === '研发部')!
  data = await repository.saveUnit({ name: 'Web 团队', kind: 'TEAM', parentId: department.id })
  data = await repository.savePerson({ displayName: '测试负责人', loginName: 'manager', jobTitle: '负责人', orgUnitId: data.units[0].id, supervisorId: null, roles: ['FOUNDER'] }, undefined, undefined, randomUUID())
  const manager = data.people.find(person => person.loginName === 'manager')!
  const staffPassword = randomUUID()
  data = await repository.savePerson({ displayName: '测试人员', loginName: 'staff', jobTitle: '前端', orgUnitId: department.id, supervisorId: manager.id, roles: ['EMPLOYEE'] }, undefined, undefined, staffPassword)
  await assert.rejects(auth.login({ enterpriseDomain: domain, loginName: 'staff', password: staffPassword }), (error: unknown) => error instanceof ApiError && error.status === 403)
  assert.ok(sessions.read(), '拒绝普通员工登录不能清除已有管理员身份')
  let staff = data.people.find(person => person.loginName === 'staff')!
  data = await repository.savePerson({ ...staff, displayName: '更新人员' }, staff.id, staff.version)
  staff = data.people.find(person => person.id === staff.id)!
  data = await repository.saveRoles(staff.id, ['EMPLOYEE', 'DEPARTMENT_MANAGER'], staff.version)
  const tag = { name: '共享标签', colorHex: '#112233' }
  data = await repository.saveTag(tag)
  const firstTag = data.tags.find(value => value.name === tag.name)!
  data = await repository.saveTag({ ...tag, colorHex: '#445566' }, firstTag.id, firstTag.version)
  // Exercise real seek pagination with >100 results.
  for (let index = 0; index < 101; index++) await client.request('/tags', { method: 'POST', body: { name: `分页标签 ${index}`, colorHex: '#112233' } })
  const itemKey = randomUUID(), command = { title: '真实联调事项', content: '<script>纯文本正文</script>', displayOrder: 1 }
  data = await repository.saveCompanyItem(command, undefined, undefined, itemKey)
  data = await repository.saveCompanyItem(command, undefined, undefined, itemKey)
  assert.equal(data.companyItems.length, 1)
  const important = data.companyItems[0]
  data = await repository.saveCompanyItem({ ...command, title: '修改后的事项' }, important.id, important.version)
  data = (await repository.getWorkspace())!
  assert.equal(data.tags.length, 102); assert.equal(data.enterprise.name, 'Web 联调公司更新')
  assert.equal(data.people.find(person => person.id === staff.id)?.displayName, '更新人员')
  assert.ok(data.people.find(person => person.id === staff.id)?.roles.includes('DEPARTMENT_MANAGER'))
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
  console.log('真实 Spring + H2 + Vite + Web repository 联调通过：初始化幂等、认证、企业/组织/人员/角色、102 标签分页、事项幂等与版本修改、刷新持久化、409、改密及退出撤销。')
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
