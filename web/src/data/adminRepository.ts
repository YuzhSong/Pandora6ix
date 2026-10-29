import { createSeed } from './seed.ts'
import { organizationDeletionBlocker } from './organizationEditing.ts'
import { roleLabels } from './types.ts'
import type { AdminData, AdminRepository, CompanyItemInput, PersonInput, TagInput, UnitInput } from './types.ts'

export const DATA_KEY = 'pandora-admin-demo-v1'
export const CATALOG_KEY = 'pandora-enterprise-catalog-v1'
export const BINDING_KEY = 'pandora-admin-company-id-v1'
interface EnterpriseCatalog { schemaVersion: 1; activeId: string; workspaces: AdminData[] }
export interface StorageAdapter { getItem(key: string): string | null; setItem(key: string, value: string): void }

function requireText(value: string, label: string, max: number) {
  if (typeof value !== 'string' || !value.trim()) throw new Error(`请填写${label}。`)
  if (value.trim().length > max) throw new Error(`${label}最多 ${max} 个字符。`)
  return value.trim()
}

export function assertNoCycle(id: string, parentId: string | null, links: Map<string, string | null>, label: string) {
  const seen = new Set([id])
  let cursor = parentId
  while (cursor) {
    if (seen.has(cursor)) throw new Error(`${label}不能指向自身或形成循环关系。`)
    if (!links.has(cursor)) throw new Error(`${label}不存在，请重新选择。`)
    seen.add(cursor)
    cursor = links.get(cursor) ?? null
  }
}

function checkVersion(item: { version: number } | undefined, expected: number | undefined) {
  if (!item) throw new Error('该记录已不存在，请刷新后重试。')
  if (item.version !== expected) throw new Error('记录已被其他页面更新。请取消编辑并刷新后重试，当前内容尚未保存。')
}

function validateData(data: AdminData) {
  if (data.schemaVersion !== 2 || !Array.isArray(data.units) || !Array.isArray(data.people) || !Array.isArray(data.tags) || !Array.isArray(data.companyItems)) throw new Error('数据格式不兼容。')
  requireText(data.enterprise.name, '企业名称', 128)
  const roots = data.units.filter(unit => unit.kind === 'COMPANY')
  if (roots.length !== 1 || roots[0].parentId !== null) throw new Error('企业必须有一个根组织。')
  const orgLinks = new Map(data.units.map(unit => [unit.id, unit.parentId]))
  const userLinks = new Map(data.people.map(person => [person.id, person.supervisorId]))
  if (orgLinks.size !== data.units.length || userLinks.size !== data.people.length || new Set(data.tags.map(tag => tag.id)).size !== data.tags.length) throw new Error('数据包含重复 ID。')
  if (!userLinks.has(data.currentUserId)) throw new Error('当前账号资料不存在。')
  if (new Set(data.companyItems.map(item => item.id)).size !== data.companyItems.length) throw new Error('事项包含重复 ID。')
  for (const item of [data.enterprise, ...data.units, ...data.people, ...data.tags, ...data.companyItems]) {
    if (!item.id || !Number.isSafeInteger(item.version) || item.version < 1) throw new Error('记录版本或 ID 无效。')
  }
  for (const unit of data.units) {
    requireText(unit.name, '组织名称', 128)
    assertNoCycle(unit.id, unit.parentId, orgLinks, '上级组织')
    const parent = data.units.find(item => item.id === unit.parentId)
    if (unit.kind !== 'COMPANY' && !(unit.kind === 'DEPARTMENT' && parent?.kind === 'COMPANY') && !(unit.kind === 'TEAM' && parent?.kind === 'DEPARTMENT')) throw new Error('部门须归属公司，团队须归属部门。')
  }
  const logins = new Set<string>()
  for (const person of data.people) {
    requireText(person.displayName, '姓名', 128)
    const login = requireText(person.loginName, '登录名', 128).toLowerCase()
    if (logins.has(login)) throw new Error('登录名已存在，请使用其他登录名。')
    logins.add(login)
    if (!orgLinks.has(person.orgUnitId)) throw new Error('请选择有效的所属组织。')
    if (!Array.isArray(person.roles) || !person.roles.length || person.roles.some(role => !Object.hasOwn(roleLabels, role))) throw new Error('请至少选择一个有效角色。')
    assertNoCycle(person.id, person.supervisorId, userLinks, '直属上级')
  }
  for (const tag of data.tags) {
    requireText(tag.name, '标签名称', 64)
    if (!/^#[\da-f]{6}$/i.test(tag.colorHex)) throw new Error('标签颜色须为 #RRGGBB 格式，例如 #4F70E8。')
  }
  for (const item of data.companyItems) {
    requireText(item.title, '事项标题', 255)
    if (typeof item.content !== 'string') throw new Error('事项正文格式无效。')
    if (!Number.isInteger(item.displayOrder) || item.displayOrder < -2147483648 || item.displayOrder > 2147483647) throw new Error('显示顺序须为有效整数。')
    if (!userLinks.has(item.publisherUserId)) throw new Error('事项发布者不存在。')
  }
}

function migrate(data: AdminData): AdminData {
  if (Number(data.schemaVersion) !== 1) return data
  const next: AdminData = { ...data, schemaVersion: 2, currentUserId: 'u-admin', companyItems: [] }
  if (['潘多拉科技（演示企业）', '潘多拉科技（Web 演示）'].includes(next.enterprise.name)) next.enterprise = { ...next.enterprise, name: '潘多拉科技', version: next.enterprise.version + 1 }
  next.people = next.people.map(person => {
    if (person.id !== 'u-admin') return person
    const displayName = person.displayName === '演示管理员' ? '管理员' : person.displayName
    const loginName = person.loginName === 'admin.demo' && !next.people.some(other => other.id !== person.id && other.loginName.toLowerCase() === 'admin') ? 'admin' : person.loginName
    return displayName !== person.displayName || loginName !== person.loginName ? { ...person, displayName, loginName, version: person.version + 1 } : person
  })
  return next
}

export function createLocalAdminRepository(storage: StorageAdapter): AdminRepository {
  let boundEnterpriseId: string | null = null
  function readLegacy(): AdminData {
    let raw: string | null
    try { raw = storage.getItem(DATA_KEY) } catch { throw new Error('无法读取资料，请检查浏览器存储权限后重试。') }
    if (!raw) return createSeed()
    try {
      const data = migrate(JSON.parse(raw) as AdminData)
      validateData(data)
      return data
    } catch { throw new Error('资料格式不兼容或已损坏，请联系维护人员处理。') }
  }
  function catalog(): EnterpriseCatalog | null {
    let raw: string | null
    try { raw = storage.getItem(CATALOG_KEY) } catch { throw new Error('无法读取企业工作空间。') }
    if (!raw) return null
    try {
      const value = JSON.parse(raw) as EnterpriseCatalog
      if (value.schemaVersion !== 1 || !Array.isArray(value.workspaces) || !value.workspaces.length || new Set(value.workspaces.map(item => item.enterprise.id)).size !== value.workspaces.length || !value.workspaces.some(item => item.enterprise.id === value.activeId)) throw Error()
      value.workspaces.forEach(validateData)
      return value
    } catch { throw new Error('企业工作空间资料已损坏，请联系维护人员处理。') }
  }
  function read(): AdminData {
    const value = catalog()
    if (!value) return readLegacy()
    let companyId: string | null
    try { companyId = boundEnterpriseId ?? storage.getItem(BINDING_KEY) } catch { throw new Error('无法读取所属公司。') }
    companyId ??= value.activeId
    const data = value.workspaces.find(item => item.enterprise.id === companyId)
    if (!data) throw new Error('所属公司资料不存在，请联系维护人员处理。')
    try { if (storage.getItem(BINDING_KEY) !== companyId) storage.setItem(BINDING_KEY, companyId) } catch { throw new Error('无法保存所属公司，请检查存储权限。') }
    boundEnterpriseId = companyId
    return data
  }
  function readForWrite(): AdminData {
    const data = read()
    if (boundEnterpriseId && boundEnterpriseId !== data.enterprise.id) throw new Error('当前企业已切换，请刷新后重新操作。')
    return data
  }
  function writeCatalog(value: EnterpriseCatalog): void {
    value.workspaces.forEach(validateData)
    try { storage.setItem(CATALOG_KEY, JSON.stringify(value)) } catch { throw new Error('保存失败：企业工作空间存储不可用或已满。') }
  }
  function write(data: AdminData): AdminData {
    validateData(data)
    const value = catalog()
    if (value) {
      if (boundEnterpriseId !== data.enterprise.id) throw new Error('当前企业已切换，请刷新后重新操作。')
      writeCatalog({ ...value, workspaces: value.workspaces.map(item => item.enterprise.id === data.enterprise.id ? data : item) })
    } else {
      try { storage.setItem(DATA_KEY, JSON.stringify(data)) } catch { throw new Error('保存失败：浏览器存储不可用或已满。输入内容已保留，请检查存储权限。') }
    }
    return data
  }
  function id(prefix: string) { return `${prefix}-${crypto.randomUUID()}` }
  return {
    async load() { const data = read(); boundEnterpriseId = data.enterprise.id; return data },
    async getWorkspace() {
      let legacy: string | null
      try { legacy = storage.getItem(DATA_KEY) } catch { throw new Error('无法读取资料，请检查浏览器存储权限。') }
      if (!catalog() && !legacy) return null
      const data = read(); boundEnterpriseId = data.enterprise.id; return data
    },
    async initializeEnterprise(name, administratorName) {
      let legacy: string | null
      try { legacy = storage.getItem(DATA_KEY) } catch { throw new Error('无法读取资料，请检查浏览器存储权限。') }
      if (catalog() || legacy) throw new Error('当前工作台已归属公司，不能重复创建企业。')
      const companyName = requireText(name, '公司名称', 128)
      const displayName = requireText(administratorName, '管理员姓名', 128)
      const rootId = id('org'), personId = id('user')
      const data: AdminData = {
        schemaVersion: 2, currentUserId: personId,
        enterprise: { id: id('enterprise'), name: companyName, version: 1 },
        units: [{ id: rootId, name: companyName, kind: 'COMPANY', parentId: null, version: 1 }],
        people: [{ id: personId, displayName, loginName: 'admin', jobTitle: '系统管理员', orgUnitId: rootId, supervisorId: null, roles: ['ADMIN'], version: 1 }],
        tags: [], companyItems: [],
      }
      const next = write(data); boundEnterpriseId = next.enterprise.id; return next
    },
    async saveEnterprise(name, expectedVersion) {
      const data = readForWrite()
      checkVersion(data.enterprise, expectedVersion)
      data.enterprise = { ...data.enterprise, name: requireText(name, '企业名称', 128), version: data.enterprise.version + 1 }
      data.units = data.units.map(unit => unit.kind === 'COMPANY' ? { ...unit, name: data.enterprise.name, version: unit.version + 1 } : unit)
      return write(data)
    },
    async saveUnit(input: UnitInput, unitId, expectedVersion) {
      const data = readForWrite()
      const old = data.units.find(item => item.id === unitId)
      if (unitId) checkVersion(old, expectedVersion)
      if (old?.kind === 'COMPANY' && (input.kind !== 'COMPANY' || input.parentId !== null)) throw new Error('公司根组织不能变更类型或上级。')
      const unit = { id: unitId ?? id('org'), kind: input.kind, parentId: input.parentId, name: requireText(input.name, '组织名称', 128), version: (old?.version ?? 0) + 1 }
      data.units = old ? data.units.map(item => item.id === unitId ? unit : item) : [...data.units, unit]
      if (old?.kind === 'COMPANY') data.enterprise = { ...data.enterprise, name: unit.name, version: data.enterprise.version + 1 }
      return write(data)
    },
    async deleteUnit(unitId, expectedVersion) {
      const data = readForWrite()
      checkVersion(data.units.find(unit => unit.id === unitId), expectedVersion)
      const blocker = organizationDeletionBlocker(data, unitId)
      if (blocker) throw new Error(blocker)
      data.units = data.units.filter(unit => unit.id !== unitId)
      return write(data)
    },
    async savePerson(input: PersonInput, personId, expectedVersion) {
      const data = readForWrite()
      const old = data.people.find(item => item.id === personId)
      if (personId) checkVersion(old, expectedVersion)
      const person = { id: personId ?? id('user'), orgUnitId: input.orgUnitId, supervisorId: input.supervisorId, displayName: requireText(input.displayName, '姓名', 128), loginName: requireText(input.loginName, '登录名', 128), jobTitle: input.jobTitle.trim(), roles: [...new Set(input.roles)], version: (old?.version ?? 0) + 1 }
      if (person.jobTitle.length > 128) throw new Error('职位最多 128 个字符。')
      data.people = old ? data.people.map(item => item.id === personId ? person : item) : [...data.people, person]
      return write(data)
    },
    async credentialHandoffs() { throw new Error('请连接真实后端。') },
    async restoreAccount() { throw new Error('请连接真实后端。') },
    async departPerson() { throw new Error('请连接真实后端办理离职。') },
    async replaceManager() { throw new Error('请连接真实后端更换负责人。') },
    async changePersonJob() { throw new Error('请连接真实后端调整岗位。') },
    async personnelChanges() { throw new Error('请连接真实后端查看人员变动。') },
    async resetPersonPassword() { throw new Error('本地演示资料不包含登录密码，请连接真实后端后操作。') },
    async saveRoles(personId, roles, expectedVersion) {
      const data = readForWrite()
      const old = data.people.find(person => person.id === personId)
      checkVersion(old, expectedVersion)
      data.people = data.people.map(person => person.id === personId ? { ...person, roles: [...new Set(roles)], version: person.version + 1 } : person)
      return write(data)
    },
    async saveTag(input: TagInput, tagId, expectedVersion) {
      const data = readForWrite()
      const old = data.tags.find(item => item.id === tagId)
      if (tagId) checkVersion(old, expectedVersion)
      const tag = { id: tagId ?? id('tag'), name: requireText(input.name, '标签名称', 64), colorHex: input.colorHex.trim().toUpperCase(), version: (old?.version ?? 0) + 1 }
      data.tags = old ? data.tags.map(item => item.id === tagId ? tag : item) : [...data.tags, tag]
      return write(data)
    },
    async saveCompanyItem(input: CompanyItemInput, itemId, expectedVersion) {
      const data = readForWrite()
      const old = data.companyItems.find(item => item.id === itemId)
      if (itemId) checkVersion(old, expectedVersion)
      const item = { id: itemId ?? id('important'), title: requireText(input.title, '事项标题', 255), content: input.content, displayOrder: input.displayOrder, publisherUserId: old?.publisherUserId ?? data.currentUserId, version: (old?.version ?? 0) + 1 }
      data.companyItems = old ? data.companyItems.map(existing => existing.id === itemId ? item : existing) : [...data.companyItems, item]
      data.companyItems.sort((a, b) => a.displayOrder - b.displayOrder || a.id.localeCompare(b.id))
      return write(data)
    },
  }
}

// Only this adapter uses browser storage. Replace it with an authenticated HTTP adapter later.
export const adminRepository = createLocalAdminRepository({
  getItem: key => window.localStorage.getItem(key),
  setItem: (key, value) => window.localStorage.setItem(key, value),
})
