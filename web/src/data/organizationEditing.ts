import type { AdminData, UnitInput, UnitKind } from './types.ts'

export function organizationDeletionBlocker(data: AdminData, id: string): string | null {
  const unit = data.units.find(unit => unit.id === id)
  if (!unit) return '该组织已不存在，请刷新资料。'
  if (unit.kind === 'COMPANY') return '公司根组织不能删除。'
  if (data.units.some(unit => unit.parentId === id)) return '请先调整或删除下级组织，再删除此组织。'
  if (data.people.some(person => person.orgUnitId === id)) return '请先转移全部直属人员（包括停用账号），再删除此组织。'
  return null
}

export function organizationParents(data: AdminData, kind: UnitKind, id?: string) {
  const excluded = new Set(id ? [id] : [])
  let changed = true
  while (changed) {
    changed = false
    for (const unit of data.units) if (unit.parentId && excluded.has(unit.parentId) && !excluded.has(unit.id)) { excluded.add(unit.id); changed = true }
  }
  return data.units.filter(unit => !excluded.has(unit.id) && unit.kind === (kind === 'TEAM' ? 'DEPARTMENT' : 'COMPANY'))
}

export function validateOrganizationEdit(data: AdminData, input: UnitInput, id?: string) {
  const old = data.units.find(unit => unit.id === id)
  if (input.kind === 'COMPANY' || old?.kind === 'COMPANY') throw new Error('公司根组织不能变更类型或上级，请通过公司信息维护名称。')
  if (!organizationParents(data, input.kind, id).some(parent => parent.id === input.parentId)) throw new Error('请选择有效的上级组织，部门归属公司，团队归属部门，不能选择自身或下级。')
  if (old && old.kind !== input.kind) {
    const children = data.units.filter(unit => unit.parentId === id)
    if (children.length) throw new Error('该组织仍有下级组织，请先调整下级归属后再变更类型。')
    const incompatibleRole = input.kind === 'TEAM' ? 'DEPARTMENT_MANAGER' : 'TEAM_LEAD'
    if (data.people.some(person => person.orgUnitId === id && person.roles.includes(incompatibleRole))) throw new Error('该组织有与新类型不匹配的负责人角色，请先调整人员角色。')
  }
}
