import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { adminRepository } from '../data/adminRepository.ts'
import { roleLabels } from '../data/types.ts'
import type { AdminData, CompanyImportantItem, Enterprise, OrgUnit, Person, Role, Tag, UnitKind } from '../data/types.ts'
import { createEditorDraft } from '../data/editorDraft.ts'
import type { Draft } from '../data/editorDraft.ts'
import { Icon } from './Icon.tsx'

export type EditRequest =
  | { kind: 'enterprise'; item: Enterprise }
  | { kind: 'unit'; item?: OrgUnit; parentId?: string }
  | { kind: 'person'; item?: Person; orgUnitId?: string }
  | { kind: 'roles'; item: Person }
  | { kind: 'tag'; item?: Tag }
  | { kind: 'company'; item?: CompanyImportantItem }


export function AdminEditor({ request, data, onClose, onSaved }: {
  request: EditRequest; data: AdminData; onClose: () => void; onSaved: (data: AdminData) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [initial] = useState(() => createEditorDraft(request, data))
  const [draft, setDraft] = useState(initial)
  const [error, setError] = useState('')
  const errorFeedback = useRef<HTMLParagraphElement>(null)
  useEffect(() => { if (error) errorFeedback.current?.focus() }, [error])
  const [saving, setSaving] = useState(false)
  const [confirmDiscard, setConfirmDiscard] = useState(false)
  const dirty = JSON.stringify(initial) !== JSON.stringify(draft)
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
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => { setDraft(current => ({ ...current, [key]: value })); setConfirmDiscard(false) }
  const close = () => { if (!saving) { if (dirty) setConfirmDiscard(true); else onClose() } }
  const title = request.kind === 'enterprise' ? '编辑公司信息' : request.kind === 'roles' ? '配置人员角色' : `${request.item ? '编辑' : '新增'}${request.kind === 'company' ? '公司重要事项' : request.kind === 'unit' ? '组织' : request.kind === 'person' ? '人员' : '标签'}`
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    setSaving(true); setError(''); setConfirmDiscard(false)
    try {
      let next: AdminData
      if (request.kind === 'enterprise') next = await adminRepository.saveEnterprise(draft.name, request.item.version)
      else if (request.kind === 'unit') next = await adminRepository.saveUnit({ name: draft.name, kind: draft.kind, parentId: draft.kind === 'COMPANY' ? null : draft.parentId || null }, request.item?.id, request.item?.version)
      else if (request.kind === 'tag') next = await adminRepository.saveTag({ name: draft.name, colorHex: draft.colorHex }, request.item?.id, request.item?.version)
      else if (request.kind === 'company') next = await adminRepository.saveCompanyItem({ title: draft.name, content: draft.content, displayOrder: Number(draft.displayOrder) }, request.item?.id, request.item?.version)
      else if (request.kind === 'roles') next = await adminRepository.saveRoles(request.item.id, draft.roles, request.item.version)
      else next = await adminRepository.savePerson({ displayName: draft.name, loginName: draft.loginName, jobTitle: draft.jobTitle, orgUnitId: draft.orgUnitId, supervisorId: draft.supervisorId || null, roles: draft.roles }, request.item?.id, request.item?.version)
      onSaved(next)
    } catch (reason) { setError(reason instanceof Error ? reason.message : '保存失败，请重试。') }
    finally { setSaving(false) }
  }
  return <><dialog ref={dialog} className="editor-dialog" aria-labelledby="editor-title" onCancel={event => { event.preventDefault(); close() }} onClick={event => { if (event.target === event.currentTarget) close() }}>
    <form onSubmit={save}>
      <div className="dialog-head"><div><h2 id="editor-title">{title}</h2></div><button type="button" className="icon-button" aria-label="关闭编辑窗口" onClick={close} disabled={saving}><Icon name="close" /></button></div>
      <div className="dialog-body">
        {request.kind === 'enterprise' && <label><span className="field-label">公司名称 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} /></label>}
        {request.kind === 'company' && <><label><span className="field-label">事项标题 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={255} value={draft.name} onChange={event => set('name', event.target.value)} /></label><label>事项内容<textarea rows={7} value={draft.content} onChange={event => set('content', event.target.value)} placeholder="填写事项说明、时间安排或注意事项" /></label><label><span className="field-label">显示顺序 <span className="required" aria-hidden="true">*</span></span><input type="number" required step={1} min={-2147483648} max={2147483647} value={draft.displayOrder} onChange={event => set('displayOrder', event.target.value)} /></label></>}
        {request.kind === 'unit' && <>
          <label><span className="field-label">组织名称 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} placeholder="例如：产品研发部" /></label>
          <div className="field-grid"><label>组织类型<select value={draft.kind} disabled={request.item?.kind === 'COMPANY'} onChange={event => { const kind = event.target.value as UnitKind; setDraft(current => ({ ...current, kind, parentId: data.units.find(unit => unit.kind === (kind === 'TEAM' ? 'DEPARTMENT' : 'COMPANY'))?.id ?? '' })) }}>
            {request.item?.kind === 'COMPANY' && <option value="COMPANY">公司</option>}<option value="DEPARTMENT">部门</option><option value="TEAM">团队</option>
          </select></label>{draft.kind !== 'COMPANY' && <label>上级组织<select required value={draft.parentId} onChange={event => set('parentId', event.target.value)}>{!draft.parentId && <option value="" disabled>暂无可选上级组织</option>}{data.units.filter(unit => unit.id !== request.item?.id && unit.kind === (draft.kind === 'TEAM' ? 'DEPARTMENT' : 'COMPANY')).map(unit => <option key={unit.id} value={unit.id}>{unit.name}</option>)}</select></label>}</div>

        </>}
        {(request.kind === 'person' || request.kind === 'roles') && <>
          {request.kind === 'roles' ? <div className="person-summary"><span className="avatar">{draft.name.slice(0, 1)}</span><div><strong>{draft.name}</strong><span>{draft.loginName} · {draft.jobTitle || '未设置职位'}</span></div></div> : <>
            <div className="field-grid"><label><span className="field-label">姓名 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={128} value={draft.name} onChange={event => set('name', event.target.value)} placeholder="人员姓名" /></label><label><span className="field-label">登录名 <span className="required" aria-hidden="true">*</span></span><input required maxLength={128} value={draft.loginName} onChange={event => set('loginName', event.target.value)} placeholder="例如：chen.sining" /></label></div>
            <label>职位<input maxLength={128} value={draft.jobTitle} onChange={event => set('jobTitle', event.target.value)} placeholder="例如：前端开发工程师" /></label>
            <div className="field-grid"><label><span className="field-label">所属组织 <span className="required" aria-hidden="true">*</span></span><select required value={draft.orgUnitId} onChange={event => set('orgUnitId', event.target.value)}>{data.units.map(unit => <option key={unit.id} value={unit.id}>{unit.name}</option>)}</select></label><label>直属上级<select value={draft.supervisorId} onChange={event => set('supervisorId', event.target.value)}><option value="">未设置直属上级</option>{data.people.filter(person => person.id !== request.item?.id).map(person => <option key={person.id} value={person.id}>{person.displayName} · {person.jobTitle || person.loginName}</option>)}</select></label></div>
            
          </>}
          {request.kind === 'roles' && <fieldset className="role-picker"><legend>业务角色 <span className="required">*</span></legend><div>{(Object.keys(roleLabels) as Role[]).map(role => <label key={role}><input type="checkbox" checked={draft.roles.includes(role)} onChange={event => set('roles', event.target.checked ? [...draft.roles, role] : draft.roles.filter(item => item !== role))} />{roleLabels[role]}</label>)}</div></fieldset>}
        </>}
        {request.kind === 'tag' && <>
          <label><span className="field-label">标签名称 <span className="required" aria-hidden="true">*</span></span><input autoFocus required maxLength={64} value={draft.name} onChange={event => set('name', event.target.value)} placeholder="例如：产品研发" /></label>
          <label><span className="field-label">标签颜色 <span className="required" aria-hidden="true">*</span></span><div className="color-control"><input type="color" aria-label="选择标签颜色" value={/^#[\da-f]{6}$/i.test(draft.colorHex) ? draft.colorHex : '#4F70E8'} onChange={event => set('colorHex', event.target.value.toUpperCase())} /><input required aria-label="颜色十六进制值" maxLength={7} value={draft.colorHex} onChange={event => set('colorHex', event.target.value)} placeholder="#4F70E8" /></div></label>
          <div className="tag-preview"><span>显示预览</span><span className="tag-chip"><i style={{ background: /^#[\da-f]{6}$/i.test(draft.colorHex) ? draft.colorHex : '#4F70E8' }} />{draft.name || '标签名称'}</span></div>
          
        </>}
        {error && <p ref={errorFeedback} className="error-message" role="alert" tabIndex={-1}>{error}</p>}

      </div>
      <div className="dialog-footer"><button className="button secondary" type="button" onClick={close} disabled={saving}>取消</button><button className="button primary" type="submit" disabled={saving || (!!request.item && !dirty)}>{saving ? '正在保存…' : request.item ? '保存修改' : '创建'}<Icon name="check" size={17} /></button></div>
    </form>
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
