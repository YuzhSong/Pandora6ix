import { roleLabels } from './types.ts'
import type { AdminData, Person, Role } from './types.ts'
import { organizationHierarchy } from './organizationHierarchy.ts'
import type { OrgUnit } from './types.ts'
export const roleOrder: Role[] = ['ADMIN', 'FOUNDER', 'DEPARTMENT_MANAGER', 'TEAM_LEAD', 'EMPLOYEE']
export function primaryRole(person: Person): Role { return roleOrder.find(role => person.roles.includes(role)) ?? 'EMPLOYEE' }
export interface PersonnelGroup {
  id: string; name: string; people: Person[]; rank: number
  total: number; children: PersonnelGroup[]
  unitKind?: OrgUnit['kind']
}
export function groupPersonnel(data: AdminData, people: Person[], mode: 'organization' | 'role') {
  const sortPeople = (members: Person[]) => [...members].sort((a, b) => roleOrder.indexOf(primaryRole(a)) - roleOrder.indexOf(primaryRole(b)) || a.displayName.localeCompare(b.displayName, 'zh-CN') || a.id.localeCompare(b.id))
  if (mode === 'organization') {
    const admins = people.filter(person => primaryRole(person) === 'ADMIN')
    const members = people.filter(person => primaryRole(person) !== 'ADMIN')
    const hierarchy = organizationHierarchy(data.units)
    function makeGroups(nodes: typeof hierarchy): PersonnelGroup[] {
      return nodes.flatMap(({ unit, children }, rank) => {
        const direct = sortPeople(members.filter(person => person.orgUnitId === unit.id))
        const nested = makeGroups(children)
        const total = direct.length + nested.reduce((count, group) => count + group.total, 0)
        return total ? [{ id: 'unit-' + unit.id, name: unit.name, unitKind: unit.kind, people: direct, total, children: nested, rank }] : []
      })
    }
    const result = makeGroups(hierarchy)
    if (admins.length) result.unshift({ id: 'role-ADMIN', name: roleLabels.ADMIN, people: sortPeople(admins), total: admins.length, children: [], rank: -1 })
    const unitIds = new Set(data.units.map(unit => unit.id))
    const unassigned = sortPeople(members.filter(person => !unitIds.has(person.orgUnitId)))
    if (unassigned.length) result.push({ id: 'unit-unassigned', name: '未分配组织', people: unassigned, total: unassigned.length, children: [], rank: result.length })
    return result
  }
  const groups = new Map<string, { id: string; name: string; people: Person[]; rank: number }>()
  for (const person of people) {
    const role = primaryRole(person)
    const id = 'role-' + role
    const name = roleLabels[role]
    const rank = roleOrder.indexOf(role)
    if (!groups.has(id)) groups.set(id, { id, name, rank, people: [] })
    groups.get(id)!.people.push(person)
  }
  return [...groups.values()].sort((a, b) => a.rank - b.rank || a.name.localeCompare(b.name, 'zh-CN')).map((group): PersonnelGroup => {
    return { ...group, people: sortPeople(group.people), total: group.people.length, children: [] }
  })
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
