import { useDeferredValue, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { apiClient } from '../../api/config.ts'
import { errorMessage } from '../../api/feedback.ts'
import { crmPath, useCrmQuery } from './api.ts'
import type { Labels, Person, Row } from './api.ts'
import { CrmDialog, Detail, Options, Pagination, PersonPicker, RecordForm, RecordTable, RequestState } from './shared.tsx'

type Contract = Row & {
  employeeUserId: string; employeeName: string; title: string; startDate: string; endDate: string;
  expiry: string; notes: string | null; attachmentName: string | null;
}
const expiries: Labels = { EXPIRING: '30 天内到期', EXPIRED: '已到期', ACTIVE: '有效', UPCOMING: '未开始' }
const columns = [
  { label: '合同', value: (row: Contract) => row.title },
  { label: '员工', value: (row: Contract) => row.employeeName },
  { label: '开始日期', value: (row: Contract) => row.startDate },
  { label: '到期日期', value: (row: Contract) => row.endDate },
  { label: '状态', value: (row: Contract) => <strong className={row.expiry === 'EXPIRED' || row.expiry === 'EXPIRING' ? 'crm-warning' : undefined}>{expiries[row.expiry]}</strong> },
]

export function ContractsPage({ people, refresh }: { people: Person[]; refresh: unknown }) {
  const [search, setSearch] = useState('')
  const [expiry, setExpiry] = useState('')
  const [offset, setOffset] = useState(0)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const key = useMemo(() => ({ refresh, revision }), [refresh, revision])
  const list = useCrmQuery<Contract[]>(crmPath('contracts', { q: useDeferredValue(search), expiry, offset }), key)
  const detail = useCrmQuery<Contract>(selected && crmPath('contracts/' + selected), key)
  const record = detail.data

  return <>
    <div className="crm-toolbar">
      <label>搜索员工<input type="search" value={search} onChange={event => { setSearch(event.target.value); setOffset(0) }} /></label>
      <label>到期状态<select value={expiry} onChange={event => { setExpiry(event.target.value); setOffset(0) }}>
        <option value="">全部</option><Options labels={expiries} />
      </select></label>
      <button className="button primary crm-new" onClick={() => { setSelected(null); setEditing(true) }}>新增合同</button>
    </div>
    <RequestState loading={!list.data} error={list.error} />
    {list.data && <RecordTable columns={columns} rows={list.data} onOpen={setSelected} />}
    <Pagination offset={offset} onChange={setOffset} count={list.data?.length} />
    {selected && !editing && <CrmDialog title="劳动合同详情" onClose={() => setSelected(null)}>
      <RequestState loading={!record} error={detail.error} />
      {record && <>
        <Detail values={[[ '合同', record.title ], [ '员工', record.employeeName ],
          [ '开始日期', record.startDate ], [ '到期日期', record.endDate ],
          [ '到期状态', expiries[record.expiry] ], [ '备注', record.notes ]]} />
        <Attachment key={record.id + ':' + record.version} record={record} onSaved={() => setRevision(previous => previous + 1)} />
      </>}
      <div className="crm-actions">
        <button className="button secondary" onClick={() => setSelected(null)}>关闭</button>
        {record && <button className="button primary" onClick={() => setEditing(true)}>编辑</button>}
      </div>
    </CrmDialog>}
    {editing && <RecordForm path="contracts" current={selected ? record : undefined} title={selected ? '编辑劳动合同' : '新增劳动合同'}
      onClose={() => setEditing(false)} onSaved={saved => { setEditing(false); setSelected(saved.id); setRevision(previous => previous + 1) }}
      values={form => ({ ...Object.fromEntries(form), employeeUserId: record?.employeeUserId ?? form.get('employeeUserId') })}>
      <label>合同名称<input name="title" required maxLength={128} defaultValue={record?.title} /></label>
      {record ? <p>员工 · {record.employeeName}</p> : <label>员工<PersonPicker people={people} name="employeeUserId" /></label>}
      <label>开始日期<input name="startDate" type="date" required defaultValue={record?.startDate} /></label>
      <label>到期日期<input name="endDate" type="date" required defaultValue={record?.endDate} /></label>
      <label className="crm-wide">备注<textarea name="notes" maxLength={2000} defaultValue={record?.notes || ''} /></label>
    </RecordForm>}
  </>
}

function Attachment({ record, onSaved }: { record: Contract; onSaved: () => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const path = 'contracts/' + record.id + '/attachment'

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const form = new FormData(event.currentTarget)
    const file = form.get('file')
    if (!(file instanceof File) || !file.size || file.size > 10 * 1024 * 1024) {
      setError('请选择 10 MiB 以内的 PDF 文件。')
      return
    }
    setBusy(true)
    setError('')
    try {
      await apiClient.request(crmPath(path, { version: record.version }), { method: 'PUT', body: form })
      onSaved()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }
  async function download() {
    setBusy(true)
    setError('')
    try {
      const file = await apiClient.request<Blob>(crmPath(path), { responseFormat: 'blob' })
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = url
      link.download = record.attachmentName ?? 'contract.pdf'
      link.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }
  return <section className="crm-subsection">
    <h3>合同附件</h3>
    {record.attachmentName && <p><button className="button secondary" disabled={busy} onClick={() => void download()}>下载 {record.attachmentName}</button></p>}
    <form className="crm-toolbar" onSubmit={event => void upload(event)}>
      <label>选择 PDF（最多 10 MiB）<input name="file" type="file" accept="application/pdf,.pdf" required disabled={busy} /></label>
      <button className="button secondary" disabled={busy}>{record.attachmentName ? '替换附件' : '上传附件'}</button>
    </form>
    {error && <p role="alert" className="error-message">{error}</p>}
  </section>
}
