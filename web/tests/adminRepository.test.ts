import assert from 'node:assert/strict'
import test from 'node:test'
import { createLocalAdminRepository, DATA_KEY, CATALOG_KEY, BINDING_KEY } from '../src/data/adminRepository.ts'
import type { StorageAdapter } from '../src/data/adminRepository.ts'
import { createSeed } from '../src/data/seed.ts'

test('local adapter deletes only empty leaves with current version and persists removal', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  let data = await repository.load()
  const root = data.units.find(unit => unit.kind === 'COMPANY')!
  await assert.rejects(repository.deleteUnit(root.id, root.version), /根组织/)
  data = await repository.saveUnit({ name: '待删空部门', kind: 'DEPARTMENT', parentId: root.id })
  const leaf = data.units.find(unit => unit.name === '待删空部门')!
  await assert.rejects(repository.deleteUnit(leaf.id, leaf.version - 1))
  await repository.deleteUnit(leaf.id, leaf.version)
  assert.ok(!(await repository.load()).units.some(unit => unit.id === leaf.id))
})

test('legacy migration preserves custom records and persists only on save', async () => {
  const storage = memoryStore()
  const legacy = { ...createSeed(), schemaVersion: 1, companyItems: undefined, currentUserId: undefined }
  legacy.enterprise.name = '潘多拉科技（演示企业）'
  legacy.people[0].displayName = '演示管理员'
  legacy.people[0].loginName = 'admin.demo'
  const raw = JSON.stringify(legacy)
  storage.setItem(DATA_KEY, raw)
  const repository = createLocalAdminRepository(storage)
  const migrated = await repository.load()
  assert.equal(migrated.schemaVersion, 2)
  assert.equal(migrated.enterprise.name, '潘多拉科技')
  assert.equal(migrated.people[0].displayName, '管理员')
  assert.equal(storage.getItem(DATA_KEY), raw)
  await repository.saveCompanyItem({ title: '安排', content: '', displayOrder: 0 })
  assert.equal((await repository.load()).companyItems.length, 1)
  legacy.enterprise.name = '自定义公司'
  legacy.people[0].displayName = '我的姓名'
  storage.setItem(DATA_KEY, JSON.stringify(legacy))
  assert.equal((await repository.load()).enterprise.name, '自定义公司')
  assert.equal((await repository.load()).people[0].displayName, '我的姓名')
})

test('important items preserve text, publisher and order while rejecting invalid or stale writes', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  const first = await repository.saveCompanyItem({ title: '较后', content: '<b>说明</b>\n第二行', displayOrder: 9 })
  const item = first.companyItems[0]
  await repository.saveCompanyItem({ title: '较前', content: '', displayOrder: 1 })
  const edited = await repository.saveCompanyItem({ title: item.title, content: item.content, displayOrder: 2 }, item.id, item.version)
  assert.deepEqual(edited.companyItems.map(item => item.title), ['较前', '较后'])
  assert.equal(edited.companyItems[1].publisherUserId, item.publisherUserId)
  assert.equal(edited.companyItems[1].content, '<b>说明</b>\n第二行')
  await assert.rejects(repository.saveCompanyItem({ title: '覆盖', content: '', displayOrder: 0 }, item.id, item.version), /其他页面更新/)
  await assert.rejects(repository.saveCompanyItem({ title: ' ', content: '', displayOrder: 0 }), /事项标题/)
  await assert.rejects(repository.saveCompanyItem({ title: '错误', content: '', displayOrder: .5 }), /整数/)
  await assert.rejects(repository.saveCompanyItem({ title: '错误', content: '', displayOrder: 2147483648 }), /整数/)
  assert.deepEqual(await repository.load(), edited)
})

function memoryStore(): StorageAdapter {
  const values = new Map<string, string>()
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => { values.set(key, value) } }
}

test('creates personnel without passwords and preserves independent organization and supervisor after reload', async () => {
  const storage = memoryStore()
  const repository = createLocalAdminRepository(storage)
  const data = await repository.savePerson({ displayName: '新成员', loginName: 'new.member', jobTitle: '开发', orgUnitId: 'org-web', supervisorId: 'u-product', roles: ['EMPLOYEE'] })
  const saved = await createLocalAdminRepository(storage).load()
  assert.deepEqual(saved, data)
  assert.equal(saved.people.at(-1)?.supervisorId, 'u-product')
  assert.equal(storage.getItem(DATA_KEY)?.includes('password'), false)
})

test('rejects self and descendant supervisor cycles without changing saved data', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  const initial = await repository.load()
  const product = initial.people.find(person => person.id === 'u-product')!
  for (const supervisorId of ['u-product', 'u-cy']) {
    await assert.rejects(repository.savePerson({ ...product, supervisorId }, product.id, product.version), /循环/)
  }
  assert.deepEqual(await repository.load(), initial)
})

test('rejects organization cycles and invalid company-department-team placement', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  const data = await repository.load()
  const department = data.units.find(unit => unit.id === 'org-product')!
  await assert.rejects(repository.saveUnit({ ...department, parentId: 'org-web' }, department.id, department.version), /循环/)
  await assert.rejects(repository.saveUnit({ name: '错误团队', kind: 'TEAM', parentId: 'org-company' }), /归属/)
})

test('rejects normalized duplicate login names and empty or unknown roles', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  const person = (await repository.load()).people[0]
  await assert.rejects(repository.savePerson({ ...person, loginName: ' ADMIN ' }), /登录名已存在/)
  await assert.rejects(repository.savePerson({ ...person, roles: [] }, person.id, person.version), /角色/)
  // Exercise an invalid value arriving from storage or a future adapter.
  await assert.rejects(repository.savePerson({ ...person, roles: ['ROOT' as never] }, person.id, person.version), /角色/)
})

test('stale edits in another repository cannot overwrite a newer version', async () => {
  const storage = memoryStore()
  const first = createLocalAdminRepository(storage)
  const second = createLocalAdminRepository(storage)
  const tag = (await first.load()).tags[0]
  await second.saveTag({ ...tag, name: '新的名称' }, tag.id, tag.version)
  await assert.rejects(first.saveTag({ ...tag, name: '陈旧编辑' }, tag.id, tag.version), /其他页面更新/)
  assert.equal((await first.load()).tags[0].name, '新的名称')
})

test('invalid color is rejected while equal tag names remain allowed', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  await assert.rejects(repository.saveTag({ name: '测试', colorHex: '#GG0000' }), /颜色/)
  const tag = (await repository.load()).tags[0]
  const result = await repository.saveTag({ name: tag.name, colorHex: '#123abc' })
  assert.equal(result.tags.at(-1)?.colorHex, '#123ABC')
})

test('storage write failure is reported and no successful mutation is returned', async () => {
  const repository = createLocalAdminRepository({ getItem: () => null, setItem: () => { throw new Error('quota') } })
  await assert.rejects(repository.saveTag({ name: '测试', colorHex: '#123456' }), /保存失败/)
  assert.equal((await repository.load()).tags.length, 5)
})

test('corrupted local data reports an error rather than silently replacing it', async () => {
  const repository = createLocalAdminRepository({ getItem: () => '{broken', setItem: () => {} })
  await assert.rejects(repository.load(), /已损坏/)
})


test('role-only updates preserve profile and reject empty, unknown and stale selections', async () => {
  const repository = createLocalAdminRepository(memoryStore())
  const before = await repository.load()
  const person = before.people.find(person => person.id === 'u-cy')!
  const saved = await repository.saveRoles(person.id, ['EMPLOYEE', 'TEAM_LEAD', 'TEAM_LEAD'], person.version)
  assert.deepEqual(saved.people.find(item => item.id === person.id), { ...person, roles: ['EMPLOYEE', 'TEAM_LEAD'], version: person.version + 1 })
  assert.deepEqual(saved.people.filter(item => item.id !== person.id), before.people.filter(item => item.id !== person.id))
  await assert.rejects(repository.saveRoles(person.id, [], person.version + 1), /角色/)
  await assert.rejects(repository.saveRoles(person.id, ['UNKNOWN' as never], person.version + 1), /角色/)
  await assert.rejects(repository.saveRoles(person.id, ['ADMIN'], person.version), /其他页面更新/)
  assert.deepEqual(await repository.load(), saved)
})

test('initialization creates one company and administrator, persists and cannot repeat', async () => {
  const storage = memoryStore()
  const repository = createLocalAdminRepository(storage)
  assert.equal(await repository.getWorkspace(), null)
  const created = await repository.initializeEnterprise('新公司', '张管理员')
  assert.equal(created.units.length, 1)
  assert.equal(created.units[0].parentId, null)
  assert.equal(created.units[0].name, '新公司')
  assert.equal(created.people.length, 1)
  assert.equal(created.people[0].displayName, '张管理员')
  assert.deepEqual(created.people[0].roles, ['ADMIN'])
  assert.equal(created.people[0].orgUnitId, created.units[0].id)
  assert.deepEqual(created.tags, [])
  assert.deepEqual(created.companyItems, [])
  assert.deepEqual(await createLocalAdminRepository(storage).getWorkspace(), created)
  await assert.rejects(repository.initializeEnterprise('另一公司', '其他管理员'), /不能重复/)
  assert.deepEqual(await repository.getWorkspace(), created)
})

test('initialization validation and storage failure leave an empty workspace', async () => {
  const storage = memoryStore()
  const repository = createLocalAdminRepository({ getItem: storage.getItem, setItem: () => { throw Error('full') } })
  await assert.rejects(repository.initializeEnterprise(' ', '管理员'), /公司名称/)
  await assert.rejects(repository.initializeEnterprise('公司', ' '), /管理员姓名/)
  await assert.rejects(repository.initializeEnterprise('公司', '管理员'), /保存失败/)
  assert.equal(await repository.getWorkspace(), null)
})

test('legacy catalog binds current company across reload and only updates its records', async () => {
  const storage = memoryStore()
  const first = createSeed()
  const other = createSeed()
  other.enterprise.id = 'enterprise-other'
  other.enterprise.name = '另一公司'
  other.units.find(unit => unit.kind === 'COMPANY')!.name = '另一公司'
  storage.setItem(CATALOG_KEY, JSON.stringify({ schemaVersion: 1, activeId: first.enterprise.id, workspaces: [first, other] }))
  const repository = createLocalAdminRepository(storage)
  assert.deepEqual(await repository.getWorkspace(), first)
  assert.equal(storage.getItem(BINDING_KEY), first.enterprise.id)
  storage.setItem(CATALOG_KEY, JSON.stringify({ schemaVersion: 1, activeId: other.enterprise.id, workspaces: [first, other] }))
  const reloaded = createLocalAdminRepository(storage)
  assert.deepEqual(await reloaded.getWorkspace(), first)
  const renamed = await reloaded.saveEnterprise('公司改名', first.enterprise.version)
  assert.equal(renamed.units.find(unit => unit.kind === 'COMPANY')?.name, '公司改名')
  const saved = JSON.parse(storage.getItem(CATALOG_KEY)!)
  assert.deepEqual(saved.workspaces[1], other)
  assert.equal(saved.activeId, other.enterprise.id)
  await assert.rejects(reloaded.initializeEnterprise('第三公司', '管理员'), /不能重复/)
  assert.equal('selectEnterprise' in reloaded, false)
})
