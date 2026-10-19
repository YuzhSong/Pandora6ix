import assert from 'node:assert/strict'
import test from 'node:test'
import { createSeed } from '../src/data/seed.ts'
import { bulkPersonnelHeaders, bulkPersonnelTemplate, createBulkPersonnel, previewBulkPersonnel } from '../src/data/bulkPersonnel.ts'
import type { AdminData, PersonInput } from '../src/data/types.ts'

const header = bulkPersonnelHeaders.join(',')
function line(login = 'batch-new', name = '新增员工') { return [name, login, 'initial-password-fixture', '男', '000000200001010001', '00000000001', '开发', '', ''].join(',') }

test('CSV template, BOM, quoted commas/newlines and escaped quotes prevalidate without exposing passwords in errors', () => {
  const data = createSeed()
  assert.equal(bulkPersonnelTemplate(), header + '\r\n')
  const result = previewBulkPersonnel('\ufeff' + header + '\r\n' + line('batch-new', '"名字,带""引号""\n换行"'), data)
  assert.deepEqual(result.errors, []); assert.equal(result.rows[0].input.displayName, '名字,带"引号"\n换行')
  assert.equal(result.rows[0].input.gender, 'MALE'); assert.deepEqual(result.rows[0].input.roles, ['EMPLOYEE'])
  assert.equal(result.rows[0].input.idNumber, '000000200001010001'); assert.equal(result.rows[0].input.phoneNumber, '00000000001')
  const invalid = previewBulkPersonnel(header + '\n' + line().replace('initial-password-fixture', 'short'), data)
  assert.ok(invalid.errors.length > 0); assert.ok(!invalid.errors.join('').includes('short'))
})

test('rejects existing/batch duplicate logins, bad personal fields, header, quote, size and row counts before writes', () => {
  const data = createSeed()
  assert.match(previewBulkPersonnel(header + '\n' + line(data.people[0].loginName), data).errors.join(''), /重复/)
  assert.match(previewBulkPersonnel(header + '\n' + line() + '\n' + line(), data).errors.join(''), /重复/)
  for (const source of ['', header, '错误表头\n' + line(), header + '\n' + line('n', '"unclosed'), header + '\n' + line().replace(',男,', ',未知,'), header + '\n' + Array.from({ length: 101 }, (_, index) => line('n' + index)).join('\n'), 'a'.repeat(1_000_001)]) assert.ok(previewBulkPersonnel(source, data).errors.length > 0)
  assert.ok(previewBulkPersonnel(header + '\n' + line().replace('initial-password-fixture', '密'.repeat(25)), data).errors.length > 0)
})

test('resolves organization paths and valid supervisors and rejects ambiguous organizations or non-managers', () => {
  const data = createSeed(), department = data.units.find(unit => unit.kind === 'DEPARTMENT')!, root = data.units.find(unit => unit.kind === 'COMPANY')!
  const manager = data.people.find(person => person.roles.includes('DEPARTMENT_MANAGER'))!
  let fields = line().split(','); fields[7] = root.name + ' / ' + department.name; fields[8] = manager.loginName
  const good = previewBulkPersonnel(header + '\n' + fields.join(','), data)
  assert.deepEqual(good.errors, []); assert.equal(good.rows[0].input.orgUnitId, department.id); assert.equal(good.rows[0].input.supervisorId, manager.id)
  data.units.push({ ...department, id: 'duplicate-org' }); fields[7] = department.name
  assert.match(previewBulkPersonnel(header + '\n' + fields.join(','), data).errors.join(''), /重复/)
  fields[7] = ''; fields[8] = data.people.find(person => person.roles.length === 1 && person.roles.includes('EMPLOYEE'))!.loginName
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
