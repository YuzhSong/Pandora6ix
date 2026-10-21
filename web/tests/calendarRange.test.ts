import assert from 'node:assert/strict'
import test from 'node:test'
import { calendarDays, parseCalendarDate, parseCalendarMonth, rangePreset, shiftMonth } from '../src/data/calendarRange.ts'
import { createOperationLogReader } from '../src/api/operationLogs.ts'
import { previewLogRequest } from '../dev/operationLogPreview.ts'
import { emptyLogFilters } from '../src/data/operationHistory.ts'

test('calendar starts on Monday and includes leap dates and neighboring months', () => {
  const days = calendarDays('2024-02')
  assert.equal(days.length, 42); assert.equal(days[0], '2024-01-29')
  assert.ok(days.includes('2024-02-29')); assert.equal(new Set(days).size, 42)
  assert.equal(shiftMonth('2026-12', 1), '2027-01'); assert.equal(shiftMonth('2026-01', -1), '2025-12')
})
test('date presets keep inclusive seven-day and current-month boundaries', () => {
  assert.deepEqual(rangePreset('today', '2026-10-20'), ['2026-10-20', '2026-10-20'])
  assert.deepEqual(rangePreset('week', '2026-01-03'), ['2025-12-28', '2026-01-03'])
  assert.deepEqual(rangePreset('month', '2026-10-20'), ['2026-10-01', '2026-10-20'])
})

test('typed dates normalize separators and reject rollover, malformed and impossible dates', () => {
  assert.equal(parseCalendarDate('2024/2/29'), '2024-02-29')
  assert.equal(parseCalendarDate(' 2026.10.2 '), '2026-10-02')
  for (const input of ['2026-02-29', '2024-02-30', '2026-13-01', '2026-00-10', '2026-10-00', '2026-10', '', '999-10-20', '2026-1-1suffix']) assert.equal(parseCalendarDate(input), null, input)
  assert.equal(parseCalendarMonth('2020年2月'), '2020-02')
  assert.equal(parseCalendarMonth('2020-02'), '2020-02')
  assert.equal(parseCalendarMonth('2020/2'), '2020-02')
  assert.equal(parseCalendarMonth('2020-13'), null)
})
test('explicit preview dataset supports fixed pages, full export and true filter semantics', async () => {
  const reader = createOperationLogReader({ request: previewLogRequest })
  const first = await reader.list(emptyLogFilters)
  assert.equal(first.items.length, 20); assert.equal(first.total, 27)
  const second = await reader.list(emptyLogFilters, { cursor: first.nextCursor!, snapshotToken: first.snapshotToken })
  assert.equal(second.items.length, 7); assert.equal(second.nextCursor, null)
  assert.equal((await reader.all(emptyLogFilters)).length, 27)
  const filtered = await reader.list({ ...emptyLogFilters, module: 'AUTH', result: 'FAILURE', keyword: 'wangwu', startDate: '2026-10-20', endDate: '2026-10-20' })
  assert.equal(filtered.total, 1)
  const empty = await reader.list({ ...emptyLogFilters, endDate: '2026-10-18' })
  assert.equal(empty.total, 0)
})
test('sample full versions survive deletion and respect aborts without touching other APIs', async () => {
  const reader = createOperationLogReader({ request: previewLogRequest })
  const changed = await reader.detail('preview-task-update')
  assert.equal(changed.before?.version, 1); assert.equal(changed.after?.version, 2)
  assert.equal(changed.versions.length, 2)
  const removed = await reader.detail('preview-journal-delete')
  assert.ok(removed.before); assert.equal(removed.after, null)
  const controller = new AbortController(); controller.abort()
  await assert.rejects(reader.list(emptyLogFilters, { signal: controller.signal }), { name: 'AbortError' })
  await assert.rejects(previewLogRequest('/admin/users'))
})
