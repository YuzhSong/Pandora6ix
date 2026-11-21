import { useEffect, useRef, useState } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import { adminRepository } from '../data/repository.ts'
import { canRestoreAccount } from '../data/personnelDisplay.ts'
import { errorMessage } from '../api/feedback.ts'
import { organizationFilterOptions } from '../data/organizationHierarchy.ts'
import { managerRole } from '../data/personnelResponsibility.ts'
import { organizationRole } from '../data/personnelCreation.ts'
import { Select } from './Select.tsx'

export function RestoreAccountDialog({ data, person, onClose, onSaved }: { data: AdminData; person: Person; onClose: () => void; onSaved: (data: AdminData) => void }) {
  const dialog = useRef<HTMLDialogElement>(null), pending = useRef(false)
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  const departed = person.employmentStatus === 'DEPARTED'
  const [team, setTeam] = useState('')
  const manager = managerRole(person)
  const occupied = manager && data.people.some(other => other.id !== person.id && other.employmentStatus !== 'DEPARTED' &&
    other.roles.includes(manager) && (manager === 'FOUNDER' || other.orgUnitId === person.orgUnitId))
  const needsTeam = departed && (organizationRole(person.roles) === 'EMPLOYEE' || occupied) &&
    !data.units.some(unit => unit.id === person.orgUnitId && unit.kind === 'TEAM')
  const teams = organizationFilterOptions(data.units).filter(option => data.units.some(unit => unit.id === option.value && unit.kind === 'TEAM'))
  const action = departed ? '撤销离职' : '恢复账号'
  const allowed = departed ? !needsTeam || !!team : canRestoreAccount(person)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null, element = dialog.current; element?.showModal(); return () => { element?.close(); opener?.focus() } }, [])
  async function restore() {
    if (pending.current || error || !allowed) return
    pending.current = true; setBusy(true)
    try { onSaved(await (departed ? adminRepository.reverseDeparture(person.id, person.version, needsTeam ? team : undefined) : adminRepository.restoreAccount(person.id, person.version))) }
    catch (reason) { setError(`${errorMessage(reason)} 请关闭并刷新资料后核对。`) }
    finally { pending.current = false; setBusy(false) }
  }
  return <dialog ref={dialog} className={departed ? 'confirm-dialog' : 'editor-dialog'} aria-labelledby="restore-account-title" onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    {!departed && <div className="dialog-head"><h2 id="restore-account-title">{action} · {person.displayName}</h2></div>}
    <div className="dialog-body">
      {departed ? <>
        <h2 id="restore-account-title">撤销离职 · {person.displayName}</h2>
        <dl className="info-list departure-reversal-details">
          <div><dt>登录账号</dt><dd>{person.loginName}</dd></div>
          <div><dt>离职日期</dt><dd>{person.departureDate}</dd></div>
          <div><dt>撤销后状态</dt><dd>在职</dd></div>
        </dl>
        {needsTeam && <label>所属团队<Select label="所属团队" value={team} options={teams} onChange={setTeam} disabled={busy} required /></label>}
      </> : <p>确认恢复此在职人员的登录权限？恢复后沿用原密码和原有角色权限。</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
    </div>
    <div className="dialog-footer"><button className="button secondary" disabled={busy} onClick={onClose}>取消</button><button className="button primary" disabled={busy || !!error || !allowed} onClick={() => void restore()}>{busy ? departed ? '正在撤销…' : '正在恢复…' : '确认' + action}</button></div>
  </dialog>
}
