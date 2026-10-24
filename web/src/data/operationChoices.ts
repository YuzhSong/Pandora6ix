const labels: Record<string, string> = {
  CREATE: '新增记录', UPDATE: '修改记录', DELETE: '删除记录', DISPATCH: '派发任务', PROGRESS: '更新任务进度',
  SUBMIT: '提交任务完成申请', APPROVE: '通过任务审核', REJECT: '驳回任务申请', ACCEPT: '验收任务',
  ENABLE: '启用账号', DISABLE: '停用账号', ROLE_CHANGE: '调整账号权限', PASSWORD_RESET: '重置员工密码', LOGIN: '账号登录', EXPORT: '导出数据',
  DEPARTURE: '办理员工离职', JOB_CHANGE: '调整员工岗位', MANAGER_REPLACEMENT: '更换组织负责人',
}
const actions: Record<string, string[]> = {
  TASK: ['CREATE', 'UPDATE', 'DELETE', 'DISPATCH', 'PROGRESS', 'SUBMIT', 'APPROVE', 'REJECT', 'ACCEPT'],
  WORK_LOG: ['CREATE', 'UPDATE', 'DELETE'],
  ORGANIZATION: ['CREATE', 'UPDATE', 'DELETE', 'MANAGER_REPLACEMENT'],
  PERSONNEL: ['CREATE', 'UPDATE', 'DELETE', 'DEPARTURE', 'JOB_CHANGE', 'MANAGER_REPLACEMENT'],
  ACCOUNT: ['CREATE', 'UPDATE', 'ENABLE', 'DISABLE', 'ROLE_CHANGE', 'PASSWORD_RESET', 'EXPORT'], AUTH: ['LOGIN'],
}
const objects: Record<string, string> = { TASK: '任务', WORK_LOG: '工作日报', ORGANIZATION: '部门或团队', PERSONNEL: '人员资料', ACCOUNT: '账号' }
export function operationChoices(module: string): { value: string; label: string }[] {
  const choices = actions[module] ?? Object.keys(labels)
  return [{ value: '', label: '全部操作' }, ...choices.map(value => ({ value, label: objects[module] && ['CREATE', 'UPDATE', 'DELETE'].includes(value) ? `${value === 'CREATE' ? '新增' : value === 'UPDATE' ? '修改' : '删除'}${objects[module]}` : labels[value] }))]
}
