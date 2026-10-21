import { ApiError } from './client.ts'
import type { OperationDetail, OperationFilters, OperationPage, OperationRecord, OperationSnapshot, OperationVersion } from '../data/operationHistory.ts'
import { dateRangeValid } from '../data/operationHistory.ts'

interface ReaderClient { request<T>(path: string, options?: { signal?: AbortSignal }): Promise<T> }
const malformed = () => new Error('系统日志数据格式不正确，请联系系统维护人员。')
function object(value: unknown): Record<string, unknown> { if (!value || typeof value !== 'object' || Array.isArray(value)) throw malformed(); return value as Record<string, unknown> }
function string(value: unknown): string { if (typeof value !== 'string') throw malformed(); return value }
function nullable(value: unknown): string | null { return value === null ? null : string(value) }
function integer(value: unknown): number { if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw malformed(); return value }
function timestamp(value: unknown): string { const result = string(value); if (!/T.*(?:Z|[+-]\d{2}:\d{2})$/.test(result) || !Number.isFinite(Date.parse(result))) throw malformed(); return result }
function record(value: unknown): OperationRecord {
  const row = object(value), actor = object(row.actor), target = object(row.object)
  if (row.result !== 'SUCCESS' && row.result !== 'FAILURE') throw malformed()
  const id = string(row.id); if (!id) throw malformed()
  return { id, occurredAt: timestamp(row.occurredAt), actor: { id: nullable(actor.id), displayName: string(actor.displayName), loginName: nullable(actor.loginName) }, object: { id: nullable(target.id), type: string(target.type), label: string(target.label) }, module: string(row.module), action: string(row.action), source: string(row.source), result: row.result, summary: string(row.summary) }
}
function snapshot(value: unknown): OperationSnapshot | null {
  if (value === null) return null
  const row = object(value)
  if (!Array.isArray(row.fields)) throw malformed()
  const fields = row.fields.map(item => { const field = object(item); return { key: string(field.key), label: string(field.label), value: nullable(field.value) } })
  if (fields.some(field => !field.key || !field.label) || new Set(fields.map(field => field.key)).size !== fields.length) throw malformed()
  return { version: integer(row.version), fields }
}
function query(filters: OperationFilters, cursor?: string, snapshotToken?: string, limit = 20): string {
  if (!dateRangeValid(filters)) throw new Error('结束日期不能早于开始日期。')
  const params = new URLSearchParams({ limit: String(limit) })
  for (const key of ['keyword', 'module', 'action', 'source', 'result'] as const) if (filters[key].trim()) params.set(key, filters[key].trim())
  // The confirmed client business timezone is Asia/Shanghai. End date is exclusive next midnight.
  if (filters.startDate) params.set('from', new Date(`${filters.startDate}T00:00:00+08:00`).toISOString())
  if (filters.endDate) params.set('to', new Date(Date.parse(`${filters.endDate}T00:00:00+08:00`) + 86_400_000).toISOString())
  if (cursor) params.set('cursor', cursor)
  if (snapshotToken) params.set('snapshotToken', snapshotToken)
  return params.toString()
}
export function createOperationLogReader(client: ReaderClient) {
  async function request(path: string, signal?: AbortSignal): Promise<unknown> {
    try { return await client.request<unknown>(path, { signal }) }
    catch (reason) { if (reason instanceof ApiError && (reason.status === 404 || reason.status === 501)) throw new Error('系统日志接口尚未接入，请联系后端完成对接。'); throw reason }
  }
  async function list(filters: OperationFilters, options: { cursor?: string; snapshotToken?: string; limit?: number; signal?: AbortSignal } = {}): Promise<OperationPage> {
    const data = object(await request(`/admin/operation-logs?${query(filters, options.cursor, options.snapshotToken, options.limit)}`, options.signal))
    if (!Array.isArray(data.items)) throw malformed()
    const items = data.items.map(record), total = integer(data.total), nextCursor = nullable(data.nextCursor), snapshotToken = string(data.snapshotToken)
    if (!snapshotToken || total < items.length || new Set(items.map(item => item.id)).size !== items.length || (nextCursor !== null && (!nextCursor || !items.length)) || (options.snapshotToken && options.snapshotToken !== snapshotToken)) throw malformed()
    return { items, total, nextCursor, snapshotToken }
  }
  async function detail(id: string, signal?: AbortSignal): Promise<OperationDetail> {
    const data = object(await request(`/admin/operation-logs/${encodeURIComponent(id)}`, signal))
    if (!Array.isArray(data.versions)) throw malformed()
    const versions: OperationVersion[] = data.versions.map(item => { const value = object(item); return { logId: string(value.logId), version: integer(value.version), occurredAt: timestamp(value.occurredAt), actorName: string(value.actorName), action: string(value.action) } })
    const base = record(data)
    if (base.id !== id || new Set(versions.map(version => version.logId)).size !== versions.length) throw malformed()
    if (base.result === 'FAILURE' && (data.before !== null || data.after !== null)) throw malformed()
    return { ...base, requestId: nullable(data.requestId), failureReason: nullable(data.failureReason), before: snapshot(data.before), after: snapshot(data.after), versions }
  }
  async function all(filters: OperationFilters, signal?: AbortSignal): Promise<OperationRecord[]> {
    const items: OperationRecord[] = [], seen = new Set<string>(), cursors = new Set<string>()
    let cursor: string | undefined, snapshotToken: string | undefined, total: number | undefined
    do {
      signal?.throwIfAborted()
      const page = await list(filters, { cursor, snapshotToken, limit: 200, signal })
      if (total !== undefined && total !== page.total) throw new Error('日志结果已发生变化，请重新导出。')
      total = page.total; snapshotToken = page.snapshotToken
      for (const row of page.items) { if (seen.has(row.id)) throw new Error('日志分页数据重复，请重新导出。'); seen.add(row.id); items.push(row) }
      if (items.length > total || (items.length === total && page.nextCursor)) throw new Error('日志分页数据不一致，请重新导出。')
      if (page.nextCursor && cursors.has(page.nextCursor)) throw new Error('日志分页异常，请联系系统维护人员。')
      cursor = page.nextCursor ?? undefined
      if (cursor) cursors.add(cursor)
    } while (cursor)
    if (items.length !== total) throw new Error('日志导出数据不完整，请重试。')
    return items
  }
  return { list, detail, all }
}
