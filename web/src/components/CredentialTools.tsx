import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import type { CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { credentialListText } from '../data/accountHandoff.ts'
import { downloadBytes, downloadText } from './downloadText.ts'
import { bulkPersonnelTemplate, createBulkPersonnel, importPersonnelTable, previewPersonnelTable } from '../data/bulkPersonnel.ts'
import type { BulkPersonnelPreview } from '../data/bulkPersonnel.ts'
import { adminRepository } from '../data/repository.ts'
import { validatePassword } from '../api/auth.ts'
import { ApiError } from '../api/client.ts'
import { errorMessage } from '../api/feedback.ts'
import { handoffLabels, useCredentialHandoffs } from './useCredentialHandoffs.ts'
import { availableHandoffAccounts } from '../data/accountHandoff.ts'
import { BulkPersonnelGrid } from './BulkPersonnelGrid.tsx'
import { Icon } from './Icon.tsx'
import { ValidatedForm } from './ValidatedForm.tsx'

function Frame({ title, busy = false, wide = false, onClose, children }: { title: string; busy?: boolean; wide?: boolean; onClose: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => { const element = dialog.current, opener = document.activeElement as HTMLElement | null; element?.showModal(); return () => { element?.close(); opener?.focus() } }, [])
  return <dialog ref={dialog} className={`editor-dialog credential-tools-dialog${wide ? ' bulk-personnel-dialog' : ''}`} aria-label={title} onCancel={event => { event.preventDefault(); if (!busy) onClose() }}><div className="dialog-head"><h2>{title}</h2><button className="icon-button" disabled={busy} aria-label={'关闭' + title} onClick={onClose}><Icon name="close" /></button></div>{children}</dialog>
}

function CopyDownload({ accounts, reload }: { accounts: CreatedAccountReceipt[]; reload?: () => Promise<CreatedAccountReceipt[]> }) {
  const [feedback, setFeedback] = useState(''), [busy, setBusy] = useState(false)
  const mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  async function deliver(download: boolean) {
    if (busy) return
    setBusy(true); setFeedback('')
    try {
      const ids = new Set(accounts.map(account => account.personId))
      const fresh = reload ? await reload() : availableHandoffAccounts(await adminRepository.credentialHandoffs()).filter(account => ids.has(account.personId))
      if (!mounted.current) return
      if (!fresh.length) throw new Error('暂无可导出的密码。')
      if (download) {
        const { credentialWorkbook } = await import('../data/credentialWorkbook.ts')
        const bytes = await credentialWorkbook(fresh)
        if (!mounted.current) return
        downloadBytes('员工账号交付清单.xlsx', bytes, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
      }
      else await navigator.clipboard.writeText(credentialListText(fresh))
      if (mounted.current) setFeedback(download ? `已导出 ${fresh.length} 人` : `已复制 ${fresh.length} 人`)
    } catch (reason) { if (mounted.current) setFeedback(errorMessage(reason)) }
    finally { if (mounted.current) setBusy(false) }
  }
  return <div className="credential-delivery-actions"><div><button className="button secondary" type="button" disabled={!accounts.length || busy} onClick={() => void deliver(false)}>一键复制清单</button><button className="button primary" type="button" disabled={!accounts.length || busy} onClick={() => void deliver(true)}>下载账号清单</button></div><p role="status">{feedback}</p></div>
}

export function ExportAccountsDialog({ onClose }: { onClose: () => void }) {
  const { items, error, loading, refresh } = useCredentialHandoffs()
  const accounts = availableHandoffAccounts(items)
  return <Frame title="批量导出账号与初始密码" wide onClose={onClose}><div className="dialog-body">{error && <p className="error-message" role="alert">{error}</p>}<div className="bulk-result-table handoff-status-table"><table><thead><tr><th>姓名</th><th>账号</th><th>初始密码</th><th>密码状态</th></tr></thead><tbody>{items.map(item => <tr key={item.personId}><td>{item.displayName}</td><td>{item.loginName}</td><td className="handoff-password-value">{item.status === 'AVAILABLE' ? item.initialPassword : '—'}</td><td>{handoffLabels[item.status]}</td></tr>)}</tbody></table></div><label className="credential-list-label">交付清单（{accounts.length} 人）<textarea readOnly rows={5} value={credentialListText(accounts)} onFocus={event => event.target.select()} /></label><CopyDownload accounts={accounts} reload={async () => availableHandoffAccounts(await refresh())} /></div><div className="dialog-footer"><button className="button secondary" disabled={loading} onClick={() => void refresh().catch(() => undefined)}>刷新</button><button className="button secondary" onClick={onClose}>关闭</button></div></Frame>
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
      setError(unavailable ? '当前服务器尚未提供该员工的密码重置接口，或账号已不存在。请关闭并刷新核对；若账号仍在，请后端部署新接口。' : `${errorMessage(reason)} 请关闭并刷新，确认账号和密码状态后再操作。`)
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
  return <Frame title="批量创建员工账号" wide busy={busy} onClose={onClose}>
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
      {receipts.length > 0 && <CopyDownload accounts={receipts} />}
    </div>
    <div className="dialog-footer">
      {busy ? <button className="button secondary" disabled={stopping} onClick={() => { stopped.current = true; setStopping(true) }}>{stopping ? '正在停止后续创建…' : '停止后续创建'}</button> : <button className="button secondary" onClick={onClose}>关闭</button>}
      <button className="button primary" disabled={busy || attempted || importing} onClick={() => void create()}>{busy ? '正在创建…' : '确认批量创建'}</button>
    </div>
  </Frame>
}
