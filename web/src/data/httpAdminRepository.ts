import { ApiError } from '../api/client.ts'
import type { createApiClient } from '../api/client.ts'
import { requireAdmin, validatePassword } from '../api/auth.ts'
import { roleLabels } from './types.ts'
import type { AdminData, AdminRepository, CompanyImportantItem, Enterprise, OrgUnit, Person, Role, UnitKind } from './types.ts'
import { profilePendingMessage, supportsPersonnelProfile, validatePersonnelProfile } from './personnelProfile.ts'
import { validateOrganizationEdit } from './organizationEditing.ts'

type Json = Record<string, unknown>
const invalid = () => new ApiError('服务器返回的管理资料格式不正确，请联系管理员。', 200, 'INVALID_RESPONSE')
function object(value: unknown): Json { if (!value || typeof value !== 'object' || Array.isArray(value)) throw invalid(); return value as Json }
function text(row: Json, key: string): string { if (typeof row[key] !== 'string') throw invalid(); return row[key] as string }
function nullable(row: Json, key: string): string | null { if (row[key] === null) return null; return text(row, key) }
function integer(row: Json, key: string): number { if (!Number.isSafeInteger(row[key])) throw invalid(); return row[key] as number }
function version(row: Json): number { const value = integer(row, 'version'); if (value < 0) throw invalid(); return value }
function enterprise(value: unknown): Enterprise { const row = object(value); return { id: text(row, 'id'), name: text(row, 'name'), version: version(row), loginDomain: text(row, 'loginDomain'), businessTimezone: nullable(row, 'businessTimezone') } }
function unit(value: unknown): OrgUnit {
  const row = object(value), kind = text(row, 'kind')
  if (!['COMPANY', 'DEPARTMENT', 'TEAM'].includes(kind)) throw invalid()
  return { id: text(row, 'id'), name: text(row, 'name'), kind: kind as UnitKind, parentId: nullable(row, 'parentId'), version: version(row) }
}
function person(value: unknown): Person {
  const row = object(value), status = text(row, 'status')
  if (!['ACTIVE', 'DISABLED'].includes(status) || !Array.isArray(row.roles) || !row.roles.length || row.roles.some(role => typeof role !== 'string' || !Object.hasOwn(roleLabels, role))) throw invalid()
  const profile: Pick<Person, 'gender' | 'idNumber' | 'phoneNumber'> = {}
  if (Object.hasOwn(row, 'gender')) {
    const gender = nullable(row, 'gender')
    if (gender !== null && gender !== 'MALE' && gender !== 'FEMALE') throw invalid()
    profile.gender = gender
  }
  if (Object.hasOwn(row, 'idNumber')) profile.idNumber = nullable(row, 'idNumber')
  if (Object.hasOwn(row, 'phoneNumber')) profile.phoneNumber = nullable(row, 'phoneNumber')
  return { id: text(row, 'id'), displayName: text(row, 'displayName'), loginName: text(row, 'loginName'), jobTitle: nullable(row, 'jobTitle') ?? '', orgUnitId: nullable(row, 'orgUnitId') ?? '', supervisorId: nullable(row, 'supervisorUserId'), roles: [...row.roles] as Role[], status: status as Person['status'], version: version(row), ...profile }
}
function item(value: unknown): CompanyImportantItem { const row = object(value); return { id: text(row, 'id'), title: text(row, 'title'), content: text(row, 'content'), displayOrder: integer(row, 'displayOrder'), publisherUserId: text(row, 'publisherUserId'), version: version(row) } }

export function createHttpAdminRepository(client: ReturnType<typeof createApiClient>): AdminRepository & { clear(): void } {
  let snapshot: AdminData | null = null
  let generation = 0
  let loadSerial = 0
  let uncertainPersonnelWrite = false
  const cancelled = () => new ApiError('身份或资料已变化，请重新加载。', 0, 'REQUEST_CANCELLED')
  const current = () => { if (!snapshot) throw new Error('请先登录并加载公司资料。'); return snapshot }
  async function all<T>(path: string, map: (value: unknown) => T): Promise<T[]> {
    const values: T[] = [], seen = new Set<string>()
    let cursor: string | null = null
    do {
      const query = new URLSearchParams({ limit: '100' })
      if (cursor) query.set('cursor', cursor)
      const page = object(await client.request(`${path}?${query}`))
      if (!Array.isArray(page.items) || (page.nextCursor !== null && typeof page.nextCursor !== 'string')) throw invalid()
      values.push(...page.items.map(map))
      cursor = page.nextCursor as string | null
      if (cursor !== null) { if (!cursor || seen.has(cursor)) throw invalid(); seen.add(cursor) }
    } while (cursor !== null)
    return values
  }
  const copy = () => structuredClone(current())
  const replace = <T extends { id: string }>(values: T[], value: T) => values.some(old => old.id === value.id) ? values.map(old => old.id === value.id ? value : old) : [...values, value]
  async function write<T>(path: string, body: unknown, parse: (value: unknown) => T, apply: (data: AdminData, value: T) => void, updating: boolean, idempotencyKey?: string) {
    const before = generation
    current()
    const response = await client.request(path, { method: updating ? 'PATCH' : 'POST', body, headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined })
    if (generation !== before) throw cancelled()
    const value = parse(response)
    apply(current(), value)
    return copy()
  }
  const profileBody = (value: Person) => ({ ...(value.gender !== undefined ? { gender: value.gender } : {}), ...(value.idNumber !== undefined ? { idNumber: value.idNumber } : {}), ...(value.phoneNumber !== undefined ? { phoneNumber: value.phoneNumber } : {}) })
  const userBody = (value: Person, expectedVersion: number, roles = value.roles) => ({ expectedVersion, displayName: value.displayName, jobTitle: value.jobTitle, orgUnitId: value.orgUnitId || null, supervisorUserId: value.supervisorId, roles, status: value.status ?? 'ACTIVE', ...profileBody(value) })
  const verifyPerson = (expected: Partial<Pick<Person, 'gender' | 'idNumber' | 'phoneNumber' | 'loginName'>>) => (value: unknown) => {
    try {
      const result = person(value)
      if (Object.entries(expected).some(([key, field]) => result[key as keyof Person] !== field)) throw invalid()
      return result
    } catch {
      uncertainPersonnelWrite = true
      throw new Error('服务器已处理请求，但未返回一致的人员资料。请关闭窗口并刷新，核实账号及资料后再操作，勿重复创建。')
    }
  }
  const checkPersonnelWrite = () => { if (uncertainPersonnelWrite) throw new Error('上次人员保存结果尚未确认，请先关闭窗口并刷新资料，勿重复提交。') }
  return {
    clear() { generation++; loadSerial++; snapshot = null; uncertainPersonnelWrite = false },
    async getWorkspace() {
      const before = generation, serial = ++loadSerial
      const me = requireAdmin(await client.request('/me'))
      const [company, units, people, companyItems] = await Promise.all([
        client.request('/admin/enterprise').then(enterprise), all('/admin/org-units', unit), all('/admin/users', person), all('/company-important-items', item),
      ])
      if (before !== generation || serial !== loadSerial) throw cancelled()
      if (!people.some(value => value.id === me.id)) throw invalid()
      snapshot = { schemaVersion: 2, currentUserId: me.id, enterprise: company, units, people, tags: [], companyItems }
      uncertainPersonnelWrite = false
      return copy()
    },
    async load() { const data = await this.getWorkspace(); if (!data) throw invalid(); return data },
    async initializeEnterprise() { throw new Error('请使用受控公司初始化流程创建服务端企业。') },
    async saveEnterprise(name, expectedVersion) {
      return write('/admin/enterprise', { name, expectedVersion }, enterprise, (data, value) => { data.enterprise = value }, true)
    },
    async saveUnit(input, id, expectedVersion) {
      validateOrganizationEdit(current(), input, id)
      return write(id ? `/admin/org-units/${encodeURIComponent(id)}` : '/admin/org-units', { ...input, ...(id ? { expectedVersion } : {}) }, unit, (data, value) => { data.units = replace(data.units, value) }, !!id)
    },
    async savePerson(input, id, expectedVersion, initialPassword) {
      checkPersonnelWrite()
      if (!input.loginName.trim() || input.loginName.length > 128 || input.loginName !== input.loginName.trim()) throw new Error('登录名须为 1–128 个字符，且前后不能有空白。')
      const supported = supportsPersonnelProfile(current().people)
      const oldPerson = current().people.find(value => value.id === id)
      const profile = profileBody({ ...oldPerson, ...input, id: id ?? '', version: expectedVersion ?? 0 })
      const profileChanged = Object.entries(profile).some(([key, value]) => value !== oldPerson?.[key as keyof Person])
      if (!supported && (!id || profileChanged)) throw new Error(profilePendingMessage)
      if (!id || profileChanged) validatePersonnelProfile(profile)
      if (id) {
        const old = current().people.find(value => value.id === id)
        if (!old || expectedVersion === undefined) throw invalid()
        const rename = old.loginName !== input.loginName ? { loginName: input.loginName } : {}
        return write(`/admin/users/${encodeURIComponent(id)}`, { ...userBody({ ...old, ...input }, expectedVersion), ...rename }, verifyPerson({ ...profile, ...rename }), (data, value) => { data.people = replace(data.people, value) }, true)
      }
      validatePassword(initialPassword ?? '')
      const body = { loginName: input.loginName, password: initialPassword, displayName: input.displayName, jobTitle: input.jobTitle, orgUnitId: input.orgUnitId || null, supervisorUserId: input.supervisorId, roles: input.roles, ...profile }
      return write('/admin/users', body, verifyPerson(profile), (data, value) => { data.people = replace(data.people, value) }, false)
    },
    async saveRoles(personId, roles, expectedVersion) {
      checkPersonnelWrite()
      const old = current().people.find(value => value.id === personId)
      if (!old) throw invalid()
      return write(`/admin/users/${encodeURIComponent(personId)}`, userBody(old, expectedVersion, roles), verifyPerson(profileBody(old)), (data, value) => { data.people = replace(data.people, value) }, true)
    },
    async saveTag() { throw new Error('管理端不提供标签管理。') },
    async saveCompanyItem(input, id, expectedVersion, idempotencyKey) {
      return write(id ? `/company-important-items/${encodeURIComponent(id)}` : '/company-important-items', { ...input, ...(id ? { expectedVersion } : {}) }, item, (data, value) => { data.companyItems = replace(data.companyItems, value) }, !!id, id ? undefined : idempotencyKey)
    },
  }
}
