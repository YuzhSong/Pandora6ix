import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { bulkPersonnelHeaders, bulkPersonnelTemplate, createBulkPersonnel, previewBulkPersonnel, previewPersonnelTable, importPersonnelTable } from '../src/data/bulkPersonnel.ts'
import type { AdminData, PersonInput } from '../src/data/types.ts'

const header = bulkPersonnelHeaders.join(',')
function line(login = 'batch-new', name = '新增员工') { return [name, login, '男', '000000200001010001', '00000000001', '员工', '', ''].join(',') }

test('editable table preserves exact password characters, ignores blank rows and maps errors to visible row numbers', () => {
  const data = createSeed(), blank = Array<string>(9).fill('')
  const source = line().split(','); const cells = [...source.slice(0, 2), '', ...source.slice(2)]; cells[0] = '名字,带"引号"'; cells[2] = '  initial,"password  '
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

test('CSV template, BOM, quoted commas/newlines and escaped quotes prevalidate without exposing passwords in errors', () => {
  const data = createSeed()
  assert.equal(bulkPersonnelTemplate(), header + '\r\n')
  const result = previewBulkPersonnel('\ufeff' + header + '\r\n' + line('batch-new', '"名字,带""引号""\n换行"'), data)
  assert.deepEqual(result.errors, []); assert.equal(result.rows[0].input.displayName, '名字,带"引号"\n换行')
  assert.equal(result.rows[0].input.gender, 'MALE'); assert.deepEqual(result.rows[0].input.roles, ['EMPLOYEE'])
  assert.equal(result.rows[0].input.idNumber, '000000200001010001'); assert.equal(result.rows[0].input.phoneNumber, '00000000001')
  assert.equal(result.rows[0].initialPassword.length, 12)
  assert.ok(!bulkPersonnelHeaders.includes('初始密码'))
  assert.equal(bulkPersonnelHeaders.length, 8)
})

test('rejects existing/batch duplicate logins, bad personal fields, header, quote, size and row counts before writes', () => {
  const data = createSeed()
  assert.match(previewBulkPersonnel(header + '\n' + line(data.people[0].loginName), data).errors.join(''), /重复/)
  assert.match(previewBulkPersonnel(header + '\n' + line() + '\n' + line(), data).errors.join(''), /重复/)
  for (const source of ['', header, '错误表头\n' + line(), header + '\n' + line('n', '"unclosed'), header + '\n' + line().replace(',男,', ',未知,'), header + '\n' + Array.from({ length: 101 }, (_, index) => line('n' + index)).join('\n'), 'a'.repeat(1_000_001)]) assert.ok(previewBulkPersonnel(source, data).errors.length > 0)
  assert.ok(previewBulkPersonnel(header + '\n' + line() + ',extra', data).errors.length > 0)
})

test('resolves organization paths and valid supervisors and rejects ambiguous organizations or non-managers', () => {
  const data = createSeed(), department = data.units.find(unit => unit.kind === 'DEPARTMENT')!, root = data.units.find(unit => unit.kind === 'COMPANY')!
  const manager = data.people.find(person => person.roles.includes('DEPARTMENT_MANAGER'))!
  let fields = line().split(','); fields[6] = root.name + ' / ' + department.name; fields[7] = manager.loginName
  const good = previewBulkPersonnel(header + '\n' + fields.join(','), data)
  assert.deepEqual(good.errors, []); assert.equal(good.rows[0].input.orgUnitId, department.id); assert.equal(good.rows[0].input.supervisorId, manager.id)
  data.units.push({ ...department, id: 'duplicate-org' }); fields[6] = department.name
  assert.match(previewBulkPersonnel(header + '\n' + fields.join(','), data).errors.join(''), /重复/)
  fields[6] = ''; fields[7] = data.people.find(person => person.roles.length === 1 && person.roles.includes('EMPLOYEE'))!.loginName
  assert.match(previewBulkPersonnel(header + '\n' + fields.join(','), data).errors.join(''), /直属上级/)
})

test('sequential batch preserves successes, stops on a failed/uncertain row, and never retries automatically', async () => {
  const data = createSeed(), rows = previewBulkPersonnel(header + '\n' + [line('one'), line('two'), line('three')].join('\n'), data).rows
  let calls = 0; const results: string[] = [], accounts: string[] = []
  await createBulkPersonnel(rows, { async savePerson(input: PersonInput): Promise<AdminData> { calls++; if (calls === 2) throw new Error('网络结果不确定'); return { ...data, people: [...data.people, { ...input, id: 'created-one', version: 0 }] } } }, { stopped: () => false, onRow: (_, status) => { results.push(status) }, onCreated: (_, account) => { accounts.push(account.loginName); assert.equal(account.initialPassword, rows[0].initialPassword) } })
  assert.equal(calls, 2); assert.deepEqual(accounts, ['one']); assert.deepEqual(results, ['creating', 'success', 'creating', 'failed'])
})

test('stop requests preserve completed accounts; a mismatched successful response stops rather than fabricating credentials', async () => {
  const data = createSeed(), rows = previewBulkPersonnel(header + '\n' + [line('one'), line('two')].join('\n'), data).rows
  let stopped = false, calls = 0
  await createBulkPersonnel(rows, { async savePerson(input) { calls++; return { ...data, people: [...data.people, { ...input, id: 'created', version: 0 }] } } }, { stopped: () => stopped, onRow: () => {}, onCreated: () => { stopped = true } })
  assert.equal(calls, 1)
  let lastStatus = '', received = false
  await createBulkPersonnel(rows, { async savePerson() { return data } }, { stopped: () => false, onRow: (_, status) => { lastStatus = status }, onCreated: () => { received = true } })
  assert.equal(lastStatus, 'failed'); assert.equal(received, false)
})


test('eight-column CSV imports generate hidden passwords and retain invalid fields for correction', () => {
  const data = createSeed(), department = data.units.find(unit => unit.kind === 'DEPARTMENT')!
  const row = line().split(','); row[2] = 'FEMALE'; row[6] = department.name
  const imported = importPersonnelTable(header + '\n' + row.join(','), data)
  assert.equal(imported[0][2].length, 12); assert.equal(imported[0][3], '女'); assert.equal(imported[0][7], department.id)
  assert.deepEqual(previewPersonnelTable(imported, data).errors, [])
  row[2] = '未知'; row[3] = ''; row[6] = '不存在的组织'; row[7] = 'missing-manager'
  const correction = importPersonnelTable(header + '\n' + row.join(','), data)
  assert.equal(correction[0][3], '未知'); assert.equal(correction[0][4], ''); assert.equal(correction[0][7], row[6]); assert.equal(correction[0][8], row[7])
  assert.ok(previewPersonnelTable(correction, data).errors.length)
  for (const invalid of ['bad header', header, header + '\nonly,two', header + '\n' + Array(101).fill(line()).join('\n')]) assert.throws(() => importPersonnelTable(invalid, data))
})
