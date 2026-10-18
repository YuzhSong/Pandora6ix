import { useEffect, useRef, useState } from 'react'
import type { AccountLogin, CreatedAccountReceipt } from '../data/accountHandoff.ts'
import { accountLoginText } from '../data/accountHandoff.ts'
import { Icon } from './Icon.tsx'

export function AccountInformation({ account, initialPassword }: { account: AccountLogin; initialPassword?: string }) {
  const [visible, setVisible] = useState(false)
  const [feedback, setFeedback] = useState('')
  async function copy() {
    setFeedback('')
    try {
      await navigator.clipboard.writeText(accountLoginText(account, initialPassword))
      setFeedback(initialPassword === undefined ? '登录信息已复制' : '登录信息和初始密码已复制')
    } catch { setFeedback('复制失败，请手动选择下方内容复制。') }
  }
  return <section className="account-information" aria-label="员工登录信息">
    <div className="account-information-heading"><h3>登录信息</h3><button className="text-button" type="button" onClick={() => void copy()}>{initialPassword === undefined ? '复制登录信息' : '复制账号和初始密码'}</button></div>
    {account.enterpriseDomain && <label>公司登录标识<input readOnly value={account.enterpriseDomain} onFocus={event => event.target.select()} /></label>}
    <label>登录账号<input readOnly value={account.loginName} onFocus={event => event.target.select()} /></label>
    {initialPassword !== undefined ? <><label>初始/重置密码<div className="account-password"><input readOnly type={visible ? 'text' : 'password'} autoComplete="off" value={initialPassword} onFocus={event => event.target.select()} /><button className="button secondary" type="button" aria-label={visible ? '隐藏交付密码' : '显示交付密码'} aria-pressed={visible} onClick={() => setVisible(value => !value)}>{visible ? '隐藏' : '显示'}</button></div></label><p>这是登记的初始或重置密码，员工自行改密后以新密码为准。</p></> : <p>暂无交付密码。</p>}
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
    <div className="dialog-body"><p className="account-handoff-intro">请将以下登录信息交给 {account.displayName}，用于员工端登录。</p><AccountInformation account={account} initialPassword={account.initialPassword} /></div>
    <div className="dialog-footer"><button className="button primary" onClick={onClose}>完成</button></div>
  </dialog>
}
