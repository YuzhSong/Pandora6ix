import { pinyin } from 'pinyin-pro'
import type { AdminData, Person, Role, UnitKind } from './types.ts'
import { roleLabels } from './types.ts'
import { managerRole } from './personnelResponsibility.ts'

export const businessRoles: Role[] = ['FOUNDER', 'DEPARTMENT_MANAGER', 'TEAM_LEAD', 'EMPLOYEE']
export const businessRoleOptions = businessRoles.map(value => ({ value, label: roleLabels[value] }))

export function defaultPersonnelRole(units: AdminData['units'], people: readonly Pick<Person, 'orgUnitId' | 'roles' | 'employmentStatus'>[], organization: string): Role {
  const vacantDepartment = units.some(unit => unit.id === organization && unit.kind === 'DEPARTMENT') &&
    !people.some(person => person.orgUnitId === organization && person.employmentStatus !== 'DEPARTED' && person.roles.includes('DEPARTMENT_MANAGER'))
  return vacantDepartment ? 'DEPARTMENT_MANAGER' : 'EMPLOYEE'
}

export function generateLoginName(name: string, people: readonly { loginName: string }[]): string {
  const existing = new Set(people.map(person => person.loginName.trim().toLowerCase()))
  const base = pinyin(name.trim(), { toneType: 'none', surname: 'head', v: true }).toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 120)
  if (!base) return ''
  let candidate = base, suffix = 2
  while (existing.has(candidate)) candidate = `${base}${suffix++}`
  return candidate
}
export function personnelRole(value: string): Role {
  const aliases: Record<string, Role> = { 部门负责人: 'DEPARTMENT_MANAGER', 团队负责人: 'TEAM_LEAD', 普通员工: 'EMPLOYEE' }
  const role = businessRoleOptions.find(option => option.value === value || option.label === value)?.value ?? (Object.hasOwn(aliases, value) ? aliases[value] : undefined)
  if (!value) return 'EMPLOYEE'
  if (!role) throw new Error('请选择创始人、部门老总、团队长或员工')
  return role
}
export function organizationRole(roles: Role[]): Role {
  return managerRole({ roles }) ?? (roles.includes('EMPLOYEE') ? 'EMPLOYEE' : 'ADMIN')
}

export function organizationAllowsRole(role: Role, kind?: UnitKind) {
  if (role === 'EMPLOYEE' || role === 'TEAM_LEAD') return kind === 'TEAM'
  if (role === 'DEPARTMENT_MANAGER') return kind === 'DEPARTMENT'
  if (role === 'FOUNDER') return kind === 'COMPANY'
  return kind !== 'DEPARTMENT'
}

export function validateRoleOrganization(data: AdminData, role: Role, org: string) {
  const kind = data.units.find(unit => unit.id === org)?.kind
  if (!organizationAllowsRole(role, kind)) {
    throw new Error(role === 'EMPLOYEE' ? '普通员工必须归属具体团队，请选择所属团队。' : '请选择与业务角色匹配的公司、部门或团队。')
  }
}

export function generateInitialPassword(): string {
  const groups = ['0123456789']
  const alphabet = groups.join('')
  function random(max: number) {
    const bytes = new Uint8Array(1), limit = 256 - 256 % max
    do { crypto.getRandomValues(bytes) } while (bytes[0] >= limit)
    return bytes[0] % max
  }
  const chars = groups.map(group => group[random(group.length)])
  while (chars.length < 12) chars.push(alphabet[random(alphabet.length)])
  for (let i = chars.length - 1; i > 0; i--) { const j = random(i + 1); [chars[i], chars[j]] = [chars[j], chars[i]] }
  return chars.join('')
}
// Internal drafts keep the generated password at index 2; it is never a table or CSV column.
export function populatedPersonnelRow(row: string[]) { return row.some((value, index) => index !== 2 && index !== 6 && !!value.trim()) }
export function preparePersonnelRow(row: string[]): string[] {
  return populatedPersonnelRow(row) && !row[2] ? row.map((value, index) => index === 2 ? generateInitialPassword() : value) : row
}
