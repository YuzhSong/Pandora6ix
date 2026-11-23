import { useDeferredValue, useMemo, useState } from 'react'
import { businessKinds, businessStatuses, crmPath, customerKinds, useCrmQuery } from './api.ts'
import { CrmDialog, Detail, Options, PersonPicker, RecordForm, RecordTable, RequestState } from './shared.tsx'
import type { Labels, Person, Row } from './api.ts'
import './crm.css'

export type Customer = Row & { name: string; kind: string; category: string | null; contact: string | null; notes: string | null; ownerUserId: string; ownerName: string }
type Participant = { userId: string; displayName?: string; responsibility: string }
type Business = Row & {
  title: string; kind: string; status: string; eventDate: string; eventTime: string | null; notes: string | null;
  ownerUserId: string; ownerName: string; customerId: string; customerName: string; participants?: Participant[];
}
const customerColumns = [
  { label: '客户名称', value: (row: Customer) => row.name },
  { label: '类型', value: (row: Customer) => (customerKinds as Labels)[row.kind] },
  { label: '分类', value: (row: Customer) => row.category },
  { label: '负责人', value: (row: Customer) => row.ownerName },
]
const businessColumns = [
  { label: '业务名称', value: (row: Business) => row.title },
  { label: '客户', value: (row: Business) => row.customerName },
  { label: '类型', value: (row: Business) => (businessKinds as Labels)[row.kind] },
  { label: '日期', value: (row: Business) => row.eventDate },
  { label: '负责人', value: (row: Business) => row.ownerName },
  { label: '状态', value: (row: Business) => (businessStatuses as Labels)[row.status] },
]

export default function CrmPage({ refresh }: { refresh: unknown }) {
  const [tab, setTab] = useState<'customers' | 'businesses'>('customers')
  const people = useCrmQuery<Person[]>(crmPath('people'), refresh)
  return <section className="panel crm-panel">
    <nav className="crm-toolbar" aria-label="CRM 管理">
      <button className={tab === 'customers' ? 'button primary' : 'button secondary'} aria-pressed={tab === 'customers'} onClick={() => setTab('customers')}>客户档案</button>
      <button className={tab === 'businesses' ? 'button primary' : 'button secondary'} aria-pressed={tab === 'businesses'} onClick={() => setTab('businesses')}>业务记录</button>
    </nav>
    <RequestState error={people.error} loading={!people.data} />
    {people.data && <Directory key={tab} tab={tab} people={people.data} refresh={refresh} />}
  </section>
}

function Directory({ tab, people, refresh }: { tab: 'customers' | 'businesses'; people: Person[]; refresh: unknown }) {
  const isCustomers = tab === 'customers'
  const [search, setSearch] = useState('')
  const [kind, setKind] = useState('')
  const [status, setStatus] = useState('')
  const [offset, setOffset] = useState(0)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const query = useDeferredValue(search)
  const path = crmPath(tab, { q: query, kind, status, offset })
  const requestRevision = useMemo(() => ({ refresh, revision }), [refresh, revision])
  const list = useCrmQuery<(Customer | Business)[]>(path, requestRevision)
  const detail = useCrmQuery<Customer | Business>(selected ? crmPath(tab + '/' + selected) : null, revision)
  const [historyOffset, setHistoryOffset] = useState(0)
  const history = useCrmQuery<Business[]>(isCustomers && selected && !editing
    ? crmPath('businesses', { customerId: selected, offset: historyOffset }) : null, revision)

  const noun = isCustomers ? '客户' : '业务'
  function saved() {
    setRevision(value => value + 1)
    setEditing(false)
    setSelected(null)
  }
  return <>
    <div className="crm-toolbar">
      <label>搜索{noun}<input type="search" value={search} onChange={event => { setSearch(event.target.value); setOffset(0) }} /></label>
      <label>类型<select value={kind} onChange={event => { setKind(event.target.value); setOffset(0) }}>
        <option value="">全部</option><Options labels={isCustomers ? customerKinds : businessKinds} />
      </select></label>
      {!isCustomers && <label>状态<select value={status} onChange={event => { setStatus(event.target.value); setOffset(0) }}>
        <option value="">全部</option><Options labels={businessStatuses} />
      </select></label>}
      <button className="button primary crm-new" onClick={() => { setSelected(null); setEditing(true) }}>新建{noun}</button>
    </div>
    <RequestState error={list.error} loading={!list.data} />
    {list.data && (isCustomers
      ? <RecordTable columns={customerColumns} rows={list.data as Customer[]} onOpen={id => { setHistoryOffset(0); setSelected(id) }} />
      : <RecordTable columns={businessColumns} rows={list.data as Business[]} onOpen={setSelected} />)}
    <Pagination offset={offset} onChange={setOffset} count={list.data?.length} />
    {selected && !editing && <CrmDialog title={noun + '详情'} onClose={() => setSelected(null)}>
      <RequestState error={detail.error} loading={!detail.data} />
      {detail.data && (isCustomers
        ? <><CustomerDetail record={detail.data as Customer} /><h3>业务历史</h3>
          <RequestState error={history.error} loading={!history.data} />
          {history.data && <RecordTable columns={businessColumns} rows={history.data} />}
          <Pagination offset={historyOffset} onChange={setHistoryOffset} count={history.data?.length} /></>
        : <BusinessDetail record={detail.data as Business} />)}
      <div className="crm-actions"><button className="button secondary" onClick={() => setSelected(null)}>关闭</button>
        {detail.data && <button className="button primary" onClick={() => setEditing(true)}>编辑</button>}</div>
    </CrmDialog>}
    {editing && (isCustomers
      ? <CustomerEditor current={selected ? detail.data as Customer : undefined} people={people} onClose={() => setEditing(false)} onSaved={saved} />
      : <BusinessEditor current={selected ? detail.data as Business : undefined} people={people} onClose={() => setEditing(false)} onSaved={saved} />)}
  </>
}

function Pagination({ offset, onChange, count }: { offset: number; onChange: (offset: number) => void; count?: number }) {
  if (!offset && count !== 100) return null
  return <div className="crm-actions"><button className="button secondary" disabled={!offset} onClick={() => onChange(offset - 100)}>上一页</button>
    <span>第 {offset / 100 + 1} 页</span>
    <button className="button secondary" disabled={count !== 100} onClick={() => onChange(offset + 100)}>下一页</button></div>
}

function CustomerDetail({ record }: { record: Customer }) {
  return <Detail values={[
    ['名称', record.name], ['类型', (customerKinds as Labels)[record.kind]], ['分类', record.category],
    ['负责人', record.ownerName], ['联系方式', record.contact], ['备注', record.notes],
  ]} />
}

function BusinessDetail({ record }: { record: Business }) {
  return <><Detail values={[
    ['名称', record.title], ['类型', (businessKinds as Labels)[record.kind]], ['状态', (businessStatuses as Labels)[record.status]],
    ['日期', [record.eventDate, record.eventTime].filter(Boolean).join(' ')], ['客户', record.customerName],
    ['负责人', record.ownerName], ['备注', record.notes],
  ]} /><h3>参与员工</h3>{record.participants?.length
    ? <ul>{record.participants.map(person => <li key={person.userId}>{person.displayName} · {person.responsibility}</li>)}</ul>
    : <p>暂无参与员工。</p>}</>
}

type EditorProps<T> = { current?: T; people: Person[]; onClose: () => void; onSaved: () => void }
function CustomerEditor({ current, people, onClose, onSaved }: EditorProps<Customer>) {
  return <RecordForm path="customers" current={current} title={current ? '编辑客户' : '新建客户'} onClose={onClose} onSaved={onSaved}>
    <label>客户名称<input name="name" required maxLength={128} defaultValue={current?.name} /></label>
    <label>类型<select name="kind" defaultValue={current?.kind || 'PERSON'}><Options labels={customerKinds} /></select></label>
    <label>分类<input name="category" maxLength={64} defaultValue={current?.category || ''} /></label>
    <label>负责人<PersonPicker people={people} value={current && { id: current.ownerUserId, displayName: current.ownerName }} /></label>
    <label>联系方式<input name="contact" maxLength={255} defaultValue={current?.contact || ''} /></label>
    <label className="crm-wide">备注<textarea name="notes" maxLength={2000} defaultValue={current?.notes || ''} /></label>
  </RecordForm>
}

function CustomerPicker({ current }: { current?: Pick<Business, 'customerId' | 'customerName'> }) {
  const [search, setSearch] = useState('')
  const [selection, setSelection] = useState(current)
  const options = useCrmQuery<Customer[]>(crmPath('customers', { q: useDeferredValue(search) }))
  return <div className="crm-picker">
    <label>搜索客户<input type="search" value={search} onChange={event => setSearch(event.target.value)} /></label>
    <label>关联客户<select name="customerId" required value={selection?.customerId || ''}
      onChange={event => {
        const customerId = event.target.value
        setSelection(customerId ? { customerId, customerName: event.target.selectedOptions[0].text } : undefined)
      }}>
      <option value="">请选择客户</option>
      {selection && !options.data?.some(row => row.id === selection.customerId) && <option value={selection.customerId}>{selection.customerName}</option>}
      {options.data?.map(row => <option key={row.id} value={row.id}>{row.name}</option>)}
    </select></label>
    <RequestState error={options.error} loading={!options.data} />
  </div>
}

function BusinessEditor({ current, people, onClose, onSaved }: EditorProps<Business>) {
  const [participants, setParticipants] = useState(() => (current?.participants ?? []).map(person => ({ ...person, key: person.userId })))
  function update(key: string, patch: Partial<Participant>) {
    setParticipants(previous => previous.map(person => person.key === key ? { ...person, ...patch } : person))
  }
  return <RecordForm path="businesses" current={current} title={current ? '编辑业务' : '新建业务'} onClose={onClose} onSaved={onSaved}
    values={form => {
      const fields = Object.fromEntries(form)
      return { ...fields, eventTime: fields.eventTime || null,
        participants: participants.map(({ userId, responsibility }) => ({ userId, responsibility })) }
    }}>
    <label>业务名称<input name="title" required maxLength={160} defaultValue={current?.title} /></label>
    <label>类型<select name="kind" defaultValue={current?.kind || 'SALON'}><Options labels={businessKinds} /></select></label>
    <label>日期<input type="date" required name="eventDate" defaultValue={current?.eventDate} /></label>
    <label>开始时间<input type="time" name="eventTime" defaultValue={current?.eventTime || ''} /></label>
    <CustomerPicker current={current} />
    <label>负责人<PersonPicker people={people} value={current && { id: current.ownerUserId, displayName: current.ownerName }} /></label>
    <label>状态<select name="status" defaultValue={current?.status || 'PLANNED'}><Options labels={businessStatuses} /></select></label>
    <label className="crm-wide">备注<textarea name="notes" maxLength={2000} defaultValue={current?.notes || ''} /></label>
    <div className="crm-wide"><h3>参与员工</h3>
      {participants.map(person => <div className="crm-participant" key={person.key}>
        <label>员工<PersonPicker people={people} value={person.userId ? { id: person.userId, displayName: person.displayName || person.userId } : undefined}
          onSelect={id => update(person.key, { userId: id, displayName: people.find(item => item.id === id)?.displayName })} /></label>
        <label>职责<input required maxLength={128} value={person.responsibility} onChange={event => update(person.key, { responsibility: event.target.value })} /></label>
        <button type="button" className="button secondary" onClick={() => setParticipants(previous => previous.filter(item => item.key !== person.key))}>移除</button>
      </div>)}
      <button type="button" className="button secondary" disabled={participants.length >= 50}
        onClick={() => setParticipants(previous => [...previous, { key: crypto.randomUUID(), userId: '', responsibility: '' }])}>添加参与员工</button>
    </div>
  </RecordForm>
}
