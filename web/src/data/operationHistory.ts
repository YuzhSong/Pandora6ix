export const logModules = { TASK: '任务', WORK_LOG: '工作日报', ORGANIZATION: '组织', PERSONNEL: '人员', ACCOUNT: '账号与权限', AUTH: '登录' } as const
export const logActions = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', DISPATCH: '派发', PROGRESS: '更新进度', SUBMIT: '提交完成', APPROVE: '审核通过', REJECT: '驳回', ACCEPT: '验收', ENABLE: '启用', DISABLE: '停用', ROLE_CHANGE: '调整权限', PASSWORD_RESET: '重置密码', LOGIN: '登录', EXPORT: '导出', DEPARTURE: '办理离职', JOB_CHANGE: '岗位调整', MANAGER_REPLACEMENT: '更换负责人' } as const
export const logSources = { WEB: 'Web', ANDROID: 'Android', SYSTEM: '系统' } as const
export const logResults = { SUCCESS: '成功', FAILURE: '失败' } as const

export interface OperationRecord {
  id: string
  occurredAt: string
  actor: { id: string | null; displayName: string | null; loginName: string | null }
  module: string
  action: string
  source: string
  result: 'SUCCESS' | 'FAILURE'
  object: { type: string; id: string | null; label: string | null }
  summary: string
}
export interface SnapshotField { key: string; label: string; value: string | null }
export interface OperationSnapshot { version: number | null; fields: SnapshotField[] }
export interface OperationVersion { logId: string; version: number | null; occurredAt: string; actorName: string | null; action: string }
export interface OperationDetail extends OperationRecord {
  requestId: string | null
  failureReason: string | null
  before: OperationSnapshot | null
  after: OperationSnapshot | null
  versions: OperationVersion[]
}
export interface OperationPage { items: OperationRecord[]; total: number; nextCursor: string | null; snapshotToken: string }
export interface OperationFilters { keyword: string; startDate: string; endDate: string; module: string; action: string; source: string; result: string }
export const emptyLogFilters: OperationFilters = { keyword: '', startDate: '', endDate: '', module: '', action: '', source: '', result: '' }

export function logLabel(dictionary: Readonly<Record<string, string>>, value: string): string { return dictionary[value] ?? '其他' }
export function logTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(value))
}
export function dateRangeValid(filters: OperationFilters): boolean { return !filters.startDate || !filters.endDate || filters.startDate <= filters.endDate }
// Defence in depth only: the server MUST omit secrets before serializing a snapshot.
export function visibleSnapshotFields(snapshot: OperationSnapshot | null): SnapshotField[] {
  return (snapshot?.fields ?? []).filter(field => !/(password|passwd|pwd|secret|token|credential|idnumber|identitynumber|idcard|身份证|密码|密钥|令牌)/i.test(`${field.key.replace(/[^a-z\d\u4e00-\u9fff]/gi, '')} ${field.label}`))
}
export function compareSnapshots(before: OperationSnapshot | null, after: OperationSnapshot | null) {
  const previous = new Map(visibleSnapshotFields(before).map(field => [field.key, field]))
  const current = new Map(visibleSnapshotFields(after).map(field => [field.key, field]))
  return [...new Set([...current.keys(), ...previous.keys()])].map(key => {
    const left = previous.get(key), right = current.get(key)
    return { key, label: right?.label ?? left!.label, before: left?.value ?? null, after: right?.value ?? null, changed: !left || !right || left.value !== right.value }
  })
}
