import type { Person } from '../data/types.ts'

export function PersonStatusBadge({ person }: { person: Person }) {
  const label = person.employmentStatus === 'DEPARTED' ? '已离职' : person.status === 'DISABLED' ? '账号停用' : null
  return label ? <span className="person-status-badge">{label}</span> : null
}
