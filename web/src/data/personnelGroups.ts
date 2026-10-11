import { roleLabels } from './types.ts'
import type { AdminData, Person, Role } from './types.ts'
export const roleOrder: Role[] = ['ADMIN', 'FOUNDER', 'DEPARTMENT_MANAGER', 'TEAM_LEAD', 'EMPLOYEE']
export function primaryRole(person: Person): Role { return roleOrder.find(role => person.roles.includes(role)) ?? 'EMPLOYEE' }
export function groupPersonnel(data: AdminData, people: Person[], mode: 'organization' | 'role') {
  const groups = new Map<string, { id: string; name: string; people: Person[]; rank: number }>()
  for (const person of people) {
    const role = primaryRole(person)
    const unit = data.units.find(unit => unit.id === person.orgUnitId)
    const department = unit?.kind === 'TEAM' ? data.units.find(parent => parent.id === unit.parentId) : unit
    const id = role === 'ADMIN' || mode === 'role' ? 'role-' + role : 'unit-' + department?.id
    const name = role === 'ADMIN' || mode === 'role' ? roleLabels[role] : department?.name ?? '未分配组织'
    const rank = role === 'ADMIN' ? -1 : mode === 'role' ? roleOrder.indexOf(role) : data.units.findIndex(unit => unit.id === department?.id)
    if (!groups.has(id)) groups.set(id, { id, name, rank, people: [] })
    groups.get(id)!.people.push(person)
  }
  return [...groups.values()].sort((a, b) => a.rank - b.rank || a.name.localeCompare(b.name, 'zh-CN')).map(group => ({ ...group, people: group.people.sort((a, b) => roleOrder.indexOf(primaryRole(a)) - roleOrder.indexOf(primaryRole(b)) || a.displayName.localeCompare(b.displayName, 'zh-CN') || a.id.localeCompare(b.id)) }))
}

export function belongsToOrganization(data: AdminData, person: Person, organizationId: string): boolean {
  if (!organizationId) return true
  let unit = data.units.find(unit => unit.id === person.orgUnitId)
  const visited = new Set<string>()
  while (unit && !visited.has(unit.id)) {
    if (unit.id === organizationId) return true
    visited.add(unit.id)
    unit = data.units.find(parent => parent.id === unit!.parentId)
  }
  return false
}
