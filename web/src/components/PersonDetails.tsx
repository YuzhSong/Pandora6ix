import { useEffect, useRef, useState } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import { canHaveSupervisor } from '../data/supervisors.ts'
import { RoleBadge } from './RoleBadge.tsx'
import { PersonStatusBadge } from './PersonStatusBadge.tsx'
import { Icon } from './Icon.tsx'
import { genderLabels, maskIdentity } from '../data/personnelProfile.ts'
import { AccountInformation } from './AccountInformation.tsx'
import { disableReason, canRestoreAccount, organizationPath, supervisorOptionLabel } from '../data/personnelDisplay.ts'

export function PersonDetails({ person, data, onClose, onEdit, onResetPassword, editLabel = '编辑资料' }: {
  person: Person; data: AdminData; onClose: () => void; editLabel?: string; onEdit: () => void; onResetPassword?: () => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [identityVisible, setIdentityVisible] = useState(false)
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null, element = dialog.current
    element?.showModal()
    return () => { element?.close(); opener?.focus() }
  }, [])
  const supervisor = data.people.find(item => item.id === person.supervisorId)
  const reports = data.people.filter(item => item.supervisorId === person.id && item.employmentStatus !== 'DEPARTED')
  return <dialog ref={dialog} className="editor-dialog person-details-dialog" aria-labelledby="person-details-title" onCancel={event => { event.preventDefault(); onClose() }}>
    <div className="dialog-head"><h2 id="person-details-title">人员资料</h2><button className="icon-button" aria-label="关闭人员详情" onClick={onClose}><Icon name="close" /></button></div>
    <div className="dialog-body">
      <div className="person-profile-identity"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><div className="person-name-line"><h3>{person.displayName}</h3><PersonStatusBadge person={person} />{person.employmentStatus !== 'DEPARTED' && <span className="badge neutral">在职</span>}{person.status !== 'DISABLED' && <span className="badge log-success">账号启用</span>}</div><div className="badges">{person.roles.map(role => <RoleBadge key={role} role={role} />)}</div></div></div>
      {person.status === 'DISABLED' && <div className="person-disabled-summary"><strong>{person.employmentStatus === 'DEPARTED' ? '离职停用' : '在职账号停用'}</strong><span>停用原因：{disableReason(person)}</span><span>{canRestoreAccount(person) ? '可由系统管理员在人员管理中恢复账号。' : person.employmentStatus === 'DEPARTED' ? '已离职账号不支持直接恢复登录。' : '请联系系统管理员核实在职状态。'}</span></div>}
      <section className="person-profile-section" aria-label="组织信息"><h3>组织信息</h3><dl className="person-profile-grid">
        <div className="profile-full-row"><dt>所属组织</dt><dd>{organizationPath(data, person.orgUnitId)}</dd></div>
        {canHaveSupervisor(person.roles) && <div className="profile-full-row"><dt>直属上级</dt><dd>{supervisor ? supervisorOptionLabel(data, supervisor) : '未设置'}</dd></div>}
        {person.departureDate && <div><dt>离职日期</dt><dd>{person.departureDate}</dd></div>}
        <div className="profile-full-row"><dt>直接下属</dt><dd>{reports.length ? <details className="person-reports"><summary>{reports.length} 人 · 展开查看</summary><ul>{reports.map(report => <li key={report.id}>{supervisorOptionLabel(data, report)}</li>)}</ul></details> : '无'}</dd></div>
      </dl></section>
      <section className="person-profile-section" aria-label="基本资料"><h3>基本资料</h3><dl className="person-profile-grid">
        <div><dt>性别</dt><dd>{person.gender ? genderLabels[person.gender] : '未填写'}</dd></div>
        <div><dt>电话号码</dt><dd>{person.phoneNumber || '未填写'}</dd></div>
        <div className="profile-full-row"><dt>身份证号</dt><dd><span>{identityVisible ? person.idNumber : maskIdentity(person.idNumber)}</span>{person.idNumber && <button className="text-button" type="button" aria-label={identityVisible ? '隐藏身份证号' : '查看完整身份证号'} aria-pressed={identityVisible} onClick={() => setIdentityVisible(value => !value)}>{identityVisible ? '隐藏' : '查看'}</button>}</dd></div>
      </dl></section>
      <AccountInformation compact account={{ personId: person.id, loginName: person.loginName, enterpriseDomain: data.enterprise.loginDomain }} allowHandoff={person.employmentStatus !== 'DEPARTED' && !person.roles.includes('ADMIN')} onResetPassword={onResetPassword} />
    </div>
    <div className="dialog-footer"><button className="button secondary" onClick={onClose}>关闭</button>{person.employmentStatus !== 'DEPARTED' && <button className="button primary" onClick={onEdit}>{editLabel}</button>}</div>
  </dialog>
}
