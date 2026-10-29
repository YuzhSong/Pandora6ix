import { CredentialDeliveryActions } from './CredentialDeliveryActions.tsx'
import { useEffect, useRef, useState } from 'react'
import type { AccountLogin, CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { accountIdentifier, accountLoginText } from '../data/accountHandoff.ts'
import { errorMessage } from '../api/feedback.ts'
import { handoffLabels, useCredentialHandoffs } from './useCredentialHandoffs.ts'
import { Icon } from './Icon.tsx'

export function AccountInformation({ account, allowHandoff = true, compact = false, onResetPassword, showCopy = true }: { account: AccountLogin & { personId?: string }; allowHandoff?: boolean; compact?: boolean; onResetPassword?: () => void; showCopy?: boolean }) {
  const [visible, setVisible] = useState(false), [feedback, setFeedback] = useState('')
  const [handoffExpanded, setHandoffExpanded] = useState(!compact)
  const handoffEnabled = allowHandoff && handoffExpanded
  const mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const { items, error, loading, refresh } = useCredentialHandoffs(account.personId, handoffEnabled && !!account.personId)
  const current = handoffEnabled ? items[0] : undefined
  const password = current?.status === 'AVAILABLE' ? current.initialPassword : undefined
  async function copy() {
    setFeedback('')
    try {
      const fresh = handoffEnabled && account.personId ? (await refresh())[0] : undefined
      if (!mounted.current) return
      await navigator.clipboard.writeText(accountLoginText(fresh ?? account, fresh?.status === 'AVAILABLE' ? fresh.initialPassword : undefined))
      if (mounted.current) setFeedback('已复制')
    } catch (reason) { if (mounted.current) setFeedback(errorMessage(reason)) }
  }
  return <section className={`account-information${compact ? ' compact-account-information person-profile-section' : ''}`} aria-label="员工登录信息">
    <div className="account-information-heading"><h3>{compact ? '账号信息' : '登录信息'}</h3>{showCopy && <button className="text-button" type="button" disabled={handoffEnabled && loading} onClick={() => void copy()}>{password === undefined ? '复制登录信息' : '复制账号和初始密码'}</button>}</div>
    {compact ? <dl className="person-profile-grid"><div className="profile-full-row"><dt>登录账号</dt><dd>{accountIdentifier(current ?? account)}</dd></div></dl> : <label>登录账号<input readOnly value={accountIdentifier(current ?? account)} onFocus={event => event.target.select()} /></label>}
    {compact && (allowHandoff || onResetPassword) && <div className="person-account-actions">{allowHandoff && <button className="text-button" type="button" aria-expanded={handoffExpanded} onClick={() => { setHandoffExpanded(value => !value); setVisible(false) }}>{handoffExpanded ? '收起初始密码' : '查看初始密码'}</button>}{onResetPassword && <button className="text-button" type="button" onClick={onResetPassword}>重置密码</button>}</div>}
    {password !== undefined ? <label>初始/重置密码<div className="account-password"><input readOnly type={visible ? 'text' : 'password'} autoComplete="off" value={password} onFocus={event => event.target.select()} /><button className="button secondary" type="button" disabled={loading} aria-label={visible ? '隐藏交付密码' : '显示交付密码'} aria-pressed={visible} onClick={async () => { if (visible) setVisible(false); else { try { await refresh(); if (mounted.current) setVisible(true) } catch { /* Error is displayed by the loader. */ } } }}>{visible ? '隐藏' : '显示'}</button></div></label> : handoffEnabled && <p role="status">{loading ? '加载中…' : error || (compact ? '当前无可查看的初始密码' : current ? handoffLabels[current.status] : '无交付记录')}</p>}
    <p className="account-copy-feedback" role="status">{feedback}</p>
  </section>
}

export function AccountCreatedDialog({ account, onClose }: { account: CreatedAccountReceipt; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [delivering, setDelivering] = useState(false)
  useEffect(() => {
    const element = dialog.current, opener = document.activeElement as HTMLElement | null
    element?.showModal()
    return () => { element?.close(); opener?.focus() }
  }, [])
  return <dialog ref={dialog} className="editor-dialog account-created-dialog" aria-labelledby="account-created-title" onCancel={event => { event.preventDefault(); if (!delivering) onClose() }}>
    <div className="dialog-head"><h2 id="account-created-title">{account.reason === 'reset' ? '员工密码已重置' : '人员与账号已创建'}</h2><button className="icon-button" aria-label="关闭账号交付信息" disabled={delivering} onClick={onClose}><Icon name="close" /></button></div>
    <div className="dialog-body"><AccountInformation account={account} showCopy={false} /><CredentialDeliveryActions personIds={[account.personId]} copyLabel="复制员工登录信息" downloadLabel="导出此账号 Excel" onBusyChange={setDelivering} /></div>
    <div className="dialog-footer"><button className="button primary" disabled={delivering} onClick={onClose}>完成</button></div>
  </dialog>
}
