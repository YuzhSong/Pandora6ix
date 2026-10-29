import { supervisorOptionLabel } from '../data/personnelDisplay.ts'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { adminRepository } from '../data/repository.ts'
import { errorMessage } from '../api/feedback.ts'
import { ApiError } from '../api/client.ts'
import { roleLabels } from '../data/types.ts'
import type { AdminData, CompanyImportantItem, Enterprise, OrgUnit, Person, Role, UnitKind } from '../data/types.ts'
import { createEditorDraft } from '../data/editorDraft.ts'
import type { Draft } from '../data/editorDraft.ts'
import { Icon } from './Icon.tsx'
import { Select } from './Select.tsx'
import { organizationParents, validateOrganizationEdit } from '../data/organizationEditing.ts'
import { supportsPersonnelProfile } from '../data/personnelProfile.ts'
import { ValidatedForm } from './ValidatedForm.tsx'
import { canHaveSupervisor, reportingError, supervisorCandidates } from '../data/supervisors.ts'
import { RoleBadge } from './RoleBadge.tsx'
import type { CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { businessRoleOptions, generateInitialPassword, personnelRole, validateRoleOrganization } from '../data/personnelCreation.ts'

export type EditRequest =
  | { kind: 'enterprise'; item: Enterprise }
  | { kind: 'unit'; item?: OrgUnit; parentId?: string }
  | { kind: 'person'; item?: Person; orgUnitId?: string }
  | { kind: 'roles'; item: Person }
  | { kind: 'company'; item?: CompanyImportantItem }


export function AdminEditor({ request, data, onClose, onSaved }: {
  request: EditRequest; data: AdminData; onClose: () => void; onSaved: (data: AdminData, account?: CreatedAccountReceipt) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [initial] = useState(() => createEditorDraft(request, data))
  const [draft, setDraft] = useState(initial)
  const [error, setError] = useState('')
  const errorFeedback = useRef<HTMLParagraphElement>(null)
  useEffect(() => { if (error) errorFeedback.current?.focus() }, [error])
  const [saving, setSaving] = useState(false)
  const [initialPassword, setInitialPassword] = useState(() => request.kind === 'person' && !request.item ? generateInitialPassword() : '')
  const profileSupported = supportsPersonnelProfile(data.people)
  const parentOptions = organizationParents(data, draft.kind, request.item?.id)
  const creationKey = useRef({ command: '', key: crypto.randomUUID() })
  const [confirmDiscard, setConfirmDiscard] = useState(false)
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => { setDraft(current => ({ ...current, [key]: value })); setConfirmDiscard(false) }
  const dirty = JSON.stringify(initial) !== JSON.stringify(draft)
  const candidates = supervisorCandidates(data.people, request.item?.id, draft.roles)
  const effectiveSupervisor = canHaveSupervisor(draft.roles) ? draft.supervisorId : ''
  const invalidSupervisor = !!effectiveSupervisor && !candidates.some(person => person.id === draft.supervisorId)
  const supervisorField = canHaveSupervisor(draft.roles) ? <label>直属上级<Select label="直属上级" popupMinWidth={380} value={draft.supervisorId} onChange={value => set('supervisorId', value)} options={[{ value: '', label: '未设置直属上级' }, ...(invalidSupervisor ? [{ value: draft.supervisorId, label: `${data.people.find(person => person.id === draft.supervisorId)?.displayName ?? '原上级'}（不符合当前角色，请重新选择）` }] : []), ...candidates.map(person => ({ value: person.id, label: supervisorOptionLabel(data, person) }))]} />{invalidSupervisor && <button className="text-button" type="button" onClick={() => set('supervisorId', '')}>清除不符合层级的上级</button>}</label> : null
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    dialog.current?.showModal()
    return () => { opener?.focus() }
  }, [])
  useEffect(() => {
    if (!dirty) return
    const protect = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', protect)
    return () => window.removeEventListener('beforeunload', protect)
  }, [dirty])
  const close = () => { if (!saving) { if (dirty) setConfirmDiscard(true); else onClose() } }
  const title = request.kind === 'enterprise' ? '编辑公司信息' : request.kind === 'roles' ? '配置人员角色' : `${request.item ? '编辑' : '新增'}${request.kind === 'company' ? '公司重要事项' : request.kind === 'unit' ? '组织' : '人员'}`
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    if (request.kind === 'person' || request.kind === 'roles') {
      const invalid = reportingError(data.people, request.item?.id, draft.roles, effectiveSupervisor)
      if (invalid) { setError(invalid); return }
    }
    setSaving(true); setError(''); setConfirmDiscard(false)
    try {
      if (request.kind === 'person' && !request.item) validateRoleOrganization(data, draft.roles[0], draft.orgUnitId)
      let next: AdminData
      if (request.kind === 'enterprise') next = await adminRepository.saveEnterprise(draft.name, request.item.version)
      else if (request.kind === 'unit') {
        const input = { name: draft.name, kind: draft.kind, parentId: draft.parentId || null }
        validateOrganizationEdit(data, input, request.item?.id)
        next = await adminRepository.saveUnit(input, request.item?.id, request.item?.version)
      }
      else if (request.kind === 'company') {
        const input = { title: draft.name, content: draft.content, displayOrder: Number(draft.displayOrder) }
        const command = JSON.stringify(input)
        if (command !== creationKey.current.command) creationKey.current = { command, key: crypto.randomUUID() }
        next = await adminRepository.saveCompanyItem(input, request.item?.id, request.item?.version, creationKey.current.key)
      }
      else if (request.kind === 'roles') next = await adminRepository.savePerson({ displayName: draft.name, loginName: draft.loginName, jobTitle: draft.jobTitle, orgUnitId: draft.orgUnitId, supervisorId: effectiveSupervisor || null, roles: draft.roles }, request.item.id, request.item.version)
      else next = await adminRepository.savePerson({ displayName: draft.name, loginName: draft.loginName, jobTitle: request.item ? draft.jobTitle : roleLabels[draft.roles[0]], orgUnitId: draft.orgUnitId, supervisorId: effectiveSupervisor || null, roles: draft.roles, ...(profileSupported ? { gender: draft.gender || null, idNumber: draft.idNumber.trim().toUpperCase() || null, phoneNumber: draft.phoneNumber.trim() || null } : {}) }, request.item?.id, request.item?.version, initialPassword)
      const created = request.kind === 'person' && !request.item ? next.people.find(person => person.loginName === draft.loginName && !data.people.some(old => old.id === person.id)) : undefined
      onSaved(next, created ? { personId: created.id, displayName: created.displayName, loginName: created.loginName, enterpriseDomain: next.enterprise.loginDomain, initialPassword } : undefined)
      setInitialPassword('')
    } catch (reason) {
      const pendingTypeChange = request.kind === 'unit' && request.item && draft.kind !== request.item.kind && reason instanceof ApiError && reason.code === 'WORKFLOW_POLICY_UNCONFIRMED'
      setError(pendingTypeChange ? `当前暂不支持修改组织类型。${reason.requestId ? ` 请求编号：${reason.requestId}` : ''}` : errorMessage(reason))
    }
    finally { setSaving(false) }
  }
  return <><dialog ref={dialog} className="editor-dialog" aria-labelledby="editor-title" onCancel={event => { event.preventDefault(); close() }} onClick={event => { if (event.target === event.currentTarget) close() }}>
    <ValidatedForm onSubmit={save}>
      <div className="dialog-head"><div><h2 id="editor-title">{title}</h2></div><button type="button" className="icon-button" aria-label="关闭编辑窗口" onClick={close} disabled={saving}><Icon name="close" /></button></div>
      <div className="dialog-body">
        {request.kind === 'enterprise' && <label><span className="field-label">公司名称 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} /></label>}
        {request.kind === 'company' && <><label><span className="field-label">事项标题 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={255} value={draft.name} onChange={event => set('name', event.target.value)} /></label><label><span>事项内容 <span className="required">*</span></span><textarea required maxLength={20000} rows={7} value={draft.content} onChange={event => set('content', event.target.value)} placeholder="填写事项说明、时间安排或注意事项" /></label><label><span className="field-label">显示顺序 <span className="required" aria-hidden="true">*</span></span><input type="number" required step={1} min={1} max={2147483647} value={draft.displayOrder} onChange={event => set('displayOrder', event.target.value)} /></label></>}
        {request.kind === 'unit' && <>
          <label><span className="field-label">组织名称 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} placeholder="例如：产品研发部" /></label>
          <div className="field-grid"><label>组织类型<Select label="组织类型" value={draft.kind} disabled={request.item?.kind === 'COMPANY'} onChange={value => { const kind = value as UnitKind; setDraft(current => ({ ...current, kind, parentId: organizationParents(data, kind, request.item?.id)[0]?.id ?? '' })) }} options={[...(request.item?.kind === 'COMPANY' ? [{ value: 'COMPANY', label: '公司' }] : []), { value: 'DEPARTMENT', label: '部门' }, { value: 'TEAM', label: '团队' }]} /></label>{draft.kind !== 'COMPANY' && <label>上级组织<Select label="上级组织" required value={draft.parentId} onChange={value => set('parentId', value)} options={parentOptions.map(unit => ({ value: unit.id, label: unit.name }))} /></label>}</div>

        </>}
        {(request.kind === 'person' || request.kind === 'roles') && <>
          {request.kind === 'roles' ? <div className="person-summary"><span className="avatar">{draft.name.slice(0, 1)}</span><div><strong>{draft.name}</strong><span>{draft.loginName}</span></div></div> : <>
            <div className="field-grid"><label><span className="field-label">姓名 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} placeholder="人员姓名" /></label><label><span className="field-label">登录名 <span className="required" aria-hidden="true">*</span></span><input required maxLength={128} autoComplete="off" spellCheck={false} value={draft.loginName} onChange={event => set('loginName', event.target.value)} placeholder="例如：chen.sining" /></label></div>
            {!profileSupported && <p className="panel-note" role="status">当前暂不支持维护完整人员资料。</p>}
            <div className="field-grid"><label><span>性别 {(!request.item || !!request.item.gender) && <span className="required">*</span>}</span><Select label="性别" required={!request.item || !!request.item.gender} disabled={!profileSupported && !!request.item} value={draft.gender} onChange={value => set('gender', value as Draft['gender'])} options={[{ value: '', label: '请选择性别' }, { value: 'MALE', label: '男' }, { value: 'FEMALE', label: '女' }]} /></label><label><span>电话号码 {(!request.item || !!request.item.phoneNumber) && <span className="required">*</span>}</span><input type="tel" required={!request.item || !!request.item.phoneNumber} disabled={!profileSupported && !!request.item} maxLength={24} autoComplete="off" value={draft.phoneNumber} onChange={event => set('phoneNumber', event.target.value)} placeholder="手机号或含区号的固定电话" /></label></div>
            <label><span>身份证号 {(!request.item || !!request.item.idNumber) && <span className="required">*</span>}</span><input required={!request.item || !!request.item.idNumber} disabled={!profileSupported && !!request.item} minLength={15} maxLength={18} autoComplete="off" spellCheck={false} value={draft.idNumber} onChange={event => set('idNumber', event.target.value.toUpperCase())} placeholder="15 位或 18 位身份证号" /></label>
            {!request.item && <label>业务角色<Select label="业务角色" value={draft.roles[0]} options={businessRoleOptions} onChange={value => { const role = personnelRole(value); setDraft(current => ({ ...current, roles: [role], jobTitle: roleLabels[role], supervisorId: '' })) }} /></label>}
            <div className="field-grid"><label>所属组织<Select label="所属组织" value={draft.orgUnitId} onChange={value => set('orgUnitId', value)} options={[{ value: '', label: '未分配组织' }, ...data.units.map(unit => ({ value: unit.id, label: unit.name }))]} /></label>{supervisorField}</div>
            
          </>}
          {request.kind === 'roles' && <><fieldset className="role-picker"><legend>业务角色 <span className="required">*</span></legend><div>{(Object.keys(roleLabels) as Role[]).map(role => <label key={role}><input type="checkbox" checked={draft.roles.includes(role)} onChange={event => set('roles', event.target.checked ? [...draft.roles, role] : draft.roles.filter(item => item !== role))} /><RoleBadge role={role} /></label>)}</div></fieldset><div className="role-supervisor">{supervisorField}</div></>}
        </>}
        {error && <p ref={errorFeedback} className="error-message" role="alert" tabIndex={-1}>{error}</p>}

      </div>
      <div className="dialog-footer"><button className="button secondary" type="button" onClick={close} disabled={saving}>取消</button><button className="button primary" type="submit" disabled={saving || (!!request.item && !dirty) || (request.kind === 'person' && !request.item && !profileSupported)}>{saving ? '正在保存…' : request.item ? '保存修改' : '创建'}<Icon name="check" size={17} /></button></div>
    </ValidatedForm>
  </dialog>{confirmDiscard && <DiscardDialog onContinue={() => setConfirmDiscard(false)} onDiscard={() => { dialog.current?.close(); onClose() }} />}</>
}

function DiscardDialog({ onContinue, onDiscard }: { onContinue: () => void; onDiscard: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const opener = useRef<HTMLElement | null>(null)
  useEffect(() => {
    opener.current = document.activeElement as HTMLElement | null
    const element = dialog.current
    element?.showModal()
    return () => { element?.close() }
  }, [])
  const resume = () => { dialog.current?.close(); opener.current?.focus(); onContinue() }
  const discard = () => { dialog.current?.close(); onDiscard() }
  return <dialog ref={dialog} className="confirm-dialog" aria-labelledby="discard-title" aria-describedby="discard-description" onCancel={event => { event.preventDefault(); resume() }}>
    <div className="dialog-body"><h2 id="discard-title">放弃未保存的修改？</h2><p id="discard-description">本次修改尚未保存，放弃后无法恢复。</p></div>
    <div className="dialog-footer"><button autoFocus className="button secondary" onClick={resume}>继续编辑</button><button className="button danger" onClick={discard}>放弃修改</button></div>
  </dialog>
}
