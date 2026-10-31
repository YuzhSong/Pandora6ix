import { followingOrganization } from './personnelHistory.ts'
import type { AdminData, AdminRepository, PersonInput } from './types.ts'
import type { CreatedAccountReceipt } from './accountHandoff.ts'
import { errorMessage } from '../api/feedback.ts'
import { validatePersonnelProfile } from './personnelProfile.ts'
import { validatePassword } from '../api/auth.ts'
import { organizationFilterOptions } from './organizationHierarchy.ts'
import { canHaveSupervisor, reportingError } from './supervisors.ts'
import { generateLoginName, generateInitialPassword, personnelRole, populatedPersonnelRow, preparePersonnelRow, validateRoleOrganization } from './personnelCreation.ts'
import { roleLabels } from './types.ts'

export const bulkPersonnelHeaders = ['姓名', '登录账号', '性别', '身份证号', '电话号码', '业务角色', '所属组织', '直属上级账号']
const matchesHeader = (header: string[]) => header.map((value, index) => index === 5 && value.trim() === '职位' ? '业务角色' : value.trim()).join(',') === bulkPersonnelHeaders.join(',')
export interface BulkPersonnelRow { rowNumber: number; input: PersonInput; initialPassword: string }
export interface BulkPersonnelPreview { rows: BulkPersonnelRow[]; errors: string[] }

function csvRows(text: string): string[][] {
  const rows: string[][] = []; let row: string[] = [], value = '', quoted = false, closed = false
  const source = text.replace(/^\ufeff/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  for (let index = 0; index < source.length; index++) {
    const char = source[index]
    if (quoted) {
      if (char === '"') { if (source[index + 1] === '"') { value += '"'; index++ } else { quoted = false; closed = true } }
      else value += char
    } else if (char === '"') {
      if (value || closed) throw new Error('CSV 引号格式不正确，请使用模板并保存为 UTF-8 CSV。')
      quoted = true
    } else if (char === ',' || char === '\n') {
      row.push(value); value = ''; closed = false
      if (char === '\n') { rows.push(row); row = [] }
    } else {
      if (closed) throw new Error('CSV 引号后只能是逗号或换行。')
      value += char
    }
  }
  if (quoted) throw new Error('CSV 存在未闭合的引号。')
  if (value || row.length || closed) { row.push(value); rows.push(row) }
  return rows.filter(row => row.some(cell => cell.trim()))
}

export function previewBulkPersonnel(text: string, data: AdminData): BulkPersonnelPreview {
  const errors: string[] = [], rows: BulkPersonnelRow[] = []
  if (text.length > 1_000_000) return { rows, errors: ['文件过大，请每次最多导入 100 人。'] }
  let parsed: string[][]
  try { parsed = csvRows(text) } catch (error) { return { rows, errors: [error instanceof Error ? error.message : 'CSV 无法读取。'] } }
  const [header, ...entries] = parsed
  if (!header || !matchesHeader(header)) return { rows, errors: ['表头不匹配，请下载模板，保留全部 8 列及顺序。'] }
  if (!entries.length || entries.length > 100) return { rows, errors: ['每批需要 1～100 位人员。'] }
  const loginNames = new Set(data.people.map(person => person.loginName.trim().toLowerCase()))
  const organizations = organizationFilterOptions(data.units)
  entries.forEach((entry, index) => {
    const rowNumber = index + 2
    try {
      if (entry.length !== header.length) throw new Error('列数不匹配')
      const [rawName, rawLogin, rawGender, rawIdentity, rawPhone, rawJob, rawOrganization, rawSupervisor] = entry
      const initialPassword = generateInitialPassword()
      const role = personnelRole(rawJob.trim())
      const displayName = rawName.trim(), loginName = rawLogin.trim() || generateLoginName(displayName, Array.from(loginNames, loginName => ({ loginName }))), jobTitle = roleLabels[role]
      if (!displayName || displayName.length > 128 || !loginName || loginName.length > 128 || jobTitle.length > 128) throw new Error('姓名/账号必填，姓名、账号和职位最多 128 字符')
      if (loginNames.has(loginName.toLowerCase())) throw new Error('登录账号在现有人员或导入文件中重复')
      loginNames.add(loginName.toLowerCase())
      validatePassword(initialPassword)
      const gender: PersonInput['gender'] = rawGender.trim() === '男' || rawGender.trim().toUpperCase() === 'MALE' ? 'MALE' : rawGender.trim() === '女' || rawGender.trim().toUpperCase() === 'FEMALE' ? 'FEMALE' : null
      const profile = { gender, idNumber: rawIdentity.trim().toUpperCase(), phoneNumber: rawPhone.trim() }
      validatePersonnelProfile(profile)
      const organization = rawOrganization.trim()
      const matches = organizations.filter(unit => unit.accessibleLabel === organization || unit.value === organization || unit.label === organization)
      if (organization && matches.length !== 1) throw new Error('所属组织不存在或名称重复，请使用完整组织路径')
      const supervisorLogin = canHaveSupervisor([role]) ? rawSupervisor.trim() : ''
      const supervisor = data.people.find(person => person.loginName === supervisorLogin)
      if (supervisorLogin && !supervisor) throw new Error('直属上级账号不存在，请先创建并配置负责人角色')
      const orgUnitId = matches[0]?.value ?? followingOrganization(data, [role], supervisor?.id ?? '') ?? ''
      validateRoleOrganization(data, role, orgUnitId)
      const reporting = reportingError(data.people, undefined, [role], supervisor?.id ?? '')
      if (reporting) throw new Error(reporting)
      rows.push({ rowNumber, initialPassword, input: { displayName, loginName, jobTitle, orgUnitId, supervisorId: supervisor?.id ?? null, roles: [role], ...profile } })
    } catch (error) { errors.push(`第 ${rowNumber} 行：${error instanceof Error ? error.message : '资料不符合要求'}`) }
  })
  return { rows, errors }
}

export function bulkPersonnelTemplate() { return bulkPersonnelHeaders.join(',') + '\r\n' }

export function importPersonnelTable(text: string, data: AdminData): string[][] {
  if (text.length > 1_000_000) throw new Error('文件过大，请每批最多 100 人。')
  const [header, ...rows] = csvRows(text)
  if (!header || !matchesHeader(header)) throw new Error('表头不匹配，请使用 CSV 模板。')
  if (!rows.length || rows.length > 100) throw new Error('每批需要 1～100 位人员。')
  const organizations = organizationFilterOptions(data.units)
  return rows.map((row, index) => {
    if (row.length !== 8) throw new Error(`第 ${index + 2} 行：列数不匹配。`)
    const cells = [...row.slice(0, 2), '', ...row.slice(2)]
    if (cells[3].trim().toUpperCase() === 'MALE') cells[3] = '男'
    else if (cells[3].trim().toUpperCase() === 'FEMALE') cells[3] = '女'
    else cells[3] = cells[3].trim()
    const matches = organizations.filter(option => [option.value, option.accessibleLabel, option.label].includes(cells[7].trim()))
    cells[7] = matches.length === 1 ? matches[0].value : cells[7].trim()
    cells[8] = cells[8].trim()
    try { if (!canHaveSupervisor([personnelRole(cells[6].trim())])) cells[8] = '' } catch { /* Invalid imported roles remain visible for correction. */ }
    return preparePersonnelRow(cells)
  })
}

export function previewPersonnelTable(cells: string[][], data: AdminData): BulkPersonnelPreview {
  const populated = cells.map((row, index) => ({ row, index })).filter(({ row }) => populatedPersonnelRow(row))
  const csv = [bulkPersonnelHeaders, ...populated.map(({ row }) => row.filter((_, index) => index !== 2))].map(row => row.map(value => '"' + value.replaceAll('"', '""') + '"').join(',')).join('\r\n')
  const result = previewBulkPersonnel(csv, data)
  return {
    rows: result.rows.map(row => ({ ...row, initialPassword: populated[row.rowNumber - 2]?.row[2] || row.initialPassword, rowNumber: (populated[row.rowNumber - 2]?.index ?? 0) + 1 })),
    errors: result.errors.map(error => error.replace(/^第 (\d+) 行：/, (_, row: string) => `第 ${(populated[Number(row) - 2]?.index ?? 0) + 1} 行：`)),
  }
}

export async function createBulkPersonnel(rows: BulkPersonnelRow[], repository: Pick<AdminRepository, 'savePerson'>, callbacks: {
  stopped: () => boolean
  onRow: (index: number, status: 'creating' | 'success' | 'failed', message?: string) => void
  onCreated: (data: AdminData, receipt: CreatedAccountReceipt) => void
}) {
  for (const [index, row] of rows.entries()) {
    if (callbacks.stopped()) break
    callbacks.onRow(index, 'creating')
    try {
      const data = await repository.savePerson(row.input, undefined, undefined, row.initialPassword)
      const created = data.people.find(person => person.loginName === row.input.loginName)
      if (!created || created.displayName !== row.input.displayName) throw new Error('服务器已处理请求，但返回账号无法确认。')
      callbacks.onCreated(data, { personId: created.id, loginName: created.loginName, displayName: created.displayName, enterpriseDomain: data.enterprise.loginDomain, initialPassword: row.initialPassword })
      callbacks.onRow(index, 'success')
    } catch (reason) {
      callbacks.onRow(index, 'failed', `${errorMessage(reason)} 已停止后续创建，请关闭并刷新资料，核实本行是否已创建后再处理，勿重复导入。`)
      break
    }
  }
}
