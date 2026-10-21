import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { departureBlocker, replacementCandidates } from '../src/data/personnelResponsibility.ts'

test('departure is blocked by required organization responsibility even without direct reports', () => {
  const data = createSeed(), founder = data.people.find(p => p.roles.includes('FOUNDER'))!
  data.people = data.people.map(p => ({ ...p, supervisorId: null }))
  assert.match(departureBlocker(data, founder)!, /负责人/)
  const department = data.people.find(p => p.roles.includes('DEPARTMENT_MANAGER'))!
  assert.match(departureBlocker(data, department)!, /负责人/)
  data.people.push({ ...founder, id: 'next-founder', status: 'ACTIVE', employmentStatus: 'IN_SERVICE' })
  assert.equal(departureBlocker(data, founder), null)
})

test('replacement candidates exclude departed, disabled, self, current admin and unrelated managers', () => {
  const data = createSeed(), manager = data.people.find(p => p.roles.includes('DEPARTMENT_MANAGER'))!
  const ordinary = data.people.find(p => p.roles.length === 1 && p.roles.includes('EMPLOYEE'))!
  data.people.push({ ...ordinary, id: 'departed', employmentStatus: 'DEPARTED' }, { ...ordinary, id: 'disabled', status: 'DISABLED' })
  const candidates = replacementCandidates(data, manager)
  assert.ok(candidates.some(p => p.id === ordinary.id))
  assert.ok(candidates.every(p => p.id !== manager.id && p.id !== data.currentUserId && p.id !== 'departed' && p.id !== 'disabled'))
  assert.ok(!candidates.some(p => p.roles.includes('FOUNDER')))
})
