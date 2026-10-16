import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { belongsToOrganization, groupPersonnel } from '../src/data/personnelGroups.ts'

test('organization grouping separates administrators and places team members under their department', () => {
  const data = createSeed()
  const groups = groupPersonnel(data, data.people, 'organization')
  assert.equal(groups[0].id, 'role-ADMIN')
  const company = groups.find(group => group.id === 'unit-org-company')!
  const product = company.children.find(group => group.id === 'unit-org-product')!
  assert.equal(product.people[0].id, 'u-product')
  const web = product.children.find(group => group.id === 'unit-org-web')!
  assert.ok(web.people.some(person => person.id === 'u-cy'))
  assert.ok(product.people.every(person => person.orgUnitId === 'org-product'))
  assert.equal(product.total, product.people.length + product.children.reduce((count, team) => count + team.total, 0))
  const shown = groups.flatMap(group => [...group.people, ...group.children.flatMap(department => [...department.people, ...department.children.flatMap(team => team.people)])])
  assert.equal(shown.length, data.people.length)
  assert.equal(new Set(shown.map(person => person.id)).size, data.people.length)
})

test('role grouping uses highest configured role rather than job-title text', () => {
  const data = createSeed()
  data.people[0].roles = ['EMPLOYEE', 'ADMIN']
  data.people.find(person => person.id === 'u-cy')!.jobTitle = '总经理'
  const groups = groupPersonnel(data, data.people, 'role')
  assert.equal(groups[0].id, 'role-ADMIN')
  assert.ok(groups.find(group => group.id === 'role-EMPLOYEE')!.people.some(person => person.id === 'u-cy'))
  assert.equal(groups.flatMap(group => group.people).length, data.people.length)
  assert.ok(groups.every(group => group.children.length === 0))
})

test('matching team members retain their department path with filtered counts and no unrelated teams', () => {
  const data = createSeed()
  const person = data.people.find(person => person.id === 'u-cy')!
  const groups = groupPersonnel(data, [person], 'organization')
  assert.equal(groups.length, 1)
  assert.equal(groups[0].id, 'unit-org-company')
  assert.equal(groups[0].total, 1)
  assert.equal(groups[0].people.length, 0)
  assert.equal(groups[0].children.length, 1)
  const department = groups[0].children[0]
  assert.equal(department.id, 'unit-org-product')
  assert.equal(department.children[0].id, 'unit-org-web')
  assert.deepEqual(department.children[0].people.map(value => value.id), [person.id])
})

test('a department without direct personnel still owns its teams and team administrators appear only once', () => {
  const data = createSeed()
  const members = data.people.filter(person => person.orgUnitId === 'org-web')
  members[0] = { ...members[0], roles: ['ADMIN', 'EMPLOYEE'] }
  const groups = groupPersonnel(data, members, 'organization')
  assert.equal(groups[0].id, 'role-ADMIN')
  const company = groups.find(group => group.id === 'unit-org-company')!
  const department = company.children.find(group => group.id === 'unit-org-product')!
  assert.equal(company.people.length, 0)
  assert.equal(company.total, members.length - 1)
  assert.equal(department.people.length, 0)
  assert.equal(department.total, members.length - 1)
  assert.equal(department.children[0].total, members.length - 1)
})

test('organization filtering includes nested teams but excludes other departments', () => {
  const data = createSeed()
  const person = data.people.find(person => person.id === 'u-cy')!
  assert.equal(belongsToOrganization(data, person, 'org-company'), true)
  assert.equal(belongsToOrganization(data, person, 'org-product'), true)
  assert.equal(belongsToOrganization(data, person, 'org-operations'), false)
})
