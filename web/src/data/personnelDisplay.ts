import type { AdminData, Person } from './types.ts'

export function supervisorOptionLabel(data: Pick<AdminData, 'units'>, person: Person): string {
  const unit = data.units.find(value => value.id === person.orgUnitId)
  const organization = unit?.kind === 'COMPANY' ? unit.name : organizationPath({ units: data.units.filter(value => value.kind !== 'COMPANY') }, person.orgUnitId)
  return `${person.displayName}（${person.loginName}） · ${organization}`
}

export function organizationPath(data: Pick<AdminData, 'units'>, id: string): string {
  const names: string[] = [], seen = new Set<string>()
  let unit = data.units.find(value => value.id === id)
  while (unit && !seen.has(unit.id)) {
    seen.add(unit.id); names.unshift(unit.name)
    unit = data.units.find(value => value.id === unit!.parentId)
  }
  return names.join(' / ') || '未分配'
}

export function organizationCounts(data: Pick<AdminData, 'units' | 'people'>, id: string) {
  const ids = new Set([id]), pending = [id]
  while (pending.length) {
    const parent = pending.pop()
    for (const unit of data.units) if (unit.parentId === parent && !ids.has(unit.id)) { ids.add(unit.id); pending.push(unit.id) }
  }
  const members = data.people.filter(person => person.employmentStatus !== 'DEPARTED' && ids.has(person.orgUnitId))
  return { direct: members.filter(person => person.orgUnitId === id).length, inclusive: members.length }
}

export function canRestoreAccount(person: Person) {
  return person.status === 'DISABLED' && person.employmentStatus === 'IN_SERVICE'
}

export function disableReason(person: Person) {
  if (person.employmentStatus === 'DEPARTED') return '办理离职后停用'
  return '未记录'
}
