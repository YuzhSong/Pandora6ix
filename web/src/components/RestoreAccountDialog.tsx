import { useEffect, useRef, useState } from 'react'
import type { AdminData, Person } from '../data/types.ts'
import { adminRepository } from '../data/repository.ts'
import { canRestoreAccount } from '../data/personnelDisplay.ts'
import { errorMessage } from '../api/feedback.ts'

export function RestoreAccountDialog({ person, onClose, onSaved }: { person: Person; onClose: () => void; onSaved: (data: AdminData) => void }) {
  const dialog = useRef<HTMLDialogElement>(null), pending = useRef(false)
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  useEffect(() => { const opener = document.activeElement as HTMLElement | null, element = dialog.current; element?.showModal(); return () => { element?.close(); opener?.focus() } }, [])
  async function restore() {
    if (pending.current || error || !canRestoreAccount(person)) return
    pending.current = true; setBusy(true)
    try { onSaved(await adminRepository.restoreAccount(person.id, person.version)) }
    catch (reason) { setError(`${errorMessage(reason)} 请关闭并刷新资料后核对。`) }
    finally { pending.current = false; setBusy(false) }
  }
  return <dialog ref={dialog} className="editor-dialog" aria-label="恢复账号" onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="dialog-head"><h2>恢复账号 · {person.displayName}</h2></div>
    <div className="dialog-body"><p>确认恢复此在职人员的登录权限？恢复后沿用原密码和原有角色权限。</p>{error && <p className="error-message" role="alert">{error}</p>}</div>
    <div className="dialog-footer"><button className="button secondary" disabled={busy} onClick={onClose}>取消</button><button className="button primary" disabled={busy || !!error || !canRestoreAccount(person)} onClick={() => void restore()}>{busy ? '正在恢复…' : '确认恢复账号'}</button></div>
  </dialog>
}
