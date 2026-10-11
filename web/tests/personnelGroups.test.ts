import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { belongsToOrganization, groupPersonnel } from '../src/data/personnelGroups.ts'

test('organization grouping separates administrators and places team members under their department', () => {
  const data = createSeed()
  const groups = groupPersonnel(data, data.people, 'organization')
  assert.equal(groups[0].id, 'role-ADMIN')
  const product = groups.find(group => group.id === 'unit-org-product')!
  assert.equal(product.people[0].id, 'u-product')
  assert.ok(product.people.some(person => person.orgUnitId === 'org-web'))
  assert.equal(groups.flatMap(group => group.people).length, data.people.length)
})

test('role grouping uses highest configured role rather than job-title text', () => {
  const data = createSeed()
  data.people[0].roles = ['EMPLOYEE', 'ADMIN']
  data.people.find(person => person.id === 'u-cy')!.jobTitle = '总经理'
  const groups = groupPersonnel(data, data.people, 'role')
  assert.equal(groups[0].id, 'role-ADMIN')
  assert.ok(groups.find(group => group.id === 'role-EMPLOYEE')!.people.some(person => person.id === 'u-cy'))
  assert.equal(groups.flatMap(group => group.people).length, data.people.length)
})

test('organization filtering includes nested teams but excludes other departments', () => {
  const data = createSeed()
  const person = data.people.find(person => person.id === 'u-cy')!
  assert.equal(belongsToOrganization(data, person, 'org-company'), true)
  assert.equal(belongsToOrganization(data, person, 'org-product'), true)
  assert.equal(belongsToOrganization(data, person, 'org-operations'), false)
})
