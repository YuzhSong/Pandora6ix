import { useEffect, useRef, useState } from 'react'
import { authService } from '../api/config.ts'
import { errorMessage } from '../api/feedback.ts'
import { ValidatedForm } from './ValidatedForm.tsx'

export function PasswordDialog({ onClose, onChanged }: { onClose: () => void; onChanged: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [currentPassword, setCurrent] = useState('')
  const [newPassword, setNew] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null; dialog.current?.showModal(); return () => opener?.focus() }, [])
  return <dialog ref={dialog} className="editor-dialog" aria-labelledby="password-title" onCancel={event => { event.preventDefault(); if (!busy) onClose() }}><ValidatedForm onSubmit={async event => {
    event.preventDefault(); if (busy) return; setError('')
    if (newPassword !== confirm) { setError('两次输入的新密码不一致。'); return }
    setBusy(true)
    try { await authService.changePassword(currentPassword, newPassword); onChanged() }
    catch (reason) { setError(errorMessage(reason)) }
    finally { setBusy(false) }
  }}><div className="dialog-head"><h2 id="password-title">修改密码</h2><button type="button" className="icon-button" aria-label="关闭修改密码" disabled={busy} onClick={onClose}>×</button></div><div className="dialog-body"><fieldset disabled={busy}>
    <label>当前密码<input autoFocus type="password" required autoComplete="current-password" value={currentPassword} onChange={event => setCurrent(event.target.value)} /></label>
    <label>新密码<input type="password" required minLength={10} maxLength={72} autoComplete="new-password" value={newPassword} onChange={event => setNew(event.target.value)} /></label>
    <label>确认新密码<input type="password" required minLength={10} maxLength={72} autoComplete="new-password" value={confirm} onChange={event => setConfirm(event.target.value)} /></label><p>修改成功后，请使用新密码重新登录。</p>{error && <p className="error-message" role="alert">{error}</p>}
  </fieldset></div><div className="dialog-footer"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>取消</button><button className="button primary" disabled={busy} type="submit">{busy ? '正在修改…' : '确认修改'}</button></div></ValidatedForm></dialog>
}
