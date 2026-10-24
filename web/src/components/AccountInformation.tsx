import { useEffect, useRef, useState } from 'react'
import type { AccountLogin, CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { accountLoginText } from '../data/accountHandoff.ts'
import { errorMessage } from '../api/feedback.ts'
import { handoffLabels, useCredentialHandoffs } from './useCredentialHandoffs.ts'
import { Icon } from './Icon.tsx'

export function AccountInformation({ account, allowHandoff = true }: { account: AccountLogin & { personId?: string }; allowHandoff?: boolean }) {
  const [visible, setVisible] = useState(false), [feedback, setFeedback] = useState('')
  const mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const { items, error, loading, refresh } = useCredentialHandoffs(account.personId, allowHandoff && !!account.personId)
  const current = items[0]
  const password = current?.status === 'AVAILABLE' ? current.initialPassword : undefined
  async function copy() {
    setFeedback('')
    try {
      const fresh = allowHandoff && account.personId ? (await refresh())[0] : undefined
      if (!mounted.current) return
      await navigator.clipboard.writeText(accountLoginText(fresh ?? account, fresh?.status === 'AVAILABLE' ? fresh.initialPassword : undefined))
      if (mounted.current) setFeedback('已复制')
    } catch (reason) { if (mounted.current) setFeedback(errorMessage(reason)) }
  }
  return <section className="account-information" aria-label="员工登录信息">
    <div className="account-information-heading"><h3>登录信息</h3><button className="text-button" type="button" disabled={loading} onClick={() => void copy()}>{password === undefined ? '复制登录信息' : '复制账号和初始密码'}</button></div>
    {account.enterpriseDomain && <label>公司登录标识<input readOnly value={account.enterpriseDomain} onFocus={event => event.target.select()} /></label>}
    <label>登录账号<input readOnly value={current?.loginName ?? account.loginName} onFocus={event => event.target.select()} /></label>
    {password !== undefined ? <label>初始/重置密码<div className="account-password"><input readOnly type={visible ? 'text' : 'password'} autoComplete="off" value={password} onFocus={event => event.target.select()} /><button className="button secondary" type="button" disabled={loading} aria-label={visible ? '隐藏交付密码' : '显示交付密码'} aria-pressed={visible} onClick={async () => { if (visible) setVisible(false); else { try { await refresh(); if (mounted.current) setVisible(true) } catch { /* Error is displayed by the loader. */ } } }}>{visible ? '隐藏' : '显示'}</button></div></label> : allowHandoff && <p role="status">{loading ? '加载中…' : current ? handoffLabels[current.status] : error || '无交付记录'}</p>}
    <p className="account-copy-feedback" role="status">{feedback}</p>
  </section>
}

export function AccountCreatedDialog({ account, onClose }: { account: CreatedAccountReceipt; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current, opener = document.activeElement as HTMLElement | null
    element?.showModal()
    return () => { element?.close(); opener?.focus() }
  }, [])
  return <dialog ref={dialog} className="editor-dialog account-created-dialog" aria-labelledby="account-created-title" onCancel={event => { event.preventDefault(); onClose() }}>
    <div className="dialog-head"><h2 id="account-created-title">{account.reason === 'reset' ? '员工密码已重置' : '人员与账号已创建'}</h2><button className="icon-button" aria-label="关闭账号交付信息" onClick={onClose}><Icon name="close" /></button></div>
    <div className="dialog-body"><AccountInformation account={account} /></div>
    <div className="dialog-footer"><button className="button primary" onClick={onClose}>完成</button></div>
  </dialog>
}
