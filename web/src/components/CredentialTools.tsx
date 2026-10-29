import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import type { CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { accountIdentifier } from '../data/accountHandoff.ts'
import { downloadText } from './downloadText.ts'
import { bulkPersonnelTemplate, createBulkPersonnel, importPersonnelTable, previewPersonnelTable } from '../data/bulkPersonnel.ts'
import type { BulkPersonnelPreview } from '../data/bulkPersonnel.ts'
import { adminRepository } from '../data/repository.ts'
import { validatePassword } from '../api/auth.ts'
import { ApiError } from '../api/client.ts'
import { errorMessage } from '../api/feedback.ts'
import { handoffLabels, useCredentialHandoffs } from './useCredentialHandoffs.ts'
import { searchDeliveryAccounts } from '../data/credentialDelivery.ts'
import { CredentialDeliveryActions } from './CredentialDeliveryActions.tsx'
import { BulkPersonnelGrid } from './BulkPersonnelGrid.tsx'
import { Icon } from './Icon.tsx'
import { ValidatedForm } from './ValidatedForm.tsx'

function Frame({ title, busy = false, wide = false, onClose, children }: { title: string; busy?: boolean; wide?: boolean; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => { const element = dialog.current, opener = document.activeElement as HTMLElement | null; element?.showModal(); return () => { element?.close(); opener?.focus() } }, [])
  return <dialog ref={dialog} className={`editor-dialog credential-tools-dialog${wide ? ' bulk-personnel-dialog' : ''}`} aria-label={title} onCancel={event => { event.preventDefault(); if (!busy) onClose() }}><div className="dialog-head"><h2>{title}</h2><button className="icon-button" disabled={busy} aria-label={'关闭' + title} onClick={onClose}><Icon name="close" /></button></div>{children}</dialog>
}

export function ExportAccountsDialog({ onClose }: { onClose: () => void }) {
  const { items, error, loading, refresh } = useCredentialHandoffs()
  const [query, setQuery] = useState(''), [selected, setSelected] = useState<string[]>([]), [showPasswords, setShowPasswords] = useState(false), [delivering, setDelivering] = useState(false)
  const available = searchDeliveryAccounts(items, ''), matches = searchDeliveryAccounts(items, query)
  const availableIds = new Set(available.map(item => item.personId))
  const selectedIds = selected.filter(id => availableIds.has(id))
  const allVisibleSelected = matches.length > 0 && matches.every(item => selectedIds.includes(item.personId))
  const unavailable = items.filter(item => item.status !== 'AVAILABLE')
  return <Frame title="账号发放" wide busy={delivering} onClose={onClose}>
    <div className="dialog-body account-delivery-body">
      <fieldset className="delivery-controls" disabled={delivering || loading}>
        <label className="search-box"><Icon name="search" size={18} /><input aria-label="搜索交付账号" placeholder="搜索姓名或登录账号" value={query} onChange={event => { setQuery(event.target.value); setSelected([]); setShowPasswords(false) }} /></label>
        <label className="delivery-checkbox"><input type="checkbox" checked={allVisibleSelected} disabled={!matches.length} onChange={event => setSelected(event.target.checked ? matches.map(item => item.personId) : [])} />全选当前结果</label>
        <label className="delivery-checkbox"><input type="checkbox" checked={showPasswords} disabled={!matches.length} onChange={event => setShowPasswords(event.target.checked)} />显示初始密码</label>
      </fieldset>
      <p className="delivery-count" role="status">可交付 {available.length} 人 · 当前 {matches.length} 人 · 已选 {selectedIds.length} 人</p>
      {error ? <p className="error-message" role="alert">{error}</p> : loading && !items.length ? <p role="status">正在加载账号…</p> : !available.length ? <div className="delivery-empty"><h3>暂无可交付的账号</h3><p>新建员工后，可在创建结果中直接复制或导出账号。</p></div> : !matches.length ? <div className="delivery-empty"><h3>未找到匹配的账号</h3><button className="text-button" onClick={() => { setQuery(''); setSelected([]) }}>清空搜索</button></div> : <div className="bulk-result-table delivery-accounts-table"><table><thead><tr><th>选择</th><th>姓名</th><th>登录账号</th><th>初始密码</th><th>可交付至</th></tr></thead><tbody>{matches.map(item => <tr key={item.personId}>
        <td><input type="checkbox" aria-label={`选择账号：${item.displayName}（${accountIdentifier(item)}）`} disabled={delivering || loading} checked={selectedIds.includes(item.personId)} onChange={event => setSelected(current => event.target.checked ? [...current, item.personId] : current.filter(id => id !== item.personId))} /></td>
        <td>{item.displayName}</td><td>{accountIdentifier(item)}</td><td className="handoff-password-value">{showPasswords ? item.initialPassword : '••••••••'}</td><td>{item.expiresAt ? new Date(item.expiresAt).toLocaleString('zh-CN') : '—'}</td>
      </tr>)}</tbody></table></div>}
      <div hidden={!available.length}><CredentialDeliveryActions personIds={selectedIds} disabled={loading || !!error} reload={refresh} onBusyChange={setDelivering} /></div>
      {unavailable.length > 0 && <details className="delivery-unavailable"><summary>不可交付账号（{unavailable.length} 人）</summary><div className="bulk-result-table"><table><thead><tr><th>姓名</th><th>登录账号</th><th>状态</th></tr></thead><tbody>{unavailable.map(item => <tr key={item.personId}><td>{item.displayName}</td><td>{accountIdentifier(item)}</td><td>{item.status === 'UNAVAILABLE' ? '初始密码不可用' : handoffLabels[item.status]}</td></tr>)}</tbody></table></div></details>}
    </div>
    <div className="dialog-footer"><button className="button secondary" disabled={loading || delivering} onClick={() => { setShowPasswords(false); void refresh().catch(() => undefined) }}>刷新</button><button className="button secondary" disabled={delivering} onClick={onClose}>关闭</button></div>
  </Frame>
}

export function ResetEmployeePasswordDialog({ person, onClose, onReset }: { person: Person; onClose: () => void; onReset: (data: AdminData, account: CreatedAccountReceipt) => void }) {
  const [password, setPassword] = useState(''), [confirm, setConfirm] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [refreshRequired, setRefreshRequired] = useState(false)
  const submitting = useRef(false)
  return <Frame title="重置员工密码" busy={busy} onClose={onClose}><ValidatedForm onSubmit={async event => {
    event.preventDefault(); if (submitting.current || refreshRequired) return
    try { validatePassword(password); if (password !== confirm) throw new Error('两次密码不一致。') } catch (reason) { setError(errorMessage(reason)); return }
    submitting.current = true; setBusy(true); setError('')
    try {
      const data = await adminRepository.resetPersonPassword(person.id, password, person.version)
      const target = data.people.find(value => value.id === person.id)!
      onReset(data, { personId: target.id, displayName: target.displayName, loginName: target.loginName, enterpriseDomain: data.enterprise.loginDomain, initialPassword: password, reason: 'reset' })
    } catch (reason) {
      const unavailable = reason instanceof ApiError && (reason.status === 404 || reason.status === 405)
      setError(unavailable ? '暂时无法重置密码，请关闭并刷新资料后重试。' : `${errorMessage(reason)} 请关闭并刷新，确认账号和密码状态后再操作。`)
      setRefreshRequired(true)
    } finally { submitting.current = false; setBusy(false) }
  }}><div className="dialog-body"><p>为 {person.displayName}（{person.loginName}）设置新密码。成功后该员工的旧登录会话将失效。</p>{person.status === 'DISABLED' && <p className="panel-note">账号目前停用，重置密码不会启用账号。</p>}<fieldset disabled={busy || refreshRequired}><label>新密码<input type="password" required minLength={10} maxLength={72} autoComplete="new-password" value={password} onChange={event => setPassword(event.target.value)} /></label><label>确认新密码<input type="password" required minLength={10} maxLength={72} autoComplete="new-password" value={confirm} onChange={event => setConfirm(event.target.value)} /></label></fieldset>{error && <p className="error-message" role="alert">{error}</p>}</div><div className="dialog-footer"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>取消</button><button type="submit" className="button primary" disabled={busy || refreshRequired}>{busy ? '正在重置…' : '确认重置'}</button></div></ValidatedForm></Frame>
}

export function BulkPersonnelDialog({ data, onClose, onCreated }: { data: AdminData; onClose: () => void; onCreated: (data: AdminData, account: CreatedAccountReceipt) => void }) {
  const [importing, setImporting] = useState(false)
  const [cells, setCells] = useState<string[][]>(() => Array.from({ length: 3 }, () => Array<string>(9).fill('')))
  const [preview, setPreview] = useState<BulkPersonnelPreview | null>(null), [busy, setBusy] = useState(false), [attempted, setAttempted] = useState(false), [stopping, setStopping] = useState(false), [fileError, setFileError] = useState('')
  const [statuses, setStatuses] = useState<Record<number, { status: string; message?: string }>>({})
  const [receipts, setReceipts] = useState<CreatedAccountReceipt[]>([])
  const [delivering, setDelivering] = useState(false)
  const stopped = useRef(false), submitted = useRef(false), mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; stopped.current = true } }, [])
  useEffect(() => { if (!busy) return; const protect = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }; window.addEventListener('beforeunload', protect); return () => window.removeEventListener('beforeunload', protect) }, [busy])
  async function create() {
    if (submitted.current || importing) return
    const checked = previewPersonnelTable(cells, data)
    setPreview(checked); setFileError('')
    if (checked.errors.length || !checked.rows.length) return
    submitted.current = true; stopped.current = false; setAttempted(true); setBusy(true)
    try { await createBulkPersonnel(checked.rows, adminRepository, { stopped: () => stopped.current, onRow: (index, status, message) => { if (mounted.current) setStatuses(current => ({ ...current, [index]: { status, message } })) }, onCreated: (next, account) => { if (mounted.current) { setReceipts(current => [...current, account]); onCreated(next, account) } } }) }
    finally { if (mounted.current) setBusy(false) }
  }
  return <Frame title="批量创建员工账号" wide busy={busy || delivering} onClose={onClose}>
    <div className="dialog-body">
      <fieldset disabled={busy || attempted || importing}>
        <div className="bulk-entry-toolbar">
          <button className="button secondary" type="button" onClick={() => downloadText('员工批量创建模板.csv', bulkPersonnelTemplate(), 'text/csv;charset=utf-8')}>下载 CSV 模板</button>
          <label className="bulk-import-button button secondary">{importing ? '正在导入…' : '导入 CSV 文件'}<input type="file" aria-label="导入 CSV 文件" accept=".csv,text/csv" onChange={async event => {
            const file = event.target.files?.[0]; event.target.value = ''; if (!file) return
            setFileError(''); setPreview(null)
            if (file.size > 1_000_000) { setFileError('文件过大，请每批最多 100 人。'); return }
            setImporting(true)
            try { const text = await file.text(); if (mounted.current && !submitted.current) setCells(importPersonnelTable(text, data)) }
            catch (reason) { if (mounted.current) setFileError(errorMessage(reason)) }
            finally { if (mounted.current) setImporting(false) }
          }} /></label>
        </div>
        <BulkPersonnelGrid rows={cells} data={data} onChange={rows => { setCells(rows); setPreview(null); setFileError('') }} />
      </fieldset>
      {fileError && <p className="error-message" role="alert">{fileError}</p>}
      {preview && <>
        {preview.errors.length > 0 && <div className="error-message" role="alert">{preview.errors.map(error => <p key={error}>{error}</p>)}</div>}
        {attempted && <div className="bulk-result-table"><table><thead><tr><th>行</th><th>姓名</th><th>登录账号</th><th>状态</th></tr></thead><tbody>{preview.rows.map((row, index) => <tr key={row.rowNumber}><td>{row.rowNumber}</td><td>{row.input.displayName}</td><td>{row.input.loginName}</td><td>{statuses[index]?.status === 'success' ? '已创建' : statuses[index]?.status === 'creating' ? '正在创建' : statuses[index]?.status === 'failed' ? '失败/待核实' : '未执行'}{statuses[index]?.message && <p className="bulk-row-error" role="alert">{statuses[index].message}</p>}</td></tr>)}</tbody></table></div>}
      </>}
      {attempted && <p role="status">已创建 {receipts.length} / {preview?.rows.length ?? 0} 位员工。</p>}
      {receipts.length > 0 && <section className="creation-delivery-result"><h3>本次已创建 {receipts.length} 人</h3><CredentialDeliveryActions personIds={receipts.map(account => account.personId)} disabled={busy} copyLabel="复制本次账号" downloadLabel="导出本次账号 Excel" onBusyChange={setDelivering} /></section>}
    </div>
    <div className="dialog-footer">
      {busy ? <button className="button secondary" disabled={stopping} onClick={() => { stopped.current = true; setStopping(true) }}>{stopping ? '正在停止后续创建…' : '停止后续创建'}</button> : <button className="button secondary" disabled={delivering} onClick={onClose}>关闭</button>}
      <button className="button primary" disabled={busy || attempted || importing} onClick={() => void create()}>{busy ? '正在创建…' : '确认批量创建'}</button>
    </div>
  </Frame>
}
