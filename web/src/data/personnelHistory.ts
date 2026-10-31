import type { AdminData, Role } from './types.ts'
import { roleLabels } from './types.ts'
import { organizationPath } from './personnelDisplay.ts'

export const personnelHistoryFields: Record<string, string> = { displayName: '姓名', loginName: '登录账号', jobTitle: '职位', roles: '角色', orgUnitId: '所属组织', supervisorUserId: '直属上级', employmentStatus: '在职状态', departureDate: '离职日期', status: '账号状态' }

export function personnelHistoryValue(data: AdminData, key: string, snapshot: Record<string, unknown>): string {
  const value = snapshot[key]
  if (value === undefined) return '当时未保存此项'
  if (key === 'roles' && Array.isArray(value)) return value.map(role => roleLabels[role as Role] ?? role).join('、') || '无'
  if (key === 'orgUnitId' || key === 'supervisorUserId') {
    if (!value) return key === 'orgUnitId' ? '未分配' : '未设置'
    const name = snapshot[key === 'orgUnitId' ? 'orgUnitName' : 'supervisorName']
    if (typeof name === 'string' && name) return name
    const reference = key === 'orgUnitId' ? (data.units.some(unit => unit.id === value) ? organizationPath(data, String(value)) : null) : data.people.find(person => person.id === value)?.displayName
    return reference ? `${reference}（当前目录参考，历史名称未保存）` : `原始标识：${String(value)}（历史名称未保存）`
  }
  if (key === 'employmentStatus') return value === 'DEPARTED' ? '已离职' : value === 'IN_SERVICE' ? '在职' : '—'
  if (key === 'status') return value === 'DISABLED' ? '已停用' : value === 'ACTIVE' ? '已启用' : '—'
  return typeof value === 'string' && value ? value : '—'
}

export function followingOrganization(data: AdminData, roles: Role[], supervisorId: string): string | null {
  if (!roles.includes('EMPLOYEE') || roles.some(role => role !== 'EMPLOYEE')) return null
  return data.people.find(person => person.id === supervisorId)?.orgUnitId || null
}
