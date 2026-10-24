import { useEffect, useRef, useState } from 'react'
import { Icon } from './Icon.tsx'

export function AvatarDialog({ onClose }: { onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    dialog.current?.showModal()
    return () => opener?.focus()
  }, [])
  return <dialog ref={dialog} className="editor-dialog" aria-labelledby="avatar-title" onCancel={event => { event.preventDefault(); onClose() }}>
    <div className="dialog-head"><h2 id="avatar-title">头像预览</h2><button className="icon-button" aria-label="关闭头像修改" onClick={onClose}><Icon name="close" /></button></div>
    <div className="dialog-body"><AvatarUpload /></div>
    <div className="dialog-footer"><button className="button secondary" onClick={onClose}>关闭</button></div>
  </dialog>
}

export function AvatarUpload() {
  const input = useRef<HTMLInputElement>(null)
  const [preview, setPreview] = useState('')
  const [error, setError] = useState('')
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])
  return <section className="avatar-upload">
    <label>选择头像<input ref={input} type="file" accept="image/png,image/jpeg,image/webp" onChange={event => {
      const file = event.target.files?.[0]
      if (!file) return
      setError(''); setPreview('')
      if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) { setError('请选择 PNG、JPEG 或 WebP 图片。'); event.target.value = ''; return }
      if (file.size > 5 * 1024 * 1024) { setError('头像图片不能超过 5 MB。'); event.target.value = ''; return }
      setPreview(URL.createObjectURL(file))
    }} /><small>PNG、JPEG 或 WebP，最大 5 MB。</small></label>
    {preview && <div className="avatar-upload-preview"><img src={preview} alt="新头像预览（尚未保存）" onError={() => { setPreview(''); setError('无法读取该图片，请重新选择。'); if (input.current) input.current.value = '' }} /><button className="text-button" type="button" onClick={() => { setPreview(''); setError(''); if (input.current) input.current.value = '' }}>取消预览</button></div>}
    {error && <p className="error-message" role="alert">{error}</p>}
    <button type="button" className="button secondary" disabled>保存头像（暂不可用）</button>
  </section>
}
