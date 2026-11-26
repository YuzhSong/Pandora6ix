import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { bulkPersonnelHeaders, bulkPersonnelTemplate, createBulkPersonnel, previewBulkPersonnel, previewPersonnelTable, importPersonnelTable } from '../src/data/bulkPersonnel.ts'
import type { AdminData, PersonInput } from '../src/data/types.ts'

const header = bulkPersonnelHeaders.join(',')
function line(login = 'batch-new', name = '新增员工', identity = '000000200001010001', phone = '00000000' + identity.replace(/\D/g, '').slice(-3)) { return [name, login, '男', identity, phone, '员工', 'org-web', ''].join(',') }

test('editable table preserves exact password characters, ignores blank rows and maps errors to visible row numbers', () => {
  const data = createSeed(), blank = Array<string>(9).fill('')
  const source = line().split(','); const cells = [...source.slice(0, 2), '', ...source.slice(2)]; cells[2] = '  initial,"password  '
  const valid = previewPersonnelTable([blank, cells, blank], data)
  assert.deepEqual(valid.errors, []); assert.equal(valid.rows[0].rowNumber, 2)
  assert.equal(valid.rows[0].initialPassword, cells[2]); assert.equal(valid.rows[0].input.displayName, cells[0])
  const duplicate = previewPersonnelTable([blank, cells, blank, cells], data)
  assert.match(duplicate.errors.join(''), /第 4 行：.*重复/)
  assert.ok(previewPersonnelTable([blank], data).errors.length)
  const invalid = [...cells]; invalid[4] = ''
  assert.match(previewPersonnelTable([blank, invalid], data).errors.join(''), /第 2 行/)
  assert.ok(previewPersonnelTable(Array.from({ length: 101 }, (_, i) => { const row = [...cells]; row[1] = 'bulk-' + i; return row }), data).errors.length)
})

test('CSV template and BOM are supported, while parsed punctuation and newlines fail name validation', () => {
  const data = createSeed()
  assert.equal(bulkPersonnelTemplate(), header + '\r\n')
  const badName = previewBulkPersonnel('\ufeff' + header + '\r\n' + line('batch-new', '"名字,带""引号""\n换行"'), data)
  assert.match(badName.errors.join(''), /第 2 行：姓名/)
  const result = previewBulkPersonnel('\ufeff' + header + '\r\n' + line('batch-new', '"Cheng"'), data)
  assert.deepEqual(result.errors, []); assert.equal(result.rows[0].input.displayName, 'Cheng')
  assert.equal(result.rows[0].input.gender, 'MALE'); assert.deepEqual(result.rows[0].input.roles, ['EMPLOYEE'])
  assert.equal(result.rows[0].input.idNumber, '000000200001010001'); assert.equal(result.rows[0].input.phoneNumber, '00000000001')
  assert.equal(result.rows[0].initialPassword.length, 12)
  assert.ok(!bulkPersonnelHeaders.includes('初始密码'))
  assert.equal(bulkPersonnelHeaders.length, 8)
})

test('rejects whitespace or mixed scripts in imported names', () => {
  for (const name of ['程瑶 程瑶', ' 程瑶', '程瑶 ', '程瑶　程瑶', '   ', '大Cheng']) {
    const result = previewBulkPersonnel(header + '\n' + line('batch-new', name), createSeed())
    assert.match(result.errors.join(''), /姓名.*空格/)
    assert.equal(result.rows.length, 0)
  }
})

test('rejects existing and batch phone duplicates despite formatting differences', () => {
  const data = createSeed()
  data.people[0].phoneNumber = '138-0000-0001'
  assert.match(previewBulkPersonnel(header + '\n' + line('existing-phone', '员工甲', undefined, '13800000001'), data).errors.join(''), /电话号码已被/)
  const rows = [line('first', '员工甲', '000000200001010002', '+86 (021) 1234-5678'), line('second', '员工乙', '000000200001010003', '8602112345678')]
  assert.match(previewBulkPersonnel(header + '\n' + rows.join('\n'), data).errors.join(''), /第 3 行：电话号码.*重复/)
})

test('rejects existing and same-batch identity numbers across case and whitespace', () => {
  const data = createSeed()
  data.people[0].idNumber = '00000020000101001X'
  const existing = previewBulkPersonnel(header + '\n' + line('existing-id', '员工甲', ' 00000020000101001x '), data)
  assert.match(existing.errors.join(''), /第 2 行：该身份证号已被/)
  const duplicate = previewBulkPersonnel(header + '\n' + [line('first', '员工甲', '00000020000101002X'), line('second', '员工乙', '00000020000101002x')].join('\n'), data)
  assert.match(duplicate.errors.join(''), /第 3 行：身份证号.*重复/)
  assert.equal(duplicate.rows.length, 1)
})

test('rejects existing/batch duplicate logins, bad personal fields, header, quote, size and row counts before writes', () => {
  const data = createSeed()
  assert.match(previewBulkPersonnel(header + '\n' + line(data.people[0].loginName), data).errors.join(''), /重复/)
  assert.match(previewBulkPersonnel(header + '\n' + line() + '\n' + line(), data).errors.join(''), /重复/)
  for (const source of ['', header, '错误表头\n' + line(), header + '\n' + line('n', '"unclosed'), header + '\n' + line().replace(',男,', ',未知,'), header + '\n' + Array.from({ length: 101 }, (_, index) => line('n' + index)).join('\n'), 'a'.repeat(1_000_001)]) assert.ok(previewBulkPersonnel(source, data).errors.length > 0)
  assert.ok(previewBulkPersonnel(header + '\n' + line() + ',extra', data).errors.length > 0)
})

test('resolves team paths and rejects department-direct ordinary employees', () => {
  const data = createSeed(), department = data.units.find(unit => unit.kind === 'DEPARTMENT')!, root = data.units.find(unit => unit.kind === 'COMPANY')!
  const team = data.units.find(unit => unit.parentId === department.id)!
  const lead = data.people.find(person => person.orgUnitId === team.id && person.roles.includes('TEAM_LEAD'))!
  let fields = line().split(','); fields[6] = root.name + ' / ' + department.name + ' / ' + team.name; fields[7] = lead.loginName
  const good = previewBulkPersonnel(header + '\n' + fields.join(','), data)
  assert.deepEqual(good.errors, []); assert.equal(good.rows[0].input.orgUnitId, team.id); assert.equal(good.rows[0].input.supervisorId, lead.id)
  fields[6] = department.id
  assert.match(previewBulkPersonnel(header + '\n' + fields.join(','), data).errors.join(''), /具体团队/)
  data.units.push({ ...team, id: 'duplicate-org' }); fields[6] = team.name
  assert.match(previewBulkPersonnel(header + '\n' + fields.join(','), data).errors.join(''), /重复/)
  fields[6] = team.id; fields[7] = 'obsolete-supervisor'
  assert.equal(previewBulkPersonnel(header + '\n' + fields.join(','), data).rows[0].input.supervisorId, lead.id)
})

test('sequential batch preserves successes, stops on a failed/uncertain row, and never retries automatically', async () => {
  const data = createSeed(), rows = previewBulkPersonnel(header + '\n' + [line('one'), line('two', '员工乙', '000000200001010002'), line('three', '员工丙', '000000200001010003')].join('\n'), data).rows
  let calls = 0; const results: string[] = [], accounts: string[] = []
  await createBulkPersonnel(rows, { async savePerson(input: PersonInput): Promise<AdminData> { calls++; if (calls === 2) throw new Error('网络结果不确定'); return { ...data, people: [...data.people, { ...input, id: 'created-one', version: 0 }] } } }, { stopped: () => false, onRow: (_, status) => { results.push(status) }, onCreated: (_, account) => { accounts.push(account.loginName); assert.equal(account.initialPassword, rows[0].initialPassword) } })
  assert.equal(calls, 2); assert.deepEqual(accounts, ['one']); assert.deepEqual(results, ['creating', 'success', 'creating', 'failed'])
})

test('stop requests preserve completed accounts; a mismatched successful response stops rather than fabricating credentials', async () => {
  const data = createSeed(), rows = previewBulkPersonnel(header + '\n' + [line('one'), line('two', '员工乙', '000000200001010002')].join('\n'), data).rows
  let stopped = false, calls = 0
  await createBulkPersonnel(rows, { async savePerson(input) { calls++; return { ...data, people: [...data.people, { ...input, id: 'created', version: 0 }] } } }, { stopped: () => stopped, onRow: () => {}, onCreated: () => { stopped = true } })
  assert.equal(calls, 1)
  let lastStatus = '', received = false
  await createBulkPersonnel(rows, { async savePerson() { return data } }, { stopped: () => false, onRow: (_, status) => { lastStatus = status }, onCreated: () => { received = true } })
  assert.equal(lastStatus, 'failed'); assert.equal(received, false)
})


test('eight-column CSV imports generate hidden passwords and retain invalid fields for correction', () => {
  const data = createSeed(), team = data.units.find(unit => unit.kind === 'TEAM')!
  const row = line().split(','); row[2] = 'FEMALE'; row[6] = team.name
  const imported = importPersonnelTable(header + '\n' + row.join(','), data)
  assert.equal(imported[0][2].length, 12); assert.equal(imported[0][3], '女'); assert.equal(imported[0][7], team.id)
  assert.deepEqual(previewPersonnelTable(imported, data).errors, [])
  row[2] = '未知'; row[3] = ''; row[6] = '不存在的组织'; row[7] = 'missing-manager'
  const correction = importPersonnelTable(header + '\n' + row.join(','), data)
  assert.equal(correction[0][3], '未知'); assert.equal(correction[0][4], ''); assert.equal(correction[0][7], row[6]); assert.equal(correction[0][8], row[7])
  assert.ok(previewPersonnelTable(correction, data).errors.length)
  for (const invalid of ['bad header', header, header + '\nonly,two', header + '\n' + Array(101).fill(line()).join('\n')]) assert.throws(() => importPersonnelTable(invalid, data))
})
