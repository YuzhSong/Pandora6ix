import assert from 'node:assert/strict'
import test from 'node:test'
import { organizationFilterOptions } from '../src/data/organizationHierarchy.ts'
import { createSeed } from '../src/data/seed.ts'
import { groupPersonnel } from '../src/data/personnelGroups.ts'

test('organization filter orders company, department and its teams together despite flat server ordering', () => {
  const data = createSeed()
  const options = organizationFilterOptions([...data.units].reverse())
  assert.equal(options[0].value, 'org-company')
  assert.equal(options[0].depth, 0)
  const departmentIndex = options.findIndex(option => option.value === 'org-product')
  const teams = options.slice(departmentIndex + 1, departmentIndex + 3)
  assert.equal(options[departmentIndex].depth, 1)
  assert.deepEqual(new Set(teams.map(team => team.value)), new Set(['org-web', 'org-mobile']))
  assert.ok(teams.every(team => team.depth === 2))
  assert.equal(options[departmentIndex].symbol, '●')
  assert.ok(teams.every(team => team.symbol === '○'))
  assert.equal(teams.find(team => team.value === 'org-web')?.accessibleLabel, '潘多拉科技 / 产品研发部 / Web 开发组')
  assert.equal(options.length, data.units.length)
})

test('empty units remain selectable and orphan or cyclic legacy nodes do not loop or disappear', () => {
  const data = createSeed()
  data.units.push({ id: 'empty', name: '空团队', kind: 'TEAM', parentId: 'org-product', version: 1 })
  data.units.push({ id: 'orphan', name: '孤立组织', kind: 'DEPARTMENT', parentId: 'missing', version: 1 })
  data.units.push({ id: 'a', name: '循环甲', kind: 'DEPARTMENT', parentId: 'b', version: 1 })
  data.units.push({ id: 'b', name: '循环乙', kind: 'DEPARTMENT', parentId: 'a', version: 1 })
  const options = organizationFilterOptions(data.units)
  assert.equal(options.length, data.units.length)
  assert.equal(new Set(options.map(option => option.value)).size, data.units.length)
  assert.equal(options.find(option => option.value === 'empty')?.depth, 2)
})

test('unassigned personnel are preserved separately and a company without direct personnel still wraps departments', () => {
  const data = createSeed()
  const employee = data.people.find(person => person.id === 'u-cy')!
  const groups = groupPersonnel(data, [employee, { ...employee, id: 'unassigned', orgUnitId: '' }], 'organization')
  assert.equal(groups[0].id, 'unit-org-company')
  assert.equal(groups[0].people.length, 0)
  assert.equal(groups[0].children[0].id, 'unit-org-product')
  assert.equal(groups[1].id, 'unit-unassigned')
  assert.equal(groups[1].people[0].id, 'unassigned')
})
