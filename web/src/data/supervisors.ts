import type { Person, Role } from './types.ts'

// ADMIN grants directory maintenance, not a business management level.
export function businessRank(roles: Role[]): number {
  if (roles.includes('FOUNDER')) return 3
  if (roles.includes('DEPARTMENT_MANAGER')) return 2
  if (roles.includes('TEAM_LEAD')) return 1
  return roles.includes('EMPLOYEE') ? 0 : -1
}

export function canHaveSupervisor(roles: Role[]): boolean {
  return !roles.includes('ADMIN') && !roles.includes('FOUNDER') && businessRank(roles) >= 0
}

export function supervisorCandidates(people: Person[], subjectId: string | undefined, roles: Role[]): Person[] {
  const rank = businessRank(roles)
  if (!canHaveSupervisor(roles)) return []
  const byId = new Map(people.map(person => [person.id, person]))
  return people.filter(candidate => {
    if (candidate.id === subjectId || candidate.status === 'DISABLED' || candidate.employmentStatus === 'DEPARTED' || businessRank(candidate.roles) <= rank) return false
    let current: Person | undefined = candidate
    const visited = new Set<string>()
    while (current) {
      if (current.id === subjectId || visited.has(current.id)) return false
      visited.add(current.id)
      current = current.supervisorId ? byId.get(current.supervisorId) : undefined
    }
    return true
  }).sort((a, b) => businessRank(b.roles) - businessRank(a.roles) || a.displayName.localeCompare(b.displayName, 'zh-CN'))
}

export function supervisorHint(roles: Role[]): string {
  const rank = businessRank(roles)
  if (rank === 3) return '创始人是最高业务职级，无需设置直属上级。'
  if (rank === 2) return '部门负责人只能选择创始人作为直属上级。'
  if (rank === 1) return '团队负责人可以选择部门负责人或创始人。'
  if (rank === 0) return '普通员工可以选择团队负责人、部门负责人或创始人。'
  return '系统管理员属于管理权限，不代表业务职级；仅有此角色时无需设置业务上级。'
}

export function reportingError(people: Person[], subjectId: string | undefined, roles: Role[], supervisorId: string): string {
  if (supervisorId && !supervisorCandidates(people, subjectId, roles).some(person => person.id === supervisorId)) return '当前直属上级不符合所选角色的层级要求，请重新选择或设为未设置。'
  if (subjectId) {
    const rank = businessRank(roles)
    const incompatible = people.filter(person => person.supervisorId === subjectId && (rank <= businessRank(person.roles) || rank < 1))
    if (incompatible.length) return `角色变更后不能继续管理这些直属下属：${incompatible.map(person => person.displayName).join('、')}。请先调整他们的直属上级。`
  }
  return ''
}
