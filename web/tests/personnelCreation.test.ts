import assert from 'node:assert/strict'
import test from 'node:test'
import { generateInitialPassword, personnelRole, preparePersonnelRow } from '../src/data/personnelCreation.ts'
import { validatePassword } from '../src/api/auth.ts'
import { createSeed } from '../src/data/seed.ts'
import { previewPersonnelTable, importPersonnelTable, bulkPersonnelHeaders } from '../src/data/bulkPersonnel.ts'

test('generated passwords meet policy, differ per person and remain stable during draft edits', () => {
  const passwords = Array.from({ length: 100 }, generateInitialPassword)
  assert.equal(new Set(passwords).size, 100)
  for (const password of passwords) {
    validatePassword(password)
    assert.equal(password.length, 12)
    assert.match(password, /^[0-9]{12}$/)
  }
  const row = preparePersonnelRow(['新员工', '', '', '', '', '', '', '', ''])
  assert.equal(preparePersonnelRow([...row.slice(0, 1), 'account', ...row.slice(2)])[2], row[2])
  assert.equal(preparePersonnelRow(Array(9).fill(''))[2], '')
  assert.ok(previewPersonnelTable([['', '', passwords[0], '', '', '', 'EMPLOYEE', '', '']], createSeed()).errors.length)
})

test('batch leaders require a vacant matching organization and receive its supervisor', () => {
  const data = createSeed()
  const row = ['新人', 'new-role-user', '', '男', '000000200001010001', '00000000001', '团队长', 'org-service', 'cheng.yao']
  const valid = previewPersonnelTable([row], data)
  assert.deepEqual(valid.errors, [])
  assert.deepEqual(valid.rows[0].input.roles, ['TEAM_LEAD'])
  assert.equal(valid.rows[0].input.jobTitle, '团队长')
  assert.equal(valid.rows[0].input.supervisorId, 'u-operations')
  validatePassword(valid.rows[0].initialPassword)
  const wrongOrg = [...row]; wrongOrg[7] = data.units.find(unit => unit.kind === 'COMPANY')!.id
  assert.match(previewPersonnelTable([wrongOrg], data).errors.join(''), /匹配/)
  const occupied = [...row]; occupied[7] = 'org-web'
  assert.match(previewPersonnelTable([occupied], data).errors.join(''), /已有团队长/)
  const duplicate = [...row]; duplicate[1] = 'new-role-user-two'; duplicate[4] = '000000200001010002'
  duplicate[5] = '00000000002'
  assert.match(previewPersonnelTable([row, duplicate], data).errors.join(''), /第 2 行.*已有团队长/)
  for (const invalid of ['项目统筹', 'ADMIN', '__proto__']) assert.throws(() => personnelRole(invalid))
})

test('blank batch role defaults to the first vacant department manager only', () => {
  const data = createSeed()
  data.people = data.people.filter(person => person.id !== 'u-product')
  const rows = [
    ['小红', 'xiaohong', '', '女', '000000200001010001', '00000000001', '', 'org-product', ''],
    ['小绿', 'xiaolv', '', '女', '000000200001010002', '00000000002', '', 'org-product', ''],
  ]
  const preview = previewPersonnelTable(rows, data)
  assert.match(preview.errors.join(''), /第 2 行.*具体团队/)
  assert.deepEqual(preview.rows.map(row => row.input.roles), [['DEPARTMENT_MANAGER']])
  rows[0][6] = '员工'
  rows[0][7] = 'org-web'
  assert.deepEqual(previewPersonnelTable(rows, data).rows.map(row => row.input.roles), [['EMPLOYEE'], ['DEPARTMENT_MANAGER']])
})

test('CSV has eight visible columns, generates hidden passwords and supports the position header alias', () => {
  const data = createSeed()
  const row = ['新人', 'new-csv-user', '男', '000000200001010001', '00000000001', '员工', 'org-web', '']
  const header = [...bulkPersonnelHeaders]; header[5] = '职位'
  const imported = importPersonnelTable(header.join(',') + '\n' + row.join(','), data)
  validatePassword(imported[0][2])
  assert.equal(preparePersonnelRow(imported[0])[2], imported[0][2])
  assert.deepEqual(previewPersonnelTable(imported, data).errors, [])
})

test('imported founder rows cannot retain an invisible supervisor', () => {
  const data = createSeed(), root = data.units.find(unit => unit.kind === 'COMPANY')!
  data.people = data.people.filter(person => !person.roles.includes('FOUNDER'))
  const row = ['新创始人', 'new-founder', '男', '000000200001010001', '00000000001', '创始人', root.id, 'obsolete-supervisor']
  const imported = importPersonnelTable(bulkPersonnelHeaders.join(',') + '\n' + row.join(','), data)
  assert.equal(imported[0][8], '')
  const checked = previewPersonnelTable(imported, data)
  assert.deepEqual(checked.errors, [])
  assert.equal(checked.rows[0].input.supervisorId, null)
})

test('batch employee without organization inherits supervisor organization, explicit organization wins', () => {
  const data = createSeed(), supervisor = data.people.find(p => p.roles.includes('TEAM_LEAD'))!
  const row = ['新人', 'inherit-org-user', '', '男', '000000200001010001', '00000000001', '员工', '', supervisor.loginName]
  const inferred = previewPersonnelTable([row], data)
  assert.deepEqual(inferred.errors, [])
  assert.equal(inferred.rows[0].input.orgUnitId, supervisor.orgUnitId)
  row[7] = 'org-mobile'
  const explicit = previewPersonnelTable([row], data)
  assert.deepEqual(explicit.errors, [])
  assert.equal(explicit.rows[0].input.orgUnitId, row[7])
})

test('bulk missing accounts use pinyin and avoid existing and earlier rows', () => {
  const data = createSeed(), supervisor = data.people.find(p => p.roles.includes('TEAM_LEAD'))!
  data.people[0].loginName = 'XIAOMING'
  const row = ['小明', '', '', '男', '000000200001010001', '00000000001', '员工', '', supervisor.loginName]
  const second = [...row]
  second[4] = '000000200001010002'
  second[5] = '00000000002'
  const checked = previewPersonnelTable([row, second], data)
  assert.deepEqual(checked.errors, [])
  assert.deepEqual(checked.rows.map(item => item.input.loginName), ['xiaoming2', 'xiaoming3'])
  for (const item of checked.rows) assert.match(item.initialPassword, /^[0-9]{12}$/)
})
