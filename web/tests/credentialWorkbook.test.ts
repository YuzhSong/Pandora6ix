import assert from 'node:assert/strict'
import test from 'node:test'
import ExcelJS from 'exceljs'
import { credentialWorkbook } from '../src/data/credentialWorkbook.ts'
import { availableHandoffAccounts } from '../src/data/accountHandoff.ts'

test('Excel handoff is a readable XLSX and preserves passwords and leading-zero accounts as text', async () => {
  const bytes = await credentialWorkbook([
    { personId: 'one', displayName: '员工甲', enterpriseDomain: 'pandora', loginName: '00123', initialPassword: '001234567890' },
    { personId: 'two', displayName: '员工乙', enterpriseDomain: 'pandora', loginName: '=1+1', initialPassword: '=SUM(1,2)' },
    { personId: 'three', displayName: '员工丙', loginName: 'staff', initialPassword: '  密码<&>12345  ' },
  ])
  assert.equal(bytes[0], 0x50); assert.equal(bytes[1], 0x4b)
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(bytes)
  const sheet = workbook.getWorksheet('账号交付')!
  assert.equal(sheet.rowCount, 4)
  assert.deepEqual([1, 2, 3, 4].map(column => sheet.getCell(1, column).value), ['姓名', '公司登录标识', '登录账号', '初始密码'])
  assert.equal(sheet.getCell('C2').value, '00123')
  assert.equal(sheet.getCell('D2').value, '001234567890')
  assert.equal(sheet.getCell('D2').numFmt, '@')
  assert.equal(sheet.getCell('C3').value, '=1+1')
  assert.equal(sheet.getCell('D3').value, '=SUM(1,2)')
  assert.equal(sheet.getCell('D4').value, '  密码<&>12345  ')
})

test('changed and expired employees never enter the Excel password handoff', async () => {
  const base = { personId: 'one', displayName: '员工甲', enterpriseDomain: 'pandora', loginName: 'staff', initialPassword: 'initial-12345' }
  const bytes = await credentialWorkbook(availableHandoffAccounts([
    { ...base, status: 'AVAILABLE' },
    { ...base, personId: 'two', loginName: 'changed', status: 'CHANGED' },
    { ...base, personId: 'three', loginName: 'expired', status: 'EXPIRED' },
  ]))
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(bytes)
  const sheet = workbook.getWorksheet('账号交付')!
  assert.equal(sheet.rowCount, 2)
  assert.equal(sheet.getCell('C2').value, 'staff')
  assert.equal(sheet.getCell('D2').value, 'initial-12345')
})
