import assert from 'node:assert/strict'
import test from 'node:test'
import { operationChoices } from '../src/data/operationChoices.ts'

test('operation filters name the actual objects rather than ambiguous standalone verbs', () => {
  const all = operationChoices('')
  assert.equal(all.find(option => option.value === 'DISABLE')?.label, '停用账号')
  assert.equal(all.find(option => option.value === 'APPROVE')?.label, '通过任务审核')
  assert.equal(all.find(option => option.value === 'LOGIN')?.label, '账号登录')
  assert.equal(new Set(all.map(option => option.value)).size, all.length)
})
test('module-specific actions exclude unrelated operations and explain create/update/delete targets', () => {
  assert.deepEqual(operationChoices('WORK_LOG'), [{ value: '', label: '全部操作' }, { value: 'CREATE', label: '新增工作日报' }, { value: 'UPDATE', label: '修改工作日报' }, { value: 'DELETE', label: '删除工作日报' }])
  assert.deepEqual(operationChoices('AUTH').map(option => option.value), ['', 'LOGIN'])
  assert.equal(operationChoices('TASK').some(option => option.value === 'DISABLE'), false)
  assert.equal(operationChoices('TASK').find(option => option.value === 'UPDATE')?.label, '修改任务')
  assert.ok(operationChoices('ACCOUNT').some(option => option.value === 'PASSWORD_RESET'))
})
