import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { AdminData, Person, PersonnelChange, Role } from '../data/types.ts'
import { departureBlocker, managerRole, replacementCandidates } from '../data/personnelResponsibility.ts'
import { roleLabels } from '../data/types.ts'
import { adminRepository } from '../data/repository.ts'
import { errorMessage } from '../api/feedback.ts'
import { ApiError } from '../api/client.ts'
import { organizationFilterOptions } from '../data/organizationHierarchy.ts'
import { canHaveSupervisor, reportingError, supervisorCandidates } from '../data/supervisors.ts'
import { Select } from './Select.tsx'
import { Icon } from './Icon.tsx'
import { ValidatedForm } from './ValidatedForm.tsx'

export type PersonnelAction = 'departure' | 'job' | 'history' | 'replacement'
export function PersonnelLifecycleDialog({ person, action, data, onClose, onSaved }: { person: Person; action: PersonnelAction; data: AdminData; onClose: () => void; onSaved: (data: AdminData) => void }) {
  const dialog = useRef<HTMLDialogElement>(null), submitting = useRef(false)
  const [jobTitle] = useState(person.jobTitle), [org, setOrg] = useState(person.orgUnitId), [supervisor, setSupervisor] = useState(person.supervisorId ?? ''), [roles, setRoles] = useState<Role[]>(person.roles)
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: data.enterprise.businessTimezone || 'UTC', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
  const [date, setDate] = useState(today), [error, setError] = useState(''), [busy, setBusy] = useState(false), [blocked, setBlocked] = useState(false)
  const [history, setHistory] = useState<PersonnelChange[] | null>(null)
  const [successorId, setSuccessorId] = useState(''), [successorTitle] = useState<string>(roleLabels[managerRole(person) ?? 'EMPLOYEE']), [formerTitle] = useState('员工')
  const successors = replacementCandidates(data, person), departureError = departureBlocker(data, person)
  const reports = data.people.filter(value => value.supervisorId === person.id && value.employmentStatus !== 'DEPARTED')
  const effectiveSupervisor = canHaveSupervisor(roles) ? supervisor : ''
  const candidates = supervisorCandidates(data.people, person.id, roles)
  const title = action === 'departure' ? '办理离职' : action === 'job' ? '岗位调整' : action === 'replacement' ? '更换负责人' : '人员变动记录'
  useEffect(() => { const opener = document.activeElement as HTMLElement | null, element = dialog.current; element?.showModal(); return () => { element?.close(); opener?.focus() } }, [])
  useEffect(() => {
    if (action !== 'history') return
    let active = true
    void adminRepository.personnelChanges(person.id).then(items => { if (active) setHistory(items) }).catch(reason => { if (active) setError(errorMessage(reason)) })
    return () => { active = false }
  }, [action, person.id])
  async function submit(event: FormEvent) {
    event.preventDefault(); if (submitting.current || blocked) return
    setError('')
    if (action === 'job') {
      if (person.roles.includes('ADMIN')) { setError('系统管理员不适用岗位调整。'); return }
      const invalid = reportingError(data.people.filter(p => p.employmentStatus !== 'DEPARTED'), person.id, roles, effectiveSupervisor)
      if (invalid) { setError(invalid); return }
      if (!roles.length) { setError('请选择角色。'); return }
      const kind = data.units.find(unit => unit.id === org)?.kind
      if ((roles.includes('TEAM_LEAD') && kind !== 'TEAM') || (roles.includes('DEPARTMENT_MANAGER') && kind !== 'DEPARTMENT')) { setError('请选择与负责人角色匹配的部门或团队。'); return }
    }
    if (action === 'replacement' && !successors.some(p => p.id === successorId)) { setError('请选择接任负责人。'); return }
    submitting.current = true; setBusy(true)
    try {
      const next = action === 'replacement' ? await adminRepository.replaceManager(person.id, successorId, person.version, successors.find(p => p.id === successorId)!.version, successorTitle, formerTitle) : action === 'departure' ? await adminRepository.departPerson(person.id, date, person.version) : await adminRepository.changePersonJob(person.id, { jobTitle, orgUnitId: org, supervisorId: effectiveSupervisor || null, roles }, person.version)
      onSaved(next)
    } catch (reason) {
      setError(errorMessage(reason))
      if (!(reason instanceof ApiError) || ![400, 422].includes(reason.status)) setBlocked(true)
    } finally { submitting.current = false; setBusy(false) }
  }
  function showValue(key: string, value: unknown) {
    if (key === 'roles' && Array.isArray(value)) return value.map(role => roleLabels[role as Role] ?? role).join('、')
    if (key === 'orgUnitId') return data.units.find(unit => unit.id === value)?.name ?? '未分配'
    if (key === 'supervisorUserId') return data.people.find(p => p.id === value)?.displayName ?? '未设置'
    if (key === 'employmentStatus') return value === 'DEPARTED' ? '已离职' : '在职'
    if (key === 'status') return value === 'DISABLED' ? '已停用' : '已启用'
    return typeof value === 'string' && value ? value : '—'
  }
  const fieldLabels: Record<string, string> = { displayName: '姓名', loginName: '登录账号', jobTitle: '职位', roles: '角色', orgUnitId: '组织 ID', orgUnitName: '所属组织', supervisorUserId: '上级人员 ID', supervisorName: '直属上级', employmentStatus: '在职状态', departureDate: '离职日期', status: '账号状态' }
  const orderedHistory = [...(history ?? [])].sort((a, b) => Date.parse(a.recordedAt) - Date.parse(b.recordedAt))
  let jobChangeNumber = 0
  return <dialog ref={dialog} className={`editor-dialog credential-tools-dialog${action === 'history' ? ' personnel-history-dialog' : ''}`} aria-label={title} onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="dialog-head"><h2>{title} · {person.displayName}</h2><button className="icon-button" disabled={busy} onClick={onClose} aria-label={'关闭' + title}><Icon name="close" /></button></div>
    <ValidatedForm onSubmit={event => void submit(event)}>
      <div className="dialog-body">
        {action === 'replacement' && !successors.length && <p className="error-message" role="alert">暂无符合条件的接任人员，请先新增员工或调整人员岗位。</p>}
        {action === 'departure' && <><label>离职日期<input type="date" value={date} max={today} required disabled={busy || blocked} onChange={event => setDate(event.target.value)} /></label>{departureError && <p className="error-message" role="alert">{departureError}</p>}</>}
        {action === 'replacement' && <fieldset disabled={busy || blocked}><label>接任负责人<Select label="接任负责人" value={successorId} options={[{ value: '', label: '请选择' }, ...successors.map(p => ({ value: p.id, label: `${p.displayName}（${p.loginName}）` }))]} onChange={setSuccessorId} required /></label><dl className="info-list"><div><dt>负责组织</dt><dd>{data.units.find(unit => unit.id === person.orgUnitId)?.name ?? '未分配'}</dd></div><div><dt>接任角色</dt><dd>{roleLabels[managerRole(person) ?? 'EMPLOYEE']}</dd></div><div><dt>原负责人角色</dt><dd>{person.roles.includes('ADMIN') ? '系统管理员、普通员工' : '普通员工'}</dd></div><div><dt>调整直属下属</dt><dd>{reports.filter(p => p.id !== successorId).map(p => p.displayName).join('、') || '无'}</dd></div></dl></fieldset>}
        {action === 'job' && <fieldset disabled={busy || blocked}><label>所属组织<Select label="调整所属组织" value={org} options={[{ value: '', label: '未分配' }, ...organizationFilterOptions(data.units)]} onChange={setOrg} /></label><div className="lifecycle-roles" role="group" aria-label="岗位角色">{(Object.keys(roleLabels) as Role[]).map(role => <label key={role}><input type="checkbox" checked={roles.includes(role)} onChange={event => setRoles(current => event.target.checked ? [...current, role] : current.filter(value => value !== role))} />{roleLabels[role]}</label>)}</div>{canHaveSupervisor(roles) && <label>直属上级<Select label="调整直属上级" value={supervisor} options={[{ value: '', label: '未设置' }, ...(supervisor && !candidates.some(p => p.id === supervisor) ? [{ value: supervisor, label: '原上级（请重新选择）' }] : []), ...candidates.map(p => ({ value: p.id, label: p.displayName }))]} onChange={setSupervisor} /></label>}</fieldset>}
        {action === 'history' && <>{!history && !error && <p role="status">正在加载…</p>}{history?.length === 0 && <p className="quiet-empty">暂无变动记录</p>}<ol className="personnel-timeline">{orderedHistory.map((change, index) => <li className="personnel-change" key={change.id}>
          <span className="personnel-event-number">{index + 1}</span>
          <h3>{change.kind === 'DEPARTURE' ? '办理离职' : `第 ${++jobChangeNumber} 次岗位调整`}</h3>
          {change.kind === 'JOB_CHANGE' && change.beforeValues.orgUnitId !== change.afterValues.orgUnitId && <p className="personnel-transfer-summary">{showValue('orgUnitId', change.beforeValues.orgUnitId)} <span aria-label="调至">→</span> {showValue('orgUnitId', change.afterValues.orgUnitId)}</p>}
          <p className="personnel-event-meta">操作人：{change.actorName ?? '历史操作者（未记录）'} · {new Date(change.recordedAt).toLocaleString('zh-CN')} · 生效日期：{change.effectiveDate}</p>
          <table className="personnel-change-table"><thead><tr><th scope="col">变动项目</th><th scope="col">{change.kind === 'DEPARTURE' ? '离职前' : '调整前'}</th><th scope="col">{change.kind === 'DEPARTURE' ? '离职后' : '调整后'}</th></tr></thead><tbody>{Object.entries(fieldLabels).filter(([key]) => JSON.stringify(change.beforeValues[key]) !== JSON.stringify(change.afterValues[key])).map(([key, label]) => <tr key={key}><th scope="row">{label}</th><td>{showValue(key, change.beforeValues[key])}</td><td>{showValue(key, change.afterValues[key])}</td></tr>)}</tbody></table>
        </li>)}</ol></>}
        {error && <p className="error-message" role="alert">{error}{blocked ? ' 请关闭并刷新资料后核对。' : ''}</p>}
      </div>
      <div className="dialog-footer"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>关闭</button>{action !== 'history' && <button className={`button ${action === 'departure' ? 'danger' : 'primary'}`} type="submit" disabled={busy || blocked || (action === 'departure' && !!departureError)}>{busy ? '正在保存…' : action === 'departure' ? '确认离职并停用账号' : action === 'replacement' ? '确认更换负责人' : '保存岗位调整'}</button>}</div>
    </ValidatedForm>
  </dialog>
}
