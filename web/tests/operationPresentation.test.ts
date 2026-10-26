import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { hasReportedSnapshotVersions, reportedHistory, presentOperationComparison, presentOperationValue, operationOrganizationPath } from '../src/data/operationPresentation.ts'
import type { OperationDetail } from '../src/data/operationHistory.ts'
import { currentOrganizationMembers } from '../src/data/organizationHierarchy.ts'

test('organization membership excludes departed people but retains disabled in-service people', () => {
  const data = createSeed(), person = data.people.find(p => p.orgUnitId === 'org-product')!
  const departed = { ...person, id: 'departed', employmentStatus: 'DEPARTED' as const }
  const disabled = { ...person, id: 'disabled', status: 'DISABLED' as const, employmentStatus: 'IN_SERVICE' as const }
  const people = [...data.people, departed, disabled]
  const members = currentOrganizationMembers(people, 'org-product')
  assert.equal(members.some(p => p.id === departed.id), false)
  assert.equal(members.some(p => p.id === disabled.id), true)
  assert.equal(people.length, data.people.length + 2)
})

test('departure snapshots show Chinese labels, states, dates and current directory references', () => {
  const data = createSeed()
  const before = { version: 0, fields: [
    { key: 'employmentStatus', label: 'employmentStatus', value: 'IN_SERVICE' },
    { key: 'departureDate', label: 'departureDate', value: null },
    { key: 'orgUnitId', label: 'orgUnitId', value: 'org-product' },
    { key: 'supervisorUserId', label: 'supervisorUserId', value: 'u-founder' },
    { key: 'roles', label: 'roles', value: '[DEPARTMENT_MANAGER, EMPLOYEE]' },
  ] }
  const after = { version: 1, fields: before.fields.map(field => ({ ...field, value: field.key === 'employmentStatus' ? 'DEPARTED' : field.key === 'departureDate' ? '2026-10-23' : field.value })) }
  const result = presentOperationComparison(before, after, data)
  assert.deepEqual(result.find(f => f.key === 'employmentStatus'), { key: 'employmentStatus', label: '在职状态', before: '在职', after: '已离职', changed: true, rawBefore: 'IN_SERVICE', rawAfter: 'DEPARTED' })
  assert.equal(result.find(f => f.key === 'departureDate')?.after, '2026年10月23日')
  assert.equal(result.find(f => f.key === 'orgUnitId')?.after, '潘多拉科技 → 产品研发部')
  assert.equal(result.find(f => f.key === 'supervisorUserId')?.after, '潘多拉科技 → 林知远（lin.zhiyuan）')
  assert.equal(result.find(f => f.key === 'roles')?.after, '部门老总、员工')
  assert.equal(before.fields[0].value, 'IN_SERVICE')
})

test('unavailable references keep raw changes distinct and never expose filtered secrets', () => {
  const data = createSeed()
  const before = { version: 1, fields: [{ key: 'orgUnitId', label: 'orgUnitId', value: 'deleted-a' }, { key: 'password', label: 'password', value: 'hidden' }] }
  const after = { version: 2, fields: [{ key: 'orgUnitId', label: 'orgUnitId', value: 'deleted-b' }] }
  const [field] = presentOperationComparison(before, after, data)
  assert.equal(field.before, field.after)
  assert.equal(field.changed, true)
  assert.equal(field.rawBefore, 'deleted-a')
  assert.equal(presentOperationComparison(before, after, data).length, 1)
  assert.equal(presentOperationValue('supervisorUserId', null, data), null)
  assert.equal(presentOperationValue('title', 'ACTIVE', data), 'ACTIVE')
  assert.equal(presentOperationValue('roles', '["FOUNDER"]', data), '创始人')
})

test('placeholder zero versions are not presented as real history and valid initial zero is retained', () => {
  const snapshot = { version: 0, fields: [] }
  assert.equal(hasReportedSnapshotVersions({ before: snapshot, after: snapshot }), false)
  assert.equal(hasReportedSnapshotVersions({ before: snapshot, after: { ...snapshot, version: 1 } }), true)
  const detail = { id: 'current', versions: [{ logId: 'current', version: 0 }, { logId: 'older', version: 0 }] } as OperationDetail
  assert.deepEqual(reportedHistory(detail).map(version => version.logId), ['older'])
})

test('positions use historical roles once and organization references include department and team', () => {
  const data = createSeed()
  const before = { version: 1, fields: [{ key: 'jobTitle', label: 'jobTitle', value: '项目统筹' }, { key: 'roles', label: 'roles', value: '[DEPARTMENT_MANAGER]' }] }
  const after = { version: 2, fields: [{ key: 'jobTitle', label: 'jobTitle', value: 'Web开发' }, { key: 'roles', label: 'roles', value: '[EMPLOYEE]' }] }
  const result = presentOperationComparison(before, after, data)
  assert.equal(result.length, 1)
  assert.equal(result[0].label, '职位 / 角色')
  assert.equal(result[0].before, '部门老总'); assert.equal(result[0].after, '员工')
  assert.equal(result[0].changed, true)
  assert.equal(operationOrganizationPath('org-web', data), '潘多拉科技 → 产品研发部 → Web 开发组')
  assert.equal(presentOperationValue('recipientUserId', 'u-cy', data), '潘多拉科技 → 产品研发部 → Web 开发组 → 程瑶（cheng.yao）')
  assert.equal(presentOperationValue('jobTitle', '项目统筹', data), '该历史记录未提供业务角色')
  assert.equal(before.fields[0].value, '项目统筹')
  data.units.find(unit => unit.id === 'org-product')!.parentId = 'org-web'
  assert.match(operationOrganizationPath('org-web', data), /组织层级异常/)
})
