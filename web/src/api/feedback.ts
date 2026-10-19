import { ApiError } from './client.ts'

const messages: Record<string, string> = {
  MANAGER_SUCCESSOR_REQUIRED: '该组织需要保留负责人，请先使用“更换负责人”。',
  INVALID_MANAGER_SUCCESSOR: '接任人员不符合条件，请选择在职且启用的普通员工，或本组织同级负责人；其他组织负责人请先完成岗位调整。',
  EMPLOYEE_DEPARTED: '该员工已离职，不能继续修改账号或岗位。',
  SELF_DEPARTURE_FORBIDDEN: '不能为当前登录账号办理离职，请由其他管理员操作。',
  SELF_JOB_CHANGE_FORBIDDEN: '当前账号的岗位调整请由其他管理员操作。',
  DIRECT_REPORTS_REMAIN: '该员工仍有在职直属下属，请先调整上下级关系。',
  REPORTING_ADJUSTMENT_REQUIRED: '岗位调整后与直属下属职级不匹配，请先调整上下级关系。',
  HANDOFF_KEY_NOT_CONFIGURED: '初始密码导出未配置，请联系系统维护人员。',
  HANDOFF_UNAVAILABLE: '初始密码暂时无法读取，请联系系统维护人员。',
  INVALID_CREDENTIALS: '登录未成功，请核对登录信息后重试。已保留填写内容。',
  CURRENT_PASSWORD_INVALID: '当前密码不正确，请重新输入。',
  LAST_ADMIN_REQUIRED: '公司必须保留至少一名启用的系统管理员。',
  LOGIN_NAME_TAKEN: '该登录名已被公司内其他账号使用，请换一个登录名。',
  ORG_ROOT_DELETE_FORBIDDEN: '公司根组织不能删除。',
  ORG_HAS_CHILDREN: '该组织仍有下级组织，请先调整或删除下级组织，刷新后再试。',
  ORG_HAS_MEMBERS: '该组织仍有直属人员（包括停用账号），请先转移人员，刷新后再试。',
  VERSION_CONFLICT: '资料已被其他人更新，请关闭编辑窗口、刷新资料后重新编辑。',
  IDEMPOTENCY_CONFLICT: '本次创建的内容已变化，请确认后重新提交。',
  UNAUTHENTICATED: '登录状态已失效，请重新登录。',
  FORBIDDEN: '此账号没有执行该操作的权限。',
  ADMIN_PASSWORD_RESET_FORBIDDEN: '管理员账号请使用本人修改密码功能。',
}
const fields: Record<string, string> = { gender: '性别', idNumber: '身份证号', phoneNumber: '电话号码', name: '名称', enterpriseDomain: '公司登录标识', loginDomain: '公司登录标识', loginName: '登录名', password: '密码', adminPassword: '管理员密码', adminDisplayName: '管理员姓名', displayName: '姓名', businessTimezone: '业务时区', content: '事项内容', title: '事项标题', colorHex: '标签颜色', roles: '角色', orgUnitId: '所属组织', supervisorUserId: '直属上级', newPassword: '新密码', currentPassword: '当前密码', displayOrder: '显示顺序' }
export function errorMessage(reason: unknown): string {
  if (!(reason instanceof Error)) return '暂时无法完成操作，请重试。'
  if (!(reason instanceof ApiError)) return reason.message
  if (reason.code === 'VALIDATION_FAILED' && reason.fieldErrors.length) {
    const invalid = reason.fieldErrors.flatMap(value => value && typeof value === 'object' && 'field' in value && typeof value.field === 'string' ? [fields[value.field] ?? value.field] : [])
    return invalid.length ? `请检查以下字段：${[...new Set(invalid)].join('、')}。` : reason.message
  }
  return messages[reason.code] ?? reason.message
}
