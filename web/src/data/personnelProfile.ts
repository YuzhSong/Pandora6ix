import type { Person } from './types.ts'

export const profilePendingMessage = '当前服务尚未支持性别、身份证号和电话号码的完整保存。请等待后端接口更新后再创建人员或修改这些资料。'
export const genderLabels = { MALE: '男', FEMALE: '女' } as const

export function supportsPersonnelProfile(people: Person[]) {
  return people.length > 0 && people.every(person => person.gender !== undefined && person.idNumber !== undefined && person.phoneNumber !== undefined)
}

export function validatePersonnelProfile(profile: Pick<Person, 'gender' | 'idNumber' | 'phoneNumber'>) {
  if (!profile.gender || !Object.hasOwn(genderLabels, profile.gender)) throw new Error('请选择性别。')
  if (!/^(?:\d{15}|\d{17}[\dX])$/.test(profile.idNumber ?? '')) throw new Error('请填写 15 位或 18 位身份证号，末位 X 使用大写。')
  if (!/^\+?[\d ()-]{6,24}$/.test(profile.phoneNumber ?? '') || (profile.phoneNumber?.replace(/\D/g, '').length ?? 0) < 6) throw new Error('请填写有效的电话号码，可包含区号、空格或短横线。')
}

export function maskIdentity(value?: string | null) {
  return value ? `${value.slice(0, 3)}${'*'.repeat(Math.max(0, value.length - 7))}${value.slice(-4)}` : '未填写'
}
