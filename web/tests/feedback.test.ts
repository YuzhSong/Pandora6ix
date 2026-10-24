import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError } from '../src/api/client.ts'
import { errorMessage } from '../src/api/feedback.ts'

test('database conflicts explain the failure in Chinese without guessing its cause', () => {
  const message = errorMessage(new ApiError('A unique resource or valid relationship is required.', 409, 'DATA_CONFLICT'))
  assert.match(message, /数据存在重复或关联关系冲突/)
  assert.match(message, /服务端错误记录/)
  assert.doesNotMatch(message, /unique resource/)
})
