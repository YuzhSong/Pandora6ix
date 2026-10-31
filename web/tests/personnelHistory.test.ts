import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { followingOrganization, personnelHistoryValue } from '../src/data/personnelHistory.ts'
import { groupPersonnel } from '../src/data/personnelGroups.ts'
import type { PersonnelGroup } from '../src/data/personnelGroups.ts'

test('transfer follows supervisor organization only for ordinary employees', () => {
  const data = createSeed(), supervisor = data.people[0]
  assert.equal(followingOrganization(data, ['EMPLOYEE'], supervisor.id), supervisor.orgUnitId || null)
  assert.equal(followingOrganization(data, ['TEAM_LEAD'], supervisor.id), null)
  assert.equal(followingOrganization(data, ['EMPLOYEE'], 'missing'), null)
})

test('captured names survive renames; legacy references are explicitly marked', () => {
  const data = createSeed(), person = data.people[0]
  assert.equal(personnelHistoryValue(data, 'supervisorUserId', { supervisorUserId: person.id, supervisorName: '当时姓名' }), '当时姓名')
  assert.match(personnelHistoryValue(data, 'supervisorUserId', { supervisorUserId: person.id }), /当前目录参考/)
  assert.match(personnelHistoryValue(data, 'orgUnitId', { orgUnitId: 'deleted' }), /原始标识：deleted/)
  assert.equal(personnelHistoryValue(data, 'orgUnitId', { orgUnitId: null }), '未分配')
  assert.equal(personnelHistoryValue(data, 'status', {}), '当时未保存此项')
})

test('transferred employee moves to supervisor group while retaining employee role', () => {
  const data = createSeed(), employee = data.people.find(p => p.roles.length === 1 && p.roles[0] === 'EMPLOYEE')!
  const supervisor = data.people.find(p => p.roles.includes('DEPARTMENT_MANAGER') && p.orgUnitId !== employee.orgUnitId)!
  const oldOrg = employee.orgUnitId
  employee.orgUnitId = followingOrganization(data, employee.roles, supervisor.id)!
  employee.supervisorId = supervisor.id
  const flatten = (groups: PersonnelGroup[]): PersonnelGroup[] => groups.flatMap(group => [group, ...flatten(group.children)])
  const groups = flatten(groupPersonnel(data, data.people, 'organization'))
  assert.ok(groups.find(group => group.id === 'unit-' + supervisor.orgUnitId)?.people.some(p => p.id === employee.id))
  assert.ok(!groups.find(group => group.id === 'unit-' + oldOrg)?.people.some(p => p.id === employee.id))
  assert.ok(groupPersonnel(data, data.people, 'role').find(group => group.id === 'role-EMPLOYEE')?.people.some(p => p.id === employee.id))
})
