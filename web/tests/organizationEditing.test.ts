import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { organizationParents, validateOrganizationEdit } from '../src/data/organizationEditing.ts'

test('a leaf team can become a department; a department with children cannot become a team', () => {
  const data = createSeed()
  const root = data.units.find(unit => unit.kind === 'COMPANY')!
  const team = data.units.find(unit => unit.kind === 'TEAM')!
  data.people = []
  assert.doesNotThrow(() => validateOrganizationEdit(data, { name: team.name, kind: 'DEPARTMENT', parentId: root.id }, team.id))
  assert.throws(() => validateOrganizationEdit(data, { name: '团队', kind: 'TEAM', parentId: data.units.find(unit => unit.kind === 'DEPARTMENT' && unit.id !== team.parentId)!.id }, team.parentId!), /下级组织/)
})

test('root changes, cyclic parents and manager-role mismatches are rejected', () => {
  const data = createSeed()
  const root = data.units.find(unit => unit.kind === 'COMPANY')!
  const team = data.units.find(unit => unit.kind === 'TEAM')!
  assert.throws(() => validateOrganizationEdit(data, { name: root.name, kind: 'DEPARTMENT', parentId: root.id }, root.id), /根组织/)
  assert.throws(() => validateOrganizationEdit(data, { name: '部门', kind: 'DEPARTMENT', parentId: team.id }, team.id), /有效的上级/)
  data.people = [{ ...data.people[0], orgUnitId: team.id, roles: ['TEAM_LEAD'] }]
  assert.throws(() => validateOrganizationEdit(data, { name: team.name, kind: 'DEPARTMENT', parentId: root.id }, team.id), /负责人角色/)
  assert.ok(organizationParents(data, 'TEAM', root.id).length === 0)
})
