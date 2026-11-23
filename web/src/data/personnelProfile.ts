import type { Person } from './types.ts'

export const profilePendingMessage = '当前服务尚未支持性别、身份证号和电话号码的完整保存。请等待后端接口更新后再创建人员或修改这些资料。'
export const genderLabels = { MALE: '男', FEMALE: '女' } as const
export const duplicateIdentityMessage = '该身份证号已被公司内其他人员使用。'
export const duplicatePhoneMessage = '该电话号码已被公司内其他人员使用。'

export function normalizePersonnelIdentity(value?: string | null) {
  return value?.trim().toUpperCase() ?? ''
}

export function normalizePersonnelPhone(value?: string | null) {
  return value?.replace(/\D/g, '') ?? ''
}

export function validatePersonnelName(name: string) {
  if (name.length > 128 || /\s/u.test(name) || !/^(?:\p{Script=Han}+|[A-Za-z]+)$/u.test(name)) {
    throw new Error('姓名须为 1–128 个字符，只能填写纯中文或纯英文字母，不能含空格、数字或特殊符号。')
  }
}

export function supportsPersonnelProfile(people: Person[]) {
  return people.length > 0 && people.every(person => person.gender !== undefined && person.idNumber !== undefined && person.phoneNumber !== undefined)
}

export function validatePersonnelProfile(profile: Pick<Person, 'gender' | 'idNumber' | 'phoneNumber'>, partial = false) {
  if ((!partial || profile.gender != null) && (!profile.gender || !Object.hasOwn(genderLabels, profile.gender))) throw new Error('请选择性别。')
  if ((!partial || profile.idNumber != null) && !/^(?:\d{15}|\d{17}[\dX])$/.test(profile.idNumber ?? '')) throw new Error('请填写 15 位或 18 位身份证号，末位 X 使用大写。')
  if ((!partial || profile.phoneNumber != null) && (!/^\+?[\d ()-]{6,24}$/.test(profile.phoneNumber ?? '') || normalizePersonnelPhone(profile.phoneNumber).length < 6)) throw new Error('请填写有效的电话号码，可包含区号、空格或短横线。')
}

export function maskIdentity(value?: string | null) {
  return value ? `${value.slice(0, 3)}${'*'.repeat(Math.max(0, value.length - 7))}${value.slice(-4)}` : '未填写'
}
