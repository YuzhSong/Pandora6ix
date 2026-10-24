import { departureBlocker, managerRole } from '../data/personnelResponsibility.ts'
import { PersonnelLifecycleDialog } from './PersonnelLifecycleDialog.tsx'
import type { PersonnelAction } from './PersonnelLifecycleDialog.tsx'
import { useState } from 'react'
import type { AdminData, OrgUnit, Role } from '../data/types.ts'
import { roleLabels, unitLabels } from '../data/types.ts'
import type { EditRequest } from './AdminEditor.tsx'
import { PersonDetails } from './PersonDetails.tsx'
import type { Person } from '../data/types.ts'
import { belongsToOrganization, groupPersonnel } from '../data/personnelGroups.ts'
import { organizationDeletionBlocker } from '../data/organizationEditing.ts'
import { currentOrganizationMembers, organizationFilterOptions } from '../data/organizationHierarchy.ts'
import type { PersonnelGroup } from '../data/personnelGroups.ts'
import { Icon } from './Icon.tsx'
import { Select } from './Select.tsx'
import { RoleBadge } from './RoleBadge.tsx'
import { PersonStatusBadge } from './PersonStatusBadge.tsx'

interface PageProps { data: AdminData; onEdit: (request: EditRequest) => void }

function Search({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return <label className="search-box"><Icon name="search" size={18} /><input aria-label={placeholder} placeholder={placeholder} value={value} onChange={event => onChange(event.target.value)} />{value && <button className="icon-button" aria-label="清空搜索" onClick={() => onChange('')}><Icon name="close" size={15} /></button>}</label>
}

export function PeoplePage({ data, onEdit, onCredentialTool, onResetPassword, onUpdated }: PageProps & { onUpdated: (data: AdminData) => void; onCredentialTool: (tool: 'bulk' | 'export') => void; onResetPassword: (person: Person) => void }) {
  const [lifecycle, setLifecycle] = useState<{ person: Person; action: PersonnelAction } | null>(null)
  const [employment, setEmployment] = useState('IN_SERVICE')
  const [query, setQuery] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [mode, setMode] = useState<'organization' | 'role'>('organization')
  const [expanded, setExpanded] = useState<string[]>(() => ['role-ADMIN', ...data.units.filter(unit => unit.kind !== 'TEAM').map(unit => 'unit-' + unit.id)])
  const [collapsedMatches, setCollapsedMatches] = useState<string[]>([])
  const [limits, setLimits] = useState<Record<string, number>>({})
  const [detail, setDetail] = useState<Person | null>(null)
  const matches = data.people.filter(person => {
    const term = query.trim().toLowerCase()
    return (employment === '' || (person.employmentStatus ?? 'IN_SERVICE') === employment) && (!term || (person.displayName + ' ' + person.loginName + ' ' + person.roles.map(role => roleLabels[role]).join(' ')).toLowerCase().includes(term)) && belongsToOrganization(data, person, unitFilter) && (!roleFilter || person.roles.includes(roleFilter as Role))
  })
  const groups = groupPersonnel(data, matches, mode)
  const filtered = !!(query || unitFilter || roleFilter)
  function renderGroup(group: PersonnelGroup, depth = 0) {
      const open = filtered ? !collapsedMatches.includes(group.id) : expanded.includes(group.id)
      const limit = limits[group.id] ?? 12
      return <section className={`personnel-group${depth ? ' personnel-team-group' : ''}`} key={group.id}><button className="group-heading" aria-expanded={open} aria-controls={'members-' + group.id} onClick={() => { if (filtered) setCollapsedMatches(current => current.includes(group.id) ? current.filter(id => id !== group.id) : [...current, group.id]); else setExpanded(current => current.includes(group.id) ? current.filter(id => id !== group.id) : [...current, group.id]) }}><Icon name={group.id === 'role-ADMIN' ? 'shield' : group.unitKind === 'COMPANY' ? 'enterprise' : 'organization'} size={19} /><strong>{group.name}</strong><span className="count-badge">{group.total}</span><Icon name="chevron" size={16} /></button>{open && <div id={'members-' + group.id}>{group.children.length > 0 && group.people.length > 0 && <h3 className="section-label">{group.unitKind === 'COMPANY' ? '公司直属人员' : '部门直属人员'}</h3>}{group.people.length > 0 && <div className="personnel-cards">{group.people.slice(0, limit).map(person => <article className="personnel-card" key={person.id}><div className="personnel-identity"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><div className="person-name-line"><button className="person-name" aria-label={'查看人员：' + person.displayName} onClick={() => setDetail(person)}>{person.displayName}</button><PersonStatusBadge person={person} /></div><small className="person-login-name">登录账号：{person.loginName}</small></div></div><span className="personnel-unit">{data.units.find(unit => unit.id === person.orgUnitId)?.name}</span><div className="badges">{person.roles.map(role => <RoleBadge key={role} role={role} />)}</div><div className="personnel-actions"><button className="text-button" aria-label={'查看账号：' + person.displayName} onClick={() => setDetail(person)}>账号与初始密码</button><button className="text-button" aria-label={'编辑人员：' + person.displayName} disabled={person.employmentStatus === 'DEPARTED'} onClick={() => onEdit({ kind: 'person', item: person })}>编辑资料</button>{!person.roles.includes('ADMIN') && <button className="text-button" aria-label={'岗位调整：' + person.displayName} disabled={person.employmentStatus === 'DEPARTED' || person.id === data.currentUserId} onClick={() => setLifecycle({ person, action: 'job' })}>岗位调整</button>}{person.employmentStatus !== 'DEPARTED' && !person.roles.includes('ADMIN') && <button className="text-button" aria-label={'重置密码：' + person.displayName} onClick={() => onResetPassword(person)}>重置密码</button>}{person.employmentStatus !== 'DEPARTED' && person.id !== data.currentUserId && <button className="text-button" disabled={!!departureBlocker(data, person)} title={departureBlocker(data, person) ?? undefined} onClick={() => setLifecycle({ person, action: 'departure' })}>办理离职</button>}{person.employmentStatus !== 'DEPARTED' && person.status !== 'DISABLED' && person.id !== data.currentUserId && managerRole(person) && <button className="text-button" aria-label={'更换负责人：' + person.displayName} onClick={() => setLifecycle({ person, action: 'replacement' })}>更换负责人</button>}<button className="text-button" onClick={() => setLifecycle({ person, action: 'history' })}>变动记录</button></div></article>)}</div>}{group.people.length > limit && <button className="button secondary group-more" onClick={() => setLimits(current => ({ ...current, [group.id]: limit + 12 }))}>显示更多（剩余 {group.people.length - limit} 位）</button>}{group.children.length > 0 && <div className="personnel-team-groups">{group.children.map(child => renderGroup(child, depth + 1))}</div>}</div>}</section>
  }
  return <section className="panel" aria-label="人员与角色">
    <div className="panel-heading"><h2>企业人员<span className="count-badge">{data.people.length}</span></h2><div className="personnel-toolbar"><button className="button secondary" onClick={() => onCredentialTool('export')}>批量交付</button><button className="button secondary" onClick={() => onCredentialTool('bulk')}>批量创建</button><button className="button primary" onClick={() => onEdit({ kind: 'person' })}><Icon name="plus" size={18} />新增人员</button></div></div>
    <div className="filters"><Search value={query} onChange={setQuery} placeholder="搜索姓名、登录名或角色" /><Select className="organization-filter" label="按组织筛选" value={unitFilter} onChange={setUnitFilter} options={[{ value: '', label: '全部组织' }, ...organizationFilterOptions(data.units)]} /><Select label="按角色筛选" value={roleFilter} onChange={setRoleFilter} options={[{ value: '', label: '全部角色' }, ...(Object.keys(roleLabels) as Role[]).map(role => ({ value: role, label: roleLabels[role] }))]} /><Select label="在职状态" value={employment} onChange={setEmployment} options={[{ value: 'IN_SERVICE', label: '在职人员' }, { value: 'DEPARTED', label: '已离职人员' }, { value: '', label: '全部人员' }]} /><Select label="人员分组" value={mode} onChange={value => { setMode(value as typeof mode); setLimits({}) }} options={[{ value: 'organization', label: '按组织层级分组' }, { value: 'role', label: '按角色分组' }]} />{filtered && <button className="text-button" onClick={() => { setQuery(''); setUnitFilter(''); setRoleFilter('') }}>重置筛选</button>}</div>
    <div className="personnel-groups">{groups.map(group => renderGroup(group))}</div>
    {!matches.length && <div className="empty-state"><h3>没有符合条件的人员</h3><button className="button secondary" onClick={() => { setQuery(''); setUnitFilter(''); setRoleFilter('') }}>重置筛选</button></div>}
    {lifecycle && <PersonnelLifecycleDialog key={lifecycle.person.id + lifecycle.action} person={data.people.find(p => p.id === lifecycle.person.id) ?? lifecycle.person} action={lifecycle.action} data={data} onClose={() => setLifecycle(null)} onSaved={next => { onUpdated(next); setLifecycle(null) }} />}
    {detail && <PersonDetails onResetPassword={detail.employmentStatus !== 'DEPARTED' && !detail.roles.includes('ADMIN') ? () => { setDetail(null); onResetPassword(data.people.find(person => person.id === detail.id) ?? detail) } : undefined} person={data.people.find(person => person.id === detail.id) ?? detail} data={data} onClose={() => setDetail(null)} onEdit={() => { setDetail(null); onEdit({ kind: 'person', item: data.people.find(person => person.id === detail.id) ?? detail }) }} />}
  </section>
}

function TreeBranch({ unit, data, selectedId, onSelect }: { unit: OrgUnit; data: AdminData; selectedId: string; onSelect: (id: string) => void }) {
  const children = data.units.filter(item => item.parentId === unit.id)
  return <li><button className={`tree-node ${unit.id === selectedId ? 'selected' : ''}`} onClick={() => onSelect(unit.id)} aria-pressed={unit.id === selectedId}><Icon name={unit.kind === 'COMPANY' ? 'enterprise' : 'organization'} size={18} /><span>{unit.name}</span><small>{currentOrganizationMembers(data.people, unit.id).length}</small></button>{children.length > 0 && <ul>{children.map(child => <TreeBranch key={child.id} unit={child} data={data} selectedId={selectedId} onSelect={onSelect} />)}</ul>}</li>
}

export function OrganizationPage({ data, onEdit, selectedId, onSelect: setSelectedId, onDelete }: PageProps & { selectedId: string; onSelect: (id: string) => void; onDelete: (unit: OrgUnit) => void }) {
  const selected = data.units.find(unit => unit.id === selectedId) ?? data.units.find(unit => unit.kind === 'COMPANY') ?? data.units[0]
  const blocker = organizationDeletionBlocker(data, selected.id)
  const parent = data.units.find(unit => unit.id === selected.parentId)
  const members = currentOrganizationMembers(data.people, selected.id)
  const children = data.units.filter(unit => unit.parentId === selected.id)
  return <><section className="panel company-information"><div><span className="eyebrow">所属公司</span><h2>{data.enterprise.name}</h2></div><button className="button secondary" onClick={() => onEdit({ kind: 'enterprise', item: data.enterprise })}>编辑公司信息</button></section><div className="organization-grid">
    <section className="panel tree-panel"><div className="panel-heading"><div><h2>组织架构</h2><p>{data.units.length} 个组织</p></div><button className="icon-button bordered" aria-label="新增组织" onClick={() => onEdit({ kind: 'unit', parentId: selected.kind === 'TEAM' ? selected.parentId ?? undefined : selected.id })}><Icon name="plus" size={19} /></button></div><ul className="organization-tree">{data.units.filter(unit => !unit.parentId).map(unit => <TreeBranch key={unit.id} unit={unit} data={data} selectedId={selected.id} onSelect={setSelectedId} />)}</ul></section>
    <section className="panel organization-detail"><div className="panel-heading"><div className="organization-title"><span className="large-icon"><Icon name="organization" size={24} /></span><div><span className="eyebrow">{unitLabels[selected.kind]}</span><h2>{selected.name}</h2></div></div><div className="organization-actions">{selected.kind !== 'TEAM' && <button className="button secondary" onClick={() => onEdit({ kind: 'unit', parentId: selected.id })}><Icon name="plus" size={16} />{selected.kind === 'COMPANY' ? '新增部门' : '新增团队'}</button>}{selected.kind !== 'COMPANY' && <button className="button secondary" onClick={() => onEdit({ kind: 'unit', item: selected })}>编辑组织</button>}{selected.kind !== 'COMPANY' && <button className="button danger" disabled={!!blocker} title={blocker ?? undefined} onClick={() => onDelete(selected)}>删除组织</button>}</div></div>{selected.kind !== 'COMPANY' && blocker && <p className="organization-delete-note">{blocker}</p>}<dl className="detail-grid"><div><dt>上级组织</dt><dd>{parent?.name ?? '无上级（公司根组织）'}</dd></div><div><dt>直接所属人员</dt><dd>{members.length} 位</dd></div><div><dt>直属下级组织</dt><dd>{children.length} 个</dd></div></dl><h3 className="section-label">直属下级组织</h3>{children.length ? <div className="child-units">{children.map(unit => <button key={unit.id} onClick={() => setSelectedId(unit.id)}><Icon name="organization" /><div><strong>{unit.name}</strong><span>{unitLabels[unit.kind]}</span></div><Icon name="chevron" size={17} /></button>)}</div> : <p className="quiet-empty">暂无直属下级组织</p>}<div className="members-heading"><h3 className="section-label">组织成员</h3><button className="text-button" onClick={() => onEdit({ kind: 'person', orgUnitId: selected.id })}><Icon name="plus" size={16} />新增人员</button></div>{members.length ? <div className="member-list">{members.map(person => <div key={person.id} className="person-summary"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><div className="person-name-line"><strong>{person.displayName}</strong><PersonStatusBadge person={person} /></div><span>{person.roles.map(role => roleLabels[role]).join("、")}</span></div><button className="text-button" onClick={() => onEdit({ kind: 'person', item: person })}>编辑</button></div>)}</div> : <p className="quiet-empty">暂无成员</p>}</section>
  </div></>
}
