import assert from 'node:assert/strict'
import test from 'node:test'
import { organizationCounts, organizationPath, canRestoreAccount, disableReason } from '../src/data/personnelDisplay.ts'
import { createSeed } from '../src/data/seed.ts'

test('counts distinguish direct and descendants, exclude departed, retain in-service disabled and tolerate cycles', () => {
  const data = createSeed(), base = data.people[0]
  data.units = [{ id: 'c', name: '公司', parentId: null, kind: 'COMPANY', version: 0 }, { id: 'd', name: '研发部', parentId: 'c', kind: 'DEPARTMENT', version: 0 }, { id: 't', name: 'Web组', parentId: 'd', kind: 'TEAM', version: 0 }]
  data.people = [
    { ...base, id: '1', orgUnitId: 'd', employmentStatus: 'IN_SERVICE' },
    { ...base, id: '2', orgUnitId: 't', employmentStatus: 'IN_SERVICE', status: 'DISABLED' },
    { ...base, id: '3', orgUnitId: 't', employmentStatus: 'IN_SERVICE' },
    { ...base, id: '4', orgUnitId: 't', employmentStatus: 'DEPARTED' },
  ]
  assert.deepEqual(organizationCounts(data, 'd'), { direct: 1, inclusive: 3 })
  assert.equal(organizationPath(data, 't'), '公司 / 研发部 / Web组')
  data.units[0].parentId = 't'
  assert.equal(organizationCounts(data, 'c').inclusive, 3)
  assert.equal(organizationPath(data, 't'), '公司 / 研发部 / Web组')
})

test('only explicitly in-service disabled accounts are eligible; missing reasons are not fabricated', () => {
  const base = createSeed().people[0]
  assert.equal(canRestoreAccount({ ...base, employmentStatus: 'IN_SERVICE', status: 'DISABLED' }), true)
  for (const employmentStatus of ['DEPARTED', undefined] as const) assert.equal(canRestoreAccount({ ...base, employmentStatus, status: 'DISABLED' }), false)
  assert.equal(canRestoreAccount({ ...base, employmentStatus: 'IN_SERVICE', status: 'ACTIVE' }), false)
  assert.equal(disableReason({ ...base, employmentStatus: 'DEPARTED' }), '办理离职后停用')
  assert.equal(disableReason({ ...base, employmentStatus: 'IN_SERVICE' }), '未记录')
})
