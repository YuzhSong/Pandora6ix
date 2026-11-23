import { useDeferredValue, useMemo, useState } from 'react'
import { crmPath, useCrmQuery } from './api.ts'
import type { Labels, Row } from './api.ts'
import { CustomerPicker, Detail, Options, Pagination, RecordForm, RecordTable, RequestState } from './shared.tsx'

type Membership = Row & { customerId: string; customerName: string; membershipType: string; startDate: string; endDate: string }
type Benefit = Row & { name: string; notes: string | null }
type Usage = Row & { serviceDate: string; status: string; notes: string | null }
type Editor = { type: 'member'; current?: Membership } | { type: 'benefit'; current?: Benefit } | { type: 'usage'; current?: Usage }
const useStatuses: Labels = { SCHEDULED: '待服务', COMPLETED: '已完成', CANCELLED: '已取消' }
const memberColumns = [
  { label: '客户', value: (row: Membership) => row.customerName },
  { label: '会员类型', value: (row: Membership) => row.membershipType },
  { label: '开始日期', value: (row: Membership) => row.startDate },
  { label: '到期日期', value: (row: Membership) => row.endDate },
]
const benefitColumns = [
  { label: '权益', value: (row: Benefit) => row.name },
  { label: '说明', value: (row: Benefit) => row.notes },
]
const useColumns = [
  { label: '服务日期', value: (row: Usage) => row.serviceDate },
  { label: '服务状态', value: (row: Usage) => useStatuses[row.status] },
  { label: '记录', value: (row: Usage) => row.notes },
]

export function MembershipPage({ refresh }: { refresh: unknown }) {
  const [search, setSearch] = useState('')
  const [offset, setOffset] = useState(0)
  const [benefitOffset, setBenefitOffset] = useState(0)
  const [useOffset, setUseOffset] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)
  const [selectedBenefit, setSelectedBenefit] = useState<Benefit | null>(null)
  const [editor, setEditor] = useState<Editor | null>(null)
  const [revision, setRevision] = useState(0)
  const key = useMemo(() => ({ refresh, revision }), [refresh, revision])
  const list = useCrmQuery<Membership[]>(crmPath('memberships', { q: useDeferredValue(search), offset }), key)
  const memberPath = selected ? 'memberships/' + selected : null
  const member = useCrmQuery<Membership>(memberPath && crmPath(memberPath), key)
  const benefitsPath = memberPath && memberPath + '/benefits'
  const benefits = useCrmQuery<Benefit[]>(benefitsPath && crmPath(benefitsPath, { offset: benefitOffset }), key)
  const usesPath = selectedBenefit && 'benefits/' + selectedBenefit.id + '/uses'
  const uses = useCrmQuery<Usage[]>(usesPath && crmPath(usesPath, { offset: useOffset }), key)

  function select(id: string | null) {
    setSelected(id)
    setSelectedBenefit(null)
    setBenefitOffset(0)
    setUseOffset(0)
  }
  function saved() {
    setEditor(null)
    setRevision(previous => previous + 1)
  }
  const current = member.data
  return <>
    <div className="crm-toolbar">
      <label>搜索客户<input type="search" value={search} onChange={event => { setSearch(event.target.value); setOffset(0); select(null) }} /></label>
      <button className="button primary crm-new" onClick={() => setEditor({ type: 'member' })}>登记会员</button>
    </div>
    <RequestState loading={!list.data} error={list.error} />
    {list.data && <RecordTable columns={memberColumns} rows={list.data} onOpen={select} />}
    <Pagination offset={offset} onChange={setOffset} count={list.data?.length} />
    {selected && <section className="crm-subsection" aria-label="会员详情">
      <RequestState loading={!current} error={member.error} />
      {current && <><h2>{current.customerName} · {current.membershipType}</h2>
        <Detail values={[[ '开始日期', current.startDate ], [ '到期日期', current.endDate ]]} />
        <div className="crm-toolbar"><button className="button secondary" onClick={() => setEditor({ type: 'member', current })}>编辑会员</button>
          <button className="button primary" onClick={() => setEditor({ type: 'benefit' })}>新增权益</button></div>
        <h3>会员权益</h3>
        <RequestState loading={!benefits.data} error={benefits.error} />
        {benefits.data && <RecordTable columns={benefitColumns} rows={benefits.data} onOpen={id => {
          const benefit = benefits.data?.find(row => row.id === id)
          if (benefit) { setSelectedBenefit(benefit); setUseOffset(0) }
        }} />}
        <Pagination offset={benefitOffset} onChange={setBenefitOffset} count={benefits.data?.length} />
      </>}
      {selectedBenefit && <section className="crm-subsection" aria-label="权益使用记录">
        <h3>{selectedBenefit.name} · 使用记录</h3>
        <div className="crm-toolbar"><button className="button secondary" onClick={() => setEditor({ type: 'benefit', current: selectedBenefit })}>编辑权益</button>
          <button className="button primary" onClick={() => setEditor({ type: 'usage' })}>记录服务</button></div>
        <RequestState loading={!uses.data} error={uses.error} />
        {uses.data && <RecordTable columns={useColumns} rows={uses.data} onOpen={id => {
          const use = uses.data?.find(row => row.id === id)
          if (use) setEditor({ type: 'usage', current: use })
        }} />}
        <Pagination offset={useOffset} onChange={setUseOffset} count={uses.data?.length} />
      </section>}
    </section>}
    {editor?.type === 'member' && <RecordForm path="memberships" current={editor.current}
      title={editor.current ? '编辑会员' : '登记会员'} onClose={() => setEditor(null)} onSaved={saved}
      values={form => ({ ...Object.fromEntries(form), customerId: editor.current?.customerId ?? form.get('customerId') })}>
      {editor.current ? <p className="crm-wide">客户 · {editor.current.customerName}</p> : <CustomerPicker />}
      <label className="crm-wide">会员类型<input name="membershipType" required maxLength={80} defaultValue={editor.current?.membershipType} /></label>
      <label>开始日期<input name="startDate" type="date" required defaultValue={editor.current?.startDate} /></label>
      <label>到期日期<input name="endDate" type="date" required defaultValue={editor.current?.endDate} /></label>
    </RecordForm>}
    {editor?.type === 'benefit' && benefitsPath && <RecordForm path={benefitsPath} current={editor.current}
      title={editor.current ? '编辑权益' : '新增权益'} onClose={() => setEditor(null)} onSaved={benefit => {
        if (selectedBenefit?.id === benefit.id) setSelectedBenefit(benefit)
        saved()
      }}>
      <label className="crm-wide">权益名称<input name="name" required maxLength={128} defaultValue={editor.current?.name} /></label>
      <label className="crm-wide">说明<textarea name="notes" maxLength={1000} defaultValue={editor.current?.notes || ''} /></label>
    </RecordForm>}
    {editor?.type === 'usage' && usesPath && <RecordForm path={usesPath} current={editor.current}
      title={editor.current ? '编辑服务记录' : '记录服务'} onClose={() => setEditor(null)} onSaved={saved}>
      <label>服务日期<input name="serviceDate" type="date" required defaultValue={editor.current?.serviceDate} /></label>
      <label>服务状态<select name="status" defaultValue={editor.current?.status || 'SCHEDULED'}><Options labels={useStatuses} /></select></label>
      <label className="crm-wide">记录<textarea name="notes" maxLength={1000} defaultValue={editor.current?.notes || ''} /></label>
    </RecordForm>}
  </>
}
