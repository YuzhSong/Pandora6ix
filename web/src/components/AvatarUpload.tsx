import { useEffect, useRef, useState } from 'react'
import { Icon } from './Icon.tsx'
import { createAvatarService, prepareAvatar } from '../api/avatar.ts'
import { apiClient } from '../api/config.ts'
import { errorMessage } from '../api/feedback.ts'
const avatarService = createAvatarService(apiClient)

export function AvatarImage({ personId, name, revision = 0 }: { personId: string; name: string; revision?: number }) {
  const [image, setImage] = useState<{ personId: string; revision: number; url: string } | null>(null)
  useEffect(() => {
    const controller = new AbortController()
    let url: string | undefined
    void avatarService.load(personId, controller.signal).then(blob => {
      if (controller.signal.aborted) return
      if (blob) { url = URL.createObjectURL(blob); setImage({ personId, revision, url }) }
      else setImage(null)
    }).catch(() => { if (!controller.signal.aborted) setImage(null) })
    return () => { controller.abort(); if (url) URL.revokeObjectURL(url) }
  }, [personId, revision])
  return image && image.personId === personId && image.revision === revision ? <img className="saved-avatar" src={image.url} alt="" onError={() => setImage(null)} /> : <>{name.slice(0, 1)}</>
}

export function AvatarDialog({ personId, onClose, onSaved }: { personId: string; onClose: () => void; onSaved: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null), input = useRef<HTMLInputElement>(null), sequence = useRef(0), submitting = useRef(false)
  const [preview, setPreview] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [processing, setProcessing] = useState(false)
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null, element = dialog.current, selection = sequence
    element?.showModal()
    return () => { selection.current++; element?.close(); opener?.focus() }
  }, [])
  async function select(file: File) {
    const serial = ++sequence.current
    setError(''); setPreview(''); setProcessing(true)
    try { const result = await prepareAvatar(file); if (serial === sequence.current) setPreview(result) }
    catch (reason) { if (serial === sequence.current) { setError(errorMessage(reason)); if (input.current) input.current.value = '' } }
    finally { if (serial === sequence.current) setProcessing(false) }
  }
  async function save() {
    if (!preview || submitting.current || processing) return
    submitting.current = true; setBusy(true); setError('')
    try { await avatarService.save(personId, preview); onSaved() }
    catch (reason) { setError(errorMessage(reason)) }
    finally { submitting.current = false; setBusy(false) }
  }
  return <dialog ref={dialog} className="editor-dialog" aria-labelledby="avatar-title" onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="dialog-head"><h2 id="avatar-title">修改头像</h2><button className="icon-button" disabled={busy} aria-label="关闭头像修改" onClick={onClose}><Icon name="close" /></button></div>
    <div className="dialog-body"><section className="avatar-upload"><label>选择头像<input ref={input} type="file" accept="image/png,image/jpeg,image/webp" disabled={busy || processing} onChange={event => { const file = event.target.files?.[0]; if (file) void select(file) }} /><small>PNG、JPEG 或 WebP，最大 5 MB。</small></label>
      {processing && <p role="status">正在处理图片…</p>}{preview && <div className="avatar-upload-preview"><img src={preview} alt="新头像预览" /></div>}{error && <p className="error-message" role="alert">{error}</p>}
    </section></div>
    <div className="dialog-footer"><button className="button secondary" disabled={busy} onClick={onClose}>取消</button><button className="button primary" disabled={!preview || busy || processing} onClick={() => void save()}>{busy ? '正在保存…' : '保存头像'}</button></div>
  </dialog>
}
