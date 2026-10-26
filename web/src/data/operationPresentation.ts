import { roleLabels } from './types.ts'
import type { AdminData, Role } from './types.ts'
import { compareSnapshots } from './operationHistory.ts'
import type { OperationSnapshot, OperationDetail } from './operationHistory.ts'

export function hasReportedSnapshotVersions(detail: Pick<OperationDetail, 'before' | 'after'>): boolean {
  return detail.before?.version != null || detail.after?.version != null
}
export function reportedHistory(detail: OperationDetail) {
  return detail.versions
}

const labels: Record<string, string> = {
  displayName: '姓名', loginName: '登录账号', jobTitle: '职位', orgUnitId: '所属组织', supervisorUserId: '直属上级',
  employmentStatus: '在职状态', departureDate: '离职日期', status: '账号状态', roles: '角色', name: '名称',
  title: '标题', description: '任务说明', content: '内容', body: '内容', kind: '类型', parentId: '上级组织',
  version: '版本', startDate: '开始日期', endDate: '结束日期', startMinute: '开始时间', endMinute: '结束时间',
  startTime: '开始时间', endTime: '结束时间', timeKind: '时间类型', priorityLevel: '重要程度', tagIds: '标签',
  reviewStatus: '审批状态', executionStatus: '执行状态', progressPercent: '完成进度', workDate: '日志日期',
  creatorUserId: '创建人', recipientUserId: '接收人', reviewerUserId: '审批人', actorUserId: '操作人',
  authorUserId: '记录人', ownerUserId: '所属人员', completedByUserId: '完成人', submittedByUserId: '提交人',
  successorUserId: '接任人员', formerUserId: '原负责人', entries: '工作记录', displayOrder: '显示顺序',
  gender: '性别', phoneNumber: '联系电话', colorHex: '标签颜色', businessTimezone: '业务时区',
  createdAt: '创建时间', updatedAt: '更新时间', completedAt: '完成时间', effectiveDate: '生效日期',
  loginDomain: '公司登录标识', enterpriseDomain: '公司登录标识', reason: '原因', note: '备注',
}
const states: Record<string, Record<string, string>> = {
  employmentStatus: { IN_SERVICE: '在职', DEPARTED: '已离职' },
  status: { ACTIVE: '已启用', DISABLED: '已停用' },
  reviewStatus: { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回' },
  executionStatus: { PENDING_APPROVAL: '待审批', ACTIVE: '进行中', AWAITING_ACCEPTANCE: '待验收', COMPLETED: '已完成' },
  kind: { COMPANY: '公司', DEPARTMENT: '部门', TEAM: '团队', PERSONAL: '个人任务', DELEGATED: '派发任务' },
  timeKind: { ALL_DAY: '全天', TIMED: '指定时间' }, gender: { MALE: '男', FEMALE: '女' },
}
function items(value: string): string[] {
  try { const list: unknown = JSON.parse(value); if (Array.isArray(list) && list.every(item => typeof item === 'string')) return list }
  catch { /* Java collection snapshots use [ROLE, ROLE] instead of JSON. */ }
  return value.replace(/^\[/, '').replace(/\]$/, '').split(',').map(item => item.trim()).filter(Boolean)
}
export function operationOrganizationPath(id: string, data: AdminData): string {
  const names: string[] = [], visited = new Set<string>()
  let next: string | null = id
  while (next) {
    if (visited.has(next)) { names.unshift('组织层级异常'); break }
    visited.add(next)
    const unit = data.units.find(unit => unit.id === next)
    if (!unit) { names.unshift(names.length ? '上级组织当前不可用' : '组织已删除或当前不可用'); break }
    names.unshift(unit.name); next = unit.parentId
  }
  return names.join(' → ')
}
function personPath(id: string, data: AdminData): string {
  const person = data.people.find(person => person.id === id)
  if (!person) return '人员已删除或当前不可用'
  const name = `${person.displayName}（${person.loginName}）`
  return person.orgUnitId ? `${operationOrganizationPath(person.orgUnitId, data)} → ${name}` : `未分配组织 → ${name}`
}
export function presentOperationValue(key: string, value: string | null, data: AdminData): string | null {
  if (value === null || value === '') return value
  if (key === 'jobTitle') {
    const legacy: Record<string, string> = { 部门负责人: '部门老总', 团队负责人: '团队长', 普通员工: '员工' }
    return Object.values(roleLabels).find(label => label === value) ?? (Object.hasOwn(roleLabels, value) ? roleLabels[value as Role] : Object.hasOwn(legacy, value) ? legacy[value] : '该历史记录未提供业务角色')
  }
  if (key === 'roles') return items(value).map(role => roleLabels[role as Role] ?? '其他角色').join('、') || '无角色'
  if (states[key]) return states[key][value] ?? '其他状态'
  if (key === 'orgUnitId' || key === 'parentId') return '历史组织名称未记录'
  if (/UserId$/.test(key)) return '历史人员名称未记录'
  if (key === 'tagIds') return items(value).map(id => data.tags.find(tag => tag.id === id)?.name ?? '标签当前不可用').join('、') || '无标签'
  if (key === 'progressPercent' && /^\d+$/.test(value)) return `${value}%`
  if (key === 'priorityLevel' && /^[1-5]$/.test(value)) return `${value} 星`
  if ((key === 'startMinute' || key === 'endMinute') && /^\d+$/.test(value)) return `${String(Math.floor(Number(value) / 60)).padStart(2, '0')}:${String(Number(value) % 60).padStart(2, '0')}`
  if ((key.endsWith('Date') || key === 'workDate') && /^\d{4}-\d{2}-\d{2}$/.test(value)) { const [year, month, day] = value.split('-'); return `${year}年${Number(month)}月${Number(day)}日` }
  return value
}
export function presentOperationComparison(before: OperationSnapshot | null, after: OperationSnapshot | null, data: AdminData) {
  const comparisons = compareSnapshots(before, after)
  const hasRoles = comparisons.some(field => field.key === 'roles')
  const historical = (key: string, value: string | null, snapshot: OperationSnapshot | null) => {
    if (!value) return value
    const nameKey = key === 'orgUnitId' ? 'orgUnitName' : key === 'supervisorUserId' ? 'supervisorName' : undefined
    return nameKey ? snapshot?.fields.find(field => field.key === nameKey)?.value ?? (key === 'orgUnitId' ? '历史组织名称未记录' : '历史人员名称未记录') : presentOperationValue(key, value, data)
  }
  return comparisons.filter(field => (field.key !== 'jobTitle' || !hasRoles) && !((field.key === 'orgUnitName' && comparisons.some(item => item.key === 'orgUnitId')) || (field.key === 'supervisorName' && comparisons.some(item => item.key === 'supervisorUserId')))).map(field => ({
    ...field,
    label: field.key === 'roles' ? '职位 / 角色' : labels[field.key] ?? (/[\u4e00-\u9fff]/.test(field.label) ? field.label : '其他资料'),
    before: historical(field.key, field.before, before), after: historical(field.key, field.after, after),
    rawBefore: field.before, rawAfter: field.after,
  }))
}
