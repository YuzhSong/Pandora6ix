import { ApiError } from './client.ts'

const messages: Record<string, string> = {
  INVALID_CREDENTIALS: '登录未成功，请核对登录信息后重试。已保留填写内容。',
  CURRENT_PASSWORD_INVALID: '当前密码不正确，请重新输入。',
  LAST_ADMIN_REQUIRED: '公司必须保留至少一名启用的系统管理员。',
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
