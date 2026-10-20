import assert from 'node:assert/strict'
import test from 'node:test'
import ExcelJS from 'exceljs'
import { ApiError } from '../src/api/client.ts'
import { createOperationLogReader } from '../src/api/operationLogs.ts'
import { compareSnapshots, emptyLogFilters, logTime } from '../src/data/operationHistory.ts'
import type { OperationRecord } from '../src/data/operationHistory.ts'
import { operationLogWorkbook } from '../src/data/operationLogWorkbook.ts'

const row: OperationRecord = { id: 'l1', occurredAt: '2026-10-20T02:30:00Z', actor: { id: 'u1', displayName: '张三', loginName: '00123' }, module: 'TASK', action: 'UPDATE', source: 'ANDROID', result: 'SUCCESS', object: { id: 't1', type: 'TASK', label: '任务 #1' }, summary: '修改任务 #1' }
function client(respond: (path: string) => unknown) { return { async request<T>(path: string) { return respond(path) as T } } }
const page = (items = [row], total = 1, nextCursor: string | null = null, snapshotToken = 'snapshot') => ({ items, total, nextCursor, snapshotToken })

test('operation query encodes filters and inclusive Beijing end date as exclusive instant', async () => {
  let path = ''
  const reader = createOperationLogReader(client(value => { path = value; return page() }))
  await reader.list({ ...emptyLogFilters, keyword: ' 张三 & / ', startDate: '2026-10-20', endDate: '2026-10-20', module: 'TASK', action: 'UPDATE', result: 'SUCCESS', source: 'ANDROID' })
  const params = new URL(`https://example.invalid${path}`).searchParams
  assert.equal(params.get('keyword'), '张三 & /')
  assert.equal(params.get('from'), '2026-10-19T16:00:00.000Z')
  assert.equal(params.get('to'), '2026-10-20T16:00:00.000Z')
  assert.equal(params.get('module'), 'TASK'); assert.equal(params.get('limit'), '20')
  await assert.rejects(reader.list({ ...emptyLogFilters, startDate: '2026-10-21', endDate: '2026-10-20' }), /结束日期/)
})

test('missing and malformed log APIs never substitute demo data', async () => {
  await assert.rejects(createOperationLogReader(client(() => { throw new ApiError('Not found', 404, 'NOT_FOUND') })).list(emptyLogFilters), /接口尚未接入/)
  for (const response of [{ items: [] }, page([{ ...row, result: 'UNKNOWN' } as unknown as OperationRecord]), page([row, row], 2), page([row], 0), page([], 1, 'next'), page([{ ...row, occurredAt: 'today' }])]) {
    await assert.rejects(createOperationLogReader(client(() => response)).list(emptyLogFilters), /格式不正确/)
  }
})

test('export retrieves every filtered cursor page within a fixed server snapshot', async () => {
  const paths: string[] = []
  const reader = createOperationLogReader(client(path => { paths.push(path); return paths.length === 1 ? page([row], 2, 'cursor-two') : page([{ ...row, id: 'l2' }], 2) }))
  const rows = await reader.all({ ...emptyLogFilters, module: 'TASK' })
  assert.deepEqual(rows.map(item => item.id), ['l1', 'l2'])
  const params = new URL(`https://example.invalid${paths[1]}`).searchParams
  assert.equal(params.get('snapshotToken'), 'snapshot'); assert.equal(params.get('cursor'), 'cursor-two'); assert.equal(params.get('module'), 'TASK'); assert.equal(params.get('limit'), '200')
})

test('export rejects partial, duplicated, looping and inconsistent datasets', async () => {
  const sequences = [
    [page([row], 2)],
    [page([row], 2, 'c'), page([row], 2)],
    [page([row], 3, 'c'), page([{ ...row, id: 'l2' }], 3, 'c')],
    [page([row], 2, 'c'), page([{ ...row, id: 'l2' }], 2, null, 'other-snapshot')],
    [page([row], 2, 'c'), page([{ ...row, id: 'l2' }], 3)],
  ]
  for (const responses of sequences) { let index = 0; await assert.rejects(createOperationLogReader(client(() => responses[index++])).all(emptyLogFilters)) }
})

test('cancelled export makes no request', async () => {
  const controller = new AbortController(); controller.abort()
  const reader = createOperationLogReader(client(() => { assert.fail('must not fetch after abort') }))
  await assert.rejects(reader.all(emptyLogFilters, controller.signal), { name: 'AbortError' })
})

test('query and detail forward cancellation signals so obsolete or logged-out views can discard work', async () => {
  const controller = new AbortController(), signals: (AbortSignal | undefined)[] = []
  const reader = createOperationLogReader({ async request<T>(path: string, options?: { signal?: AbortSignal }) {
    signals.push(options?.signal)
    return (path.includes('?') ? page() : { ...row, requestId: null, failureReason: null, before: null, after: null, versions: [] }) as T
  } })
  await reader.list(emptyLogFilters, { signal: controller.signal }); await reader.detail(row.id, controller.signal)
  assert.deepEqual(signals, [controller.signal, controller.signal])
})

test('failed operation cannot be presented as a committed historical version', async () => {
  const failed = { ...row, result: 'FAILURE', requestId: null, failureReason: '权限不足', before: null, after: null, versions: [] }
  assert.equal((await createOperationLogReader(client(() => failed)).detail(row.id)).failureReason, '权限不足')
  await assert.rejects(createOperationLogReader(client(() => ({ ...failed, after: { version: 2, fields: [] } }))).detail(row.id), /格式不正确/)
})

test('snapshot comparison retains added and removed fields, null transitions, full text and safe unchanged fields', () => {
  const before = { version: 1, fields: [{ key: 'title', label: '标题', value: '标题' }, { key: 'body', label: '内容', value: '第一行\n第二行' }, { key: 'old', label: '旧字段', value: '历史' }, { key: 'nullable', label: '备注', value: null }] }
  const after = { version: 2, fields: [{ key: 'title', label: '标题', value: '标题' }, { key: 'body', label: '内容', value: '第一行\n更新行' }, { key: 'new', label: '新字段', value: null }, { key: 'nullable', label: '备注', value: '' }] }
  const result = compareSnapshots(before, after)
  assert.equal(result.find(field => field.key === 'title')?.changed, false)
  assert.equal(result.find(field => field.key === 'body')?.before, '第一行\n第二行')
  assert.deepEqual(result.filter(field => field.changed).map(field => field.key), ['body', 'new', 'nullable', 'old'])
  assert.equal(compareSnapshots(before, null).length, 4)
  assert.equal(compareSnapshots(null, after).length, 4)
})

test('snapshots defensively suppress credentials and identity fields from both versions', () => {
  const snapshot = { version: 1, fields: [
    { key: 'password_hash', label: '摘要', value: 'hash-secret' }, { key: 'accessToken', label: '访问凭据', value: 'secret' },
    { key: 'id_number', label: '证件', value: 'secret' }, { key: 'unknown', label: '身份证号', value: 'secret' }, { key: 'title', label: '标题', value: '<script>literal</script>' },
  ] }
  assert.deepEqual(compareSnapshots(snapshot, snapshot).map(field => field.key), ['title'])
})

test('detail validates identity and full snapshots independently of deleted live objects', async () => {
  const detail = { ...row, action: 'DELETE', requestId: null, failureReason: null, before: { version: 2, fields: [{ key: 'content', label: '内容', value: '已删除对象的内容' }] }, after: null, versions: [{ logId: row.id, version: 2, occurredAt: row.occurredAt, actorName: '张三', action: 'DELETE' }] }
  const reader = createOperationLogReader(client(() => detail))
  assert.equal((await reader.detail(row.id)).before?.fields[0].value, '已删除对象的内容')
  await assert.rejects(reader.detail('another-id'), /格式不正确/)
  await assert.rejects(createOperationLogReader(client(() => ({ ...detail, before: { version: 1, fields: [{ key: 'body', label: '正文', value: { raw: 'JSON' } }] } }))).detail(row.id), /格式不正确/)
})

test('operation Excel uses readable Chinese labels and literal text, with no business snapshots', async () => {
  const bytes = await operationLogWorkbook([{ ...row, summary: '=HYPERLINK("url")' }])
  const workbook = new ExcelJS.Workbook(); await workbook.xlsx.load(bytes)
  const sheet = workbook.getWorksheet('系统操作日志')!
  assert.equal(sheet.rowCount, 2)
  assert.equal(sheet.getCell('C2').value, '00123'); assert.equal(sheet.getCell('C2').numFmt, '@')
  assert.equal(sheet.getCell('D2').value, '任务'); assert.equal(sheet.getCell('E2').value, '修改')
  assert.equal(sheet.getCell('G2').value, '=HYPERLINK("url")'); assert.equal(sheet.getCell('I2').value, '成功')
  assert.match(String(sheet.getCell('A2').value), /10:30:00/)
  assert.match(logTime(row.occurredAt), /10:30:00/)
})
