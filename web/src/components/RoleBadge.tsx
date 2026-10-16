import { roleLabels } from '../data/types.ts'
import type { Role } from '../data/types.ts'

export function RoleBadge({ role }: { role: Role }) {
  return <span className="badge role-badge" data-role={role}>{roleLabels[role]}</span>
}
