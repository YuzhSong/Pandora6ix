import type { OrgUnit } from './types.ts'

interface OrganizationNode { unit: OrgUnit; children: OrganizationNode[] }

/** Order parent before children, including empty units; tolerate disconnected legacy data. */
export function organizationHierarchy(units: OrgUnit[]): OrganizationNode[] {
  const ids = new Set(units.map(unit => unit.id))
  const visited = new Set<string>()
  function visit(unit: OrgUnit): OrganizationNode {
    visited.add(unit.id)
    const children: OrganizationNode[] = []
    for (const child of units.filter(child => child.parentId === unit.id)) {
      if (!visited.has(child.id)) children.push(visit(child))
    }
    return { unit, children }
  }
  const roots = units.filter(unit => !unit.parentId || !ids.has(unit.parentId))
  const result = roots.filter(unit => !visited.has(unit.id)).map(visit)
  for (const unit of units) if (!visited.has(unit.id)) result.push(visit(unit))
  return result
}

export function organizationFilterOptions(units: OrgUnit[]) {
  const options: { value: string; label: string; depth: number; accessibleLabel: string; symbol: string }[] = []
  function append(nodes: OrganizationNode[], depth: number, parents: string[]) {
    for (const { unit, children } of nodes) {
      const path = [...parents, unit.name]
      options.push({ value: unit.id, label: unit.name, depth, accessibleLabel: path.join(' / '), symbol: unit.kind === 'COMPANY' ? '▣' : unit.kind === 'DEPARTMENT' ? '●' : '○' })
      append(children, depth + 1, path)
    }
  }
  append(organizationHierarchy(units), 0, [])
  return options
}
