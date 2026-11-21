import type { AdminData, Person, Role } from './types.ts'
import { roleLabels } from './types.ts'

const leadershipRoles: Role[] = ['FOUNDER', 'DEPARTMENT_MANAGER', 'TEAM_LEAD']

export function managerRole(person: Pick<Person, 'roles'>): Role | undefined {
  return leadershipRoles.find(role => person.roles.includes(role))
}

export function validateLeadershipAssignment(
  people: readonly Pick<Person, 'id' | 'orgUnitId' | 'roles' | 'employmentStatus'>[],
  roles: readonly Role[],
  organization: string,
  personId?: string,
) {
  const previous = people.find(person => person.id === personId)
  for (const role of leadershipRoles) {
    const samePosition = (org: string) => role === 'FOUNDER' || org === organization
    if (!roles.includes(role) || (previous?.roles.includes(role) && samePosition(previous.orgUnitId))) continue
    if (people.some(person => person.id !== personId && person.employmentStatus !== 'DEPARTED' &&
        person.roles.includes(role) && samePosition(person.orgUnitId))) {
      throw new Error(`该${role === 'FOUNDER' ? '公司' : role === 'DEPARTMENT_MANAGER' ? '部门' : '团队'}已有${roleLabels[role]}，请使用“更换负责人”。`)
    }
  }
}
export function departureBlocker(data: AdminData, person: Person): string | null {
  const reports = data.people.filter(p => p.supervisorId === person.id && p.employmentStatus !== 'DEPARTED')
  if (reports.length) return `请先更换负责人或调整直属下属（${reports.length} 人）。`
  const role = managerRole(person)
  if (!role) return null
  const needed = role === 'FOUNDER' ? data.units.some(unit => unit.kind !== 'COMPANY')
    : data.units.some(unit => unit.parentId === person.orgUnitId) || data.people.some(p => p.id !== person.id && p.orgUnitId === person.orgUnitId && p.employmentStatus !== 'DEPARTED')
  const successor = data.people.some(p => p.id !== person.id && p.employmentStatus !== 'DEPARTED' && p.status !== 'DISABLED' && p.roles.includes(role) && (role === 'FOUNDER' || p.orgUnitId === person.orgUnitId))
  return needed && !successor ? '该组织需要保留负责人，请先更换负责人。' : null
}
export function replacementCandidates(data: AdminData, person: Person) {
  const role = managerRole(person)
  if (!role) return []
  return data.people.filter(p => {
    if (p.id === person.id || p.id === data.currentUserId || p.employmentStatus === 'DEPARTED' || p.status === 'DISABLED') return false
    const sameRole = managerRole(p) === role && p.orgUnitId === person.orgUnitId
    const employee = !managerRole(p) && p.roles.includes('EMPLOYEE') && !data.people.some(report => report.supervisorId === p.id && report.employmentStatus !== 'DEPARTED')
    return sameRole || employee
  })
}
