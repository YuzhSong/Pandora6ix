import { useDeferredValue, useEffect, useId, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { apiClient } from '../../api/config.ts'
import { errorMessage } from '../../api/feedback.ts'

import { crmPath, useCrmQuery } from './api.ts'
import type { Labels, Person, Row } from './api.ts'

export function RequestState({ error, loading }: { error?: string; loading: boolean }) {
  if (error) return <p className="error-message" role="alert">{error}</p>
  if (loading) return <p role="status">正在加载…</p>
  return null
}

export function Options({ labels }: { labels: Labels }) {
  return Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)
}

export function PersonPicker({ people, value, name = 'ownerUserId', required = true, onSelect }: {
  people: Person[]; value?: Person; name?: string; required?: boolean; onSelect?: (id: string) => void
}) {
  return <select name={name} defaultValue={value?.id || ''} required={required} onChange={event => onSelect?.(event.target.value)}>
    <option value="">请选择员工</option>
    {value && !people.some(person => person.id === value.id) && <option value={value.id}>{value.displayName}</option>}
    {people.map(person => <option key={person.id} value={person.id}>{person.displayName}</option>)}
  </select>
}

export function Detail({ values }: { values: [string, ReactNode][] }) {
  return <dl className="crm-detail">{values.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
}

export function CrmDialog({ title, children, onClose, busy = false }: {
  title: string; children: ReactNode; onClose: () => void; busy?: boolean
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const focus = document.activeElement as HTMLElement | null
    ref.current?.showModal()
    return () => focus?.focus()
  }, [])
  return <dialog className="confirm-dialog crm-dialog" ref={ref} aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="dialog-body"><h2 id={titleId}>{title}</h2>{children}</div>
  </dialog>
}

export function RecordForm<T extends Row>({ path, current, title, children, onClose, onSaved, values }: {
  path: string; current?: T; title: string; children: ReactNode; onClose: () => void; onSaved: (record: T) => void;
  values?: (form: FormData) => Record<string, unknown>
}) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const data = new FormData(event.currentTarget)
    const body = values ? values(data) : Object.fromEntries(data)
    if (current) body.version = current.version
    setBusy(true)
    setError('')
    try {
      const saved = await apiClient.request<T>(crmPath(path + (current ? '/' + current.id : '')), {
        method: current ? 'PUT' : 'POST', body,
      })
      onSaved(saved)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }
  return <CrmDialog title={title} onClose={onClose} busy={busy}>
    <form onSubmit={event => void submit(event)}>
      <fieldset className="crm-fields" disabled={busy}>{children}</fieldset>
      {error && <p className="error-message" role="alert">{error}</p>}
      <div className="crm-actions">
        <button type="button" className="button secondary" onClick={onClose} disabled={busy}>取消</button>
        <button className="button primary" disabled={busy}>{busy ? '保存中…' : '保存'}</button>
      </div>
    </form>
  </CrmDialog>
}

export function RecordTable<T extends Row>({ columns, rows, onOpen }: {
  columns: { label: string; value: (row: T) => ReactNode }[]; rows: T[]; onOpen?: (id: string) => void
}) {
  if (!rows.length) return <p className="crm-empty">没有符合条件的记录。</p>
  return <div className="crm-table"><table>
    <thead><tr>{columns.map(column => <th key={column.label} scope="col">{column.label}</th>)}{onOpen && <th scope="col">操作</th>}</tr></thead>
    <tbody>{rows.map(row => <tr key={row.id}>
      {columns.map(column => <td key={column.label}>{column.value(row) ?? '—'}</td>)}
      {onOpen && <td><button className="button secondary" onClick={() => onOpen(row.id)}>查看</button></td>}
    </tr>)}</tbody>
  </table></div>
}

export function Pagination({ offset, onChange, count }: { offset: number; onChange: (offset: number) => void; count?: number }) {
  if (!offset && count !== 100) return null
  return <div className="crm-actions"><button className="button secondary" disabled={!offset} onClick={() => onChange(offset - 100)}>上一页</button>
    <span>第 {offset / 100 + 1} 页</span>
    <button className="button secondary" disabled={count !== 100} onClick={() => onChange(offset + 100)}>下一页</button></div>
}

export function CustomerPicker({ current }: { current?: { customerId: string; customerName: string } }) {
  const [search, setSearch] = useState('')
  const [selection, setSelection] = useState(current)
  const options = useCrmQuery<{ id: string; name: string }[]>(crmPath('customers', { q: useDeferredValue(search) }))
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
