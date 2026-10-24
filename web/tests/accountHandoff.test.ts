import assert from 'node:assert/strict'
import test from 'node:test'
import { accountLoginText, credentialListText } from '../src/data/accountHandoff.ts'

test('ordinary account copy excludes passwords even if supplied an object with a receipt field', () => {
  const receipt = { loginName: 'staff.login', enterpriseDomain: 'company-domain', initialPassword: 'temporary-fixture' }
  const text = accountLoginText(receipt)
  assert.equal(text, '公司登录标识：company-domain\n登录账号：staff.login')
  assert.ok(!text.includes(receipt.initialPassword))
})

test('bulk handoff uses only explicitly provided credentials and preserves passwords as plain text', () => {
  const text = credentialListText([{ personId: 'one', displayName: '员工', enterpriseDomain: 'company', loginName: 'login', initialPassword: '=password+"text"' }])
  assert.equal(text, '姓名：员工\n公司登录标识：company\n登录账号：login\n初始密码：=password+"text"')
  assert.equal(credentialListText([]), '')
})

test('explicit handoff includes initial password exactly; legacy accounts omit unavailable company identifier', () => {
  const password = 'initial password with spaces'
  assert.equal(accountLoginText({ loginName: '员工一', enterpriseDomain: 'company-domain' }, password), `公司登录标识：company-domain\n登录账号：员工一\n初始密码：${password}`)
  assert.equal(accountLoginText({ loginName: 'legacy' }), '登录账号：legacy')
})


test('only currently available server credentials enter the delivery list', async () => {
  const { availableHandoffAccounts } = await import('../src/data/accountHandoff.ts')
  const base = { personId: 'person', displayName: '员工', loginName: 'renamed', enterpriseDomain: 'team', initialPassword: 'temporary' }
  const result = availableHandoffAccounts([{ ...base, status: 'AVAILABLE' }, { ...base, personId: 'changed', status: 'CHANGED' }, { ...base, personId: 'expired', status: 'EXPIRED' }, { ...base, personId: 'legacy', status: 'UNAVAILABLE' }])
  assert.equal(result.length, 1)
  assert.equal(result[0].loginName, 'renamed')
})
