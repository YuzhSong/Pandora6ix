import { useState } from 'react'
import type { ReactNode } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import { canHaveSupervisor } from '../data/supervisors.ts'
import { organizationPath, disableReason } from '../data/personnelDisplay.ts'
import { PersonStatusBadge } from './PersonStatusBadge.tsx'
import { RoleBadge } from './RoleBadge.tsx'

export function PersonnelTable({ people, data, onDetail, actions }: { people: Person[]; data: AdminData; onDetail: (person: Person) => void; actions: (person: Person) => ReactNode }) {
  const [page, setPage] = useState(1)
  const pages = Math.max(1, Math.ceil(people.length / 20)), current = Math.min(page, pages)
  return <><div className="personnel-table-scroll"><table className="personnel-table"><thead><tr><th>姓名 / 账号</th><th>所属组织</th><th>直属上级</th><th>角色</th><th>在职状态</th><th>账号状态</th><th>操作</th></tr></thead><tbody>{people.slice((current - 1) * 20, current * 20).map(person => <tr key={person.id}>
    <td><div className="person-name-line"><button className="person-name" onClick={() => onDetail(person)}>{person.displayName}</button><PersonStatusBadge person={person} /></div><small>{person.loginName}</small></td>
    <td>{organizationPath(data, person.orgUnitId)}</td><td>{canHaveSupervisor(person.roles) ? data.people.find(value => value.id === person.supervisorId)?.displayName || '未设置' : '—'}</td><td><div className="badges">{person.roles.map(role => <RoleBadge key={role} role={role} />)}</div></td><td>{person.employmentStatus === 'DEPARTED' ? '已离职' : '在职'}</td><td>{person.status === 'DISABLED' ? <>已停用<small>原因：{disableReason(person)}</small></> : '已启用'}</td><td><button className="text-button" onClick={() => onDetail(person)}>查看资料</button><details className="personnel-row-actions"><summary>更多操作</summary><div className="personnel-actions">{actions(person)}</div></details></td>
  </tr>)}</tbody></table></div>{people.length > 20 && <div className="table-footer"><span>共 {people.length} 人</span><div className="pagination"><button aria-label="上一页人员" disabled={current === 1} onClick={() => setPage(current - 1)}>‹</button><span>{current} / {pages}</span><button aria-label="下一页人员" disabled={current === pages} onClick={() => setPage(current + 1)}>›</button></div></div>}</>
}
