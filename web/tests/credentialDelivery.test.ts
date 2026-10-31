import assert from 'node:assert/strict'
import test from 'node:test'
import ExcelJS from 'exceljs'
import { prepareCredentialDelivery, searchDeliveryAccounts } from '../src/data/credentialDelivery.ts'
import { credentialListText } from '../src/data/accountHandoff.ts'
import { credentialWorkbook } from '../src/data/credentialWorkbook.ts'
import type { CredentialHandoff } from '../src/data/types.ts'

const one: CredentialHandoff = { personId: 'one', displayName: '员工甲', loginName: '00123', enterpriseDomain: 'pandora', status: 'AVAILABLE', initialPassword: 'old-password', expiresAt: '2026-11-01T00:00:00Z' }
const two: CredentialHandoff = { ...one, personId: 'two', displayName: '员工乙', loginName: 'staff2', initialPassword: 'another-password' }

test('delivery search includes only available accounts and matches full login identifier', () => {
  const items: CredentialHandoff[] = [one, two, { ...one, personId: 'expired', status: 'EXPIRED', initialPassword: undefined }, { ...one, personId: 'changed', status: 'CHANGED', initialPassword: undefined }, { ...one, personId: 'legacy', status: 'UNAVAILABLE', initialPassword: undefined }]
  assert.deepEqual(searchDeliveryAccounts(items, '').map(item => item.personId), ['one', 'two'])
  assert.deepEqual(searchDeliveryAccounts(items, '00123@PANDORA').map(item => item.personId), ['one'])
  assert.equal(searchDeliveryAccounts(items, '不存在').length, 0)
  assert.equal(searchDeliveryAccounts(items, '员工乙')[0].personId, 'two')
})

test('current batch and individual delivery include only requested IDs with fresh password and account data', async () => {
  let reads = 0
  const accounts = await prepareCredentialDelivery(async () => { reads++; return [two, { ...one, loginName: 'renamed', initialPassword: '=new-password' }] }, ['one', 'one'])
  assert.equal(reads, 1); assert.equal(accounts.length, 1)
  assert.equal(accounts[0].loginName, 'renamed'); assert.equal(accounts[0].initialPassword, '=new-password')
  assert.ok(!credentialListText(accounts).includes('old-password'))
  assert.ok(!credentialListText(accounts).includes('staff2'))
  const workbook = new ExcelJS.Workbook(); await workbook.xlsx.load(await credentialWorkbook(accounts))
  const sheet = workbook.getWorksheet('账号交付')!
  assert.equal(sheet.getCell('D2').value, 'pandora'); assert.equal(sheet.getCell('E2').value, 'renamed');
  assert.equal(sheet.rowCount, 2); assert.equal(sheet.getCell('B2').value, 'renamed@pandora')
  assert.equal(sheet.getCell('C2').value, '=new-password'); assert.equal(sheet.getCell('C2').numFmt, '@')
})

test('changed, expired, disabled or missing selected accounts block export instead of silently exporting a partial selection', async () => {
  for (const status of ['CHANGED', 'EXPIRED', 'UNAVAILABLE'] as const) {
    await assert.rejects(prepareCredentialDelivery(async () => [two, { ...one, status, initialPassword: undefined }], ['one', 'two']), /重新选择/)
  }
  await assert.rejects(prepareCredentialDelivery(async () => [two], ['one']), /重新选择/)
  await assert.rejects(prepareCredentialDelivery(async () => { throw new Error('未登录') }, ['one']), /未登录/)
  await assert.rejects(prepareCredentialDelivery(async () => { assert.fail('empty selection must not fetch') }, []), /先选择/)
})
