import assert from 'node:assert/strict'
import test from 'node:test'
import { businessRank, reportingError, supervisorCandidates } from '../src/data/supervisors.ts'
import type { Person, Role } from '../src/data/types.ts'

function person(id: string, roles: Role[], supervisorId: string | null = null): Person {
  return { id, roles, supervisorId, displayName: id, loginName: id, jobTitle: '', orgUnitId: 'unit', version: 0, status: 'ACTIVE' }
}
const people = [person('admin', ['ADMIN']), person('founder', ['FOUNDER']), person('manager', ['DEPARTMENT_MANAGER'], 'founder'), person('lead', ['TEAM_LEAD'], 'manager'), person('staff', ['EMPLOYEE'], 'lead')]
const ids = (subject: string, roles: Role[]) => supervisorCandidates(people, subject, roles).map(person => person.id)

test('supervisor candidates differ by highest business role, ignoring administrator permission', () => {
  assert.deepEqual(ids('manager', ['DEPARTMENT_MANAGER']), ['founder'])
  assert.deepEqual(ids('lead', ['TEAM_LEAD']), ['founder', 'manager'])
  assert.deepEqual(ids('staff', ['EMPLOYEE']), ['founder', 'manager', 'lead'])
  assert.deepEqual(ids('founder', ['FOUNDER']), [])
  assert.deepEqual(ids('admin', ['ADMIN']), [])
  assert.equal(businessRank(['ADMIN', 'TEAM_LEAD', 'DEPARTMENT_MANAGER']), 2)
})

test('self, disabled users, descendants and existing cycles are never offered', () => {
  const data = [person('self', ['EMPLOYEE']), person('descendant', ['FOUNDER'], 'self'), { ...person('disabled', ['FOUNDER']), status: 'DISABLED' as const }, person('loop1', ['DEPARTMENT_MANAGER'], 'loop2'), person('loop2', ['TEAM_LEAD'], 'loop1'), person('safe', ['FOUNDER'])]
  assert.deepEqual(supervisorCandidates(data, 'self', ['EMPLOYEE']).map(person => person.id), ['safe'])
})

test('role changes cannot retain a junior supervisor or invalidate direct reports', () => {
  assert.match(reportingError(people, 'manager', ['DEPARTMENT_MANAGER'], 'lead'), /层级/)
  assert.match(reportingError(people, 'manager', ['EMPLOYEE'], ''), /直属下属/)
  assert.equal(reportingError(people, 'staff', ['TEAM_LEAD'], 'manager'), '')
  assert.match(reportingError(people, 'staff', ['TEAM_LEAD'], 'lead'), /层级/)
  assert.match(reportingError(people, 'founder', ['FOUNDER'], 'manager'), /层级/)
})


test('departed people are excluded from supervisor choices even with a legacy active flag', () => {
  const departed = { ...person('departed', ['FOUNDER']), employmentStatus: 'DEPARTED' as const }
  assert.deepEqual(supervisorCandidates([departed, person('safe', ['FOUNDER'])], undefined, ['EMPLOYEE']).map(p => p.id), ['safe'])
})
