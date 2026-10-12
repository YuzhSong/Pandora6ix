import { useState } from 'react'
import type { AdminData, OrgUnit, Role } from '../data/types.ts'
import { roleLabels, unitLabels } from '../data/types.ts'
import type { EditRequest } from './AdminEditor.tsx'
import { PersonDetails } from './PersonDetails.tsx'
import type { Person } from '../data/types.ts'
import { belongsToOrganization, groupPersonnel } from '../data/personnelGroups.ts'
import { Icon } from './Icon.tsx'

interface PageProps { data: AdminData; onEdit: (request: EditRequest) => void }

function Search({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return <label className="search-box"><Icon name="search" size={18} /><input aria-label={placeholder} placeholder={placeholder} value={value} onChange={event => onChange(event.target.value)} />{value && <button className="icon-button" aria-label="清空搜索" onClick={() => onChange('')}><Icon name="close" size={15} /></button>}</label>
}

export function PeoplePage({ data, onEdit }: PageProps) {
  const [query, setQuery] = useState('')
  const [unitFilter, setUnitFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [mode, setMode] = useState<'organization' | 'role'>('organization')
  const [expanded, setExpanded] = useState<string[]>(['role-ADMIN'])
  const [collapsedMatches, setCollapsedMatches] = useState<string[]>([])
  const [limits, setLimits] = useState<Record<string, number>>({})
  const [detail, setDetail] = useState<Person | null>(null)
  const matches = data.people.filter(person => {
    const term = query.trim().toLowerCase()
    return (!term || (person.displayName + ' ' + person.loginName + ' ' + person.jobTitle).toLowerCase().includes(term)) && belongsToOrganization(data, person, unitFilter) && (!roleFilter || person.roles.includes(roleFilter as Role))
  })
  const groups = groupPersonnel(data, matches, mode)
  const filtered = !!(query || unitFilter || roleFilter)
  return <section className="panel" aria-label="人员与角色">
    <div className="panel-heading"><h2>企业人员<span className="count-badge">{data.people.length}</span></h2><button className="button primary" onClick={() => onEdit({ kind: 'person' })}><Icon name="plus" size={18} />新增人员</button></div>
    <div className="filters"><Search value={query} onChange={setQuery} placeholder="搜索姓名、登录名或职位" /><select aria-label="按组织筛选" value={unitFilter} onChange={event => setUnitFilter(event.target.value)}><option value="">全部组织</option>{data.units.map(unit => <option key={unit.id} value={unit.id}>{unit.name}</option>)}</select><select aria-label="按角色筛选" value={roleFilter} onChange={event => setRoleFilter(event.target.value)}><option value="">全部角色</option>{(Object.keys(roleLabels) as Role[]).map(role => <option key={role} value={role}>{roleLabels[role]}</option>)}</select><select aria-label="人员分组" value={mode} onChange={event => { setMode(event.target.value as typeof mode); setLimits({}) }}><option value="organization">按部门分组</option><option value="role">按角色分组</option></select>{filtered && <button className="text-button" onClick={() => { setQuery(''); setUnitFilter(''); setRoleFilter('') }}>重置筛选</button>}</div>
    <div className="personnel-groups">{groups.map(group => {
      const open = filtered ? !collapsedMatches.includes(group.id) : expanded.includes(group.id)
      const limit = limits[group.id] ?? 12
      return <section className="personnel-group" key={group.id}><button className="group-heading" aria-expanded={open} aria-controls={'members-' + group.id} onClick={() => { if (filtered) setCollapsedMatches(current => current.includes(group.id) ? current.filter(id => id !== group.id) : [...current, group.id]); else setExpanded(current => current.includes(group.id) ? current.filter(id => id !== group.id) : [...current, group.id]) }}><Icon name={group.id === 'role-ADMIN' ? 'shield' : 'organization'} size={19} /><strong>{group.name}</strong><span className="count-badge">{group.people.length}</span><Icon name="chevron" size={16} /></button>{open && <div id={'members-' + group.id}><div className="personnel-cards">{group.people.slice(0, limit).map(person => <article className="personnel-card" key={person.id}><div className="personnel-identity"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><button className="person-name" aria-label={'查看人员：' + person.displayName} onClick={() => setDetail(person)}>{person.displayName}</button><small>{person.loginName}</small></div></div><p>{person.jobTitle || '未设置职位'}</p><span className="personnel-unit">{data.units.find(unit => unit.id === person.orgUnitId)?.name}</span><div className="badges">{person.roles.map(role => <span key={role} className="badge neutral">{roleLabels[role]}</span>)}</div><div className="personnel-actions"><button className="text-button" aria-label={'编辑人员：' + person.displayName} onClick={() => onEdit({ kind: 'person', item: person })}>编辑资料</button><button className="text-button" aria-label={'配置角色：' + person.displayName} onClick={() => onEdit({ kind: 'roles', item: person })}>配置角色</button></div></article>)}</div>{group.people.length > limit && <button className="button secondary group-more" onClick={() => setLimits(current => ({ ...current, [group.id]: limit + 12 }))}>显示更多（剩余 {group.people.length - limit} 位）</button>}</div>}</section>
    })}</div>
    {!matches.length && <div className="empty-state"><h3>没有符合条件的人员</h3><button className="button secondary" onClick={() => { setQuery(''); setUnitFilter(''); setRoleFilter('') }}>重置筛选</button></div>}
    {detail && <PersonDetails person={data.people.find(person => person.id === detail.id) ?? detail} data={data} onClose={() => setDetail(null)} onEdit={() => { setDetail(null); onEdit({ kind: 'person', item: data.people.find(person => person.id === detail.id) ?? detail }) }} />}
  </section>
}

function TreeBranch({ unit, data, selectedId, onSelect }: { unit: OrgUnit; data: AdminData; selectedId: string; onSelect: (id: string) => void }) {
  const children = data.units.filter(item => item.parentId === unit.id)
  return <li><button className={`tree-node ${unit.id === selectedId ? 'selected' : ''}`} onClick={() => onSelect(unit.id)} aria-pressed={unit.id === selectedId}><Icon name={unit.kind === 'COMPANY' ? 'enterprise' : 'organization'} size={18} /><span>{unit.name}</span><small>{data.people.filter(person => person.orgUnitId === unit.id).length}</small></button>{children.length > 0 && <ul>{children.map(child => <TreeBranch key={child.id} unit={child} data={data} selectedId={selectedId} onSelect={onSelect} />)}</ul>}</li>
}

export function OrganizationPage({ data, onEdit }: PageProps) {
  const [selectedId, setSelectedId] = useState('org-company')
  const selected = data.units.find(unit => unit.id === selectedId) ?? data.units[0]
  const parent = data.units.find(unit => unit.id === selected.parentId)
  const members = data.people.filter(person => person.orgUnitId === selected.id)
  const children = data.units.filter(unit => unit.parentId === selected.id)
  return <><section className="panel company-information"><div><span className="eyebrow">所属公司</span><h2>{data.enterprise.name}</h2></div><button className="button secondary" onClick={() => onEdit({ kind: 'enterprise', item: data.enterprise })}>编辑公司信息</button></section><div className="organization-grid">
    <section className="panel tree-panel"><div className="panel-heading"><div><h2>组织架构</h2><p>{data.units.length} 个组织</p></div><button className="icon-button bordered" aria-label="新增组织" onClick={() => onEdit({ kind: 'unit', parentId: selected.kind === 'TEAM' ? selected.parentId ?? undefined : selected.id })}><Icon name="plus" size={19} /></button></div><ul className="organization-tree">{data.units.filter(unit => !unit.parentId).map(unit => <TreeBranch key={unit.id} unit={unit} data={data} selectedId={selected.id} onSelect={setSelectedId} />)}</ul></section>
    <section className="panel organization-detail"><div className="panel-heading"><div className="organization-title"><span className="large-icon"><Icon name="organization" size={24} /></span><div><span className="eyebrow">{unitLabels[selected.kind]}</span><h2>{selected.name}</h2></div></div><div className="organization-actions">{selected.kind !== 'TEAM' && <button className="button secondary" onClick={() => onEdit({ kind: 'unit', parentId: selected.id })}><Icon name="plus" size={16} />{selected.kind === 'COMPANY' ? '新增部门' : '新增团队'}</button>}{selected.kind !== 'COMPANY' && <button className="button secondary" onClick={() => onEdit({ kind: 'unit', item: selected })}>编辑组织</button>}</div></div><dl className="detail-grid"><div><dt>上级组织</dt><dd>{parent?.name ?? '无上级（公司根组织）'}</dd></div><div><dt>直接所属人员</dt><dd>{members.length} 位</dd></div><div><dt>直属下级组织</dt><dd>{children.length} 个</dd></div></dl><h3 className="section-label">直属下级组织</h3>{children.length ? <div className="child-units">{children.map(unit => <button key={unit.id} onClick={() => setSelectedId(unit.id)}><Icon name="organization" /><div><strong>{unit.name}</strong><span>{unitLabels[unit.kind]}</span></div><Icon name="chevron" size={17} /></button>)}</div> : <p className="quiet-empty">暂无直属下级组织</p>}<div className="members-heading"><h3 className="section-label">组织成员</h3><button className="text-button" onClick={() => onEdit({ kind: 'person', orgUnitId: selected.id })}><Icon name="plus" size={16} />新增人员</button></div>{members.length ? <div className="member-list">{members.map(person => <div key={person.id} className="person-summary"><span className="avatar">{person.displayName.slice(0, 1)}</span><div><strong>{person.displayName}</strong><span>{person.jobTitle || '未设置职位'}</span></div><button className="text-button" onClick={() => onEdit({ kind: 'person', item: person })}>编辑</button></div>)}</div> : <p className="quiet-empty">暂无成员</p>}</section>
  </div></>
}

export function TagsPage({ data, onEdit }: PageProps) {
  const [query, setQuery] = useState('')
  const tags = data.tags.filter(tag => tag.name.toLowerCase().includes(query.trim().toLowerCase()))
  return <section className="panel"><div className="panel-heading"><div><h2>企业共用标签<span className="count-badge">{data.tags.length}</span></h2></div><button className="button primary" onClick={() => onEdit({ kind: 'tag' })}><Icon name="plus" size={18} />新增标签</button></div><div className="filters"><Search value={query} onChange={setQuery} placeholder="搜索标签名称" /><span className="result-count">{query ? `找到 ${tags.length} 个标签` : `共 ${tags.length} 个标签`}</span></div><div className="tag-grid">{tags.map(tag => <article className="tag-card" key={tag.id}><div className="tag-card-top"><span className="tag-color" style={{ background: tag.colorHex }}><Icon name="tags" size={22} /></span><span className="badge neutral">企业共用</span></div><h3>{tag.name}</h3><p><i style={{ background: tag.colorHex }} />{tag.colorHex}</p><button className="text-button" aria-label={`编辑标签：${tag.name}`} onClick={() => onEdit({ kind: 'tag', item: tag })}>编辑标签<Icon name="arrow" size={16} /></button></article>)}</div>{!tags.length && <div className="empty-state"><Icon name="tags" size={30} /><h3>{query ? '没有找到该标签' : '暂无企业标签'}</h3><p>{query ? '试试其他关键词。' : '添加标签后，可在任务和日报中选用。'}</p>{query ? <button className="button secondary" onClick={() => setQuery('')}>清空搜索</button> : <button className="button primary" onClick={() => onEdit({ kind: 'tag' })}>新增标签</button>}</div>}</section>
}
