import type { AdminData, Role, UnitKind } from './types.ts'
import type { EditRequest } from '../components/AdminEditor.tsx'

export interface Draft {
  name: string; loginName: string; jobTitle: string; orgUnitId: string
  supervisorId: string; roles: Role[]; kind: UnitKind; parentId: string; colorHex: string
  content: string; displayOrder: string
  gender: '' | 'MALE' | 'FEMALE'; idNumber: string; phoneNumber: string
}

export function createEditorDraft(request: EditRequest, data: AdminData): Draft {
  const rootId = data.units.find(unit => unit.kind === 'COMPANY')?.id ?? ''
  const draft: Draft = { name: '', loginName: '', jobTitle: '', orgUnitId: rootId, supervisorId: '', roles: ['EMPLOYEE'], kind: 'DEPARTMENT', parentId: rootId, colorHex: '#4F70E8', content: '', displayOrder: '1', gender: '', idNumber: '', phoneNumber: '' }
  if (request.kind === 'unit' && !request.item && request.parentId) {
    const parent = data.units.find(unit => unit.id === request.parentId)
    if (parent && parent.kind !== 'TEAM') Object.assign(draft, { parentId: parent.id, kind: parent.kind === 'COMPANY' ? 'DEPARTMENT' : 'TEAM' })
  }
  if (request.kind === 'person' && !request.item && request.orgUnitId && data.units.some(unit => unit.id === request.orgUnitId)) draft.orgUnitId = request.orgUnitId
  if (request.kind === 'enterprise' && request.item) draft.name = request.item.name
  if (request.kind === 'unit' && request.item) Object.assign(draft, { name: request.item.name, kind: request.item.kind, parentId: request.item.parentId ?? '' })
  if ((request.kind === 'person' || request.kind === 'roles') && request.item) Object.assign(draft, { name: request.item.displayName, loginName: request.item.loginName, jobTitle: request.item.jobTitle, orgUnitId: request.item.orgUnitId, supervisorId: request.item.supervisorId ?? '', roles: [...request.item.roles] })
  if ((request.kind === 'person' || request.kind === 'roles') && request.item) Object.assign(draft, { gender: request.item.gender ?? '', idNumber: request.item.idNumber ?? '', phoneNumber: request.item.phoneNumber ?? '' })
  if (request.kind === 'company' && request.item) Object.assign(draft, { name: request.item.title, content: request.item.content, displayOrder: String(request.item.displayOrder) })
  return draft
}

