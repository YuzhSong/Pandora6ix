// Explicit loopback development preview only. Never a fallback for failed real APIs.
import type { OperationDetail, OperationRecord, OperationSnapshot } from '../src/data/operationHistory.ts'

const actor = { id: 'preview-user-1', displayName: '张三', loginName: 'zhangsan' }
const before: OperationSnapshot = { version: 1, fields: [
  { key: 'title', label: '任务名称', value: '准备验收材料' },
  { key: 'description', label: '任务内容', value: '整理材料清单\n核对相关附件' },
  { key: 'assignees', label: '执行人员', value: '李四、王五' },
  { key: 'deadline', label: '截止时间', value: '2026-10-21 18:00' },
  { key: 'status', label: '状态', value: '进行中' },
] }
const after: OperationSnapshot = { version: 2, fields: before.fields.map(field => field.key === 'description' ? { ...field, value: '整理材料清单\n核对相关附件\n补充验收记录' } : field.key === 'deadline' ? { ...field, value: '2026-10-22 18:00' } : field) }
const task = { type: 'TASK', id: 'preview-task-1024', label: '任务 #1024' }
const records: OperationRecord[] = [
  { id: 'preview-task-update', occurredAt: '2026-10-20T03:30:00Z', actor, module: 'TASK', action: 'UPDATE', source: 'ANDROID', result: 'SUCCESS', object: task, summary: '修改任务 #1024 的内容和截止时间' },
  { id: 'preview-journal-update', occurredAt: '2026-10-20T03:10:00Z', actor: { id: 'preview-user-2', displayName: '李四', loginName: 'lisi' }, module: 'WORK_LOG', action: 'UPDATE', source: 'WEB', result: 'SUCCESS', object: { type: 'WORK_LOG', id: 'preview-journal-208', label: '工作日报 #208' }, summary: '修改工作日报 #208' },
  { id: 'preview-journal-delete', occurredAt: '2026-10-20T02:40:00Z', actor, module: 'WORK_LOG', action: 'DELETE', source: 'ANDROID', result: 'SUCCESS', object: { type: 'WORK_LOG', id: 'preview-journal-207', label: '工作日报 #207' }, summary: '删除工作日报 #207' },
  { id: 'preview-login-failure', occurredAt: '2026-10-20T02:15:00Z', actor: { id: null, displayName: '未认证用户', loginName: 'wangwu' }, module: 'AUTH', action: 'LOGIN', source: 'WEB', result: 'FAILURE', object: { type: 'ACCOUNT', id: null, label: '员工账号' }, summary: '登录账号验证失败' },
  { id: 'preview-task-create', occurredAt: '2026-10-20T01:30:00Z', actor: { id: 'preview-user-2', displayName: '李四', loginName: 'lisi' }, module: 'TASK', action: 'CREATE', source: 'WEB', result: 'SUCCESS', object: task, summary: '创建任务 #1024' },
  ...Array.from({ length: 22 }, (_, index): OperationRecord => ({ id: `preview-journal-create-${index}`, occurredAt: new Date(Date.parse('2026-10-19T09:30:00Z') - index * 20 * 60_000).toISOString(), actor: { id: `preview-user-${index % 3 + 1}`, displayName: ['张三', '李四', '王五'][index % 3], loginName: ['zhangsan', 'lisi', 'wangwu'][index % 3] }, module: 'WORK_LOG', action: 'CREATE', source: index % 2 ? 'WEB' : 'ANDROID', result: 'SUCCESS', object: { type: 'WORK_LOG', id: `preview-journal-${300 + index}`, label: `工作日报 #${300 + index}` }, summary: `新增工作日报 #${300 + index}` })),
]
function journal(version: number, content: string): OperationSnapshot { return { version, fields: [{ key: 'date', label: '日报日期', value: '2026-10-20' }, { key: 'content', label: '日报内容', value: content }, { key: 'author', label: '填写人员', value: '李四' }] } }
function detail(row: OperationRecord): OperationDetail {
  const base: OperationDetail = { ...row, requestId: `preview-operation-${row.id}`, failureReason: row.result === 'FAILURE' ? '登录账号或密码不正确' : null, before: null, after: null, versions: [] }
  if (row.module === 'TASK') {
    base.before = row.action === 'UPDATE' ? before : null; base.after = row.action === 'UPDATE' ? after : before
    base.versions = [{ logId: 'preview-task-update', version: 2, occurredAt: records[0].occurredAt, actorName: '张三', action: 'UPDATE' }, { logId: 'preview-task-create', version: 1, occurredAt: records[4].occurredAt, actorName: '李四', action: 'CREATE' }]
  } else if (row.module === 'WORK_LOG') {
    base.before = row.action !== 'CREATE' ? journal(1, '核对材料清单\n整理相关附件') : null
    base.after = row.action === 'DELETE' ? null : journal(row.action === 'UPDATE' ? 2 : 1, row.action === 'UPDATE' ? '核对材料清单\n整理相关附件\n完成记录归档' : '核对材料清单\n整理相关附件')
    base.versions = [{ logId: row.id, version: base.after?.version ?? 1, occurredAt: row.occurredAt, actorName: row.actor.displayName, action: row.action }]
  }
  return base
}
export async function previewLogRequest<T>(path: string, options?: { signal?: AbortSignal }): Promise<T> {
  options?.signal?.throwIfAborted()
  const url = new URL(path, 'http://localhost'), params = url.searchParams
  if (url.pathname === '/admin/operation-logs') {
    const keyword = params.get('keyword')?.trim().toLocaleLowerCase()
    const items = records.filter(row => ['module', 'action', 'source', 'result'].every(key => !params.get(key) || params.get(key) === row[key as keyof OperationRecord]) && (!keyword || `${row.actor.displayName} ${row.actor.loginName ?? ''} ${row.object.label} ${row.summary}`.toLocaleLowerCase().includes(keyword)) && (!params.get('from') || Date.parse(row.occurredAt) >= Date.parse(params.get('from')!)) && (!params.get('to') || Date.parse(row.occurredAt) < Date.parse(params.get('to')!)))
    const offset = Number(params.get('cursor') ?? 0), limit = Number(params.get('limit') ?? 20)
    if (!Number.isSafeInteger(offset) || offset < 0 || !Number.isSafeInteger(limit) || limit < 1 || limit > 200) throw new Error('预览分页参数无效')
    return { items: items.slice(offset, offset + limit), total: items.length, nextCursor: offset + limit < items.length ? String(offset + limit) : null, snapshotToken: 'local-operation-preview-v1' } as T
  }
  const row = records.find(row => row.id === decodeURIComponent(url.pathname.split('/').at(-1) ?? ''))
  if (!row) throw new Error('找不到该预览日志')
  return detail(row) as T
}
