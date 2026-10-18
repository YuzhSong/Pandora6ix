import { useEffect, useRef } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import { RoleBadge } from './RoleBadge.tsx'
import { Icon } from './Icon.tsx'
import { genderLabels } from '../data/personnelProfile.ts'
import { AccountInformation } from './AccountInformation.tsx'

export function PersonDetails({ person, data, onClose, onEdit, onResetPassword, initialPassword, editLabel = '编辑资料' }: {
  person: Person
  data: AdminData
  onClose: () => void
  editLabel?: string
  onEdit: () => void
  onResetPassword?: () => void
  initialPassword?: string
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    dialog.current?.showModal()
    return () => opener?.focus()
  }, [])
  const organization = data.units.find(unit => unit.id === person.orgUnitId)
  const parent = data.units.find(unit => unit.id === organization?.parentId)
  const reports = data.people.filter(item => item.supervisorId === person.id)
  return (
    <dialog ref={dialog} className="editor-dialog" aria-labelledby="person-details-title" onCancel={event => { event.preventDefault(); onClose() }}>
      <div className="dialog-head">
        <h2 id="person-details-title">人员资料</h2>
        <button className="icon-button" aria-label="关闭人员详情" onClick={onClose}><Icon name="close" /></button>
      </div>
      <div className="dialog-body">
        <div className="account-identity"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><h3>{person.displayName}</h3><span>{person.jobTitle || '未设置职位'}</span></div></div>
        <AccountInformation account={{ loginName: person.loginName, enterpriseDomain: data.enterprise.loginDomain }} initialPassword={initialPassword} />
        <dl className="info-list profile-details">
          <div><dt>性别</dt><dd>{person.gender ? genderLabels[person.gender] : '未填写'}</dd></div>
          <div><dt>身份证号</dt><dd>{person.idNumber || '未填写'}</dd></div>
          <div><dt>电话号码</dt><dd>{person.phoneNumber || '未填写'}</dd></div>
          <div><dt>所属组织</dt><dd>{parent ? `${parent.name} / ` : ''}{organization?.name}</dd></div>
          <div><dt>直属上级</dt><dd>{data.people.find(item => item.id === person.supervisorId)?.displayName || '未设置'}</dd></div>
          <div><dt>业务角色</dt><dd className="badges">{person.roles.map(role => <RoleBadge key={role} role={role} />)}</dd></div>
          <div><dt>直接下属</dt><dd>{reports.length ? reports.map(item => item.displayName).join('、') : '无'}</dd></div>
        </dl>
      </div>
      <div className="dialog-footer"><button className="button secondary" onClick={onClose}>关闭</button>{onResetPassword && <button className="button secondary" onClick={onResetPassword}>重置密码</button>}<button className="button primary" onClick={onEdit}>{editLabel}</button></div>
    </dialog>
  )
}
