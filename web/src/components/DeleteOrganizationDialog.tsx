import { useEffect, useRef, useState } from 'react'
import { adminRepository } from '../data/repository.ts'
import type { AdminData, OrgUnit } from '../data/types.ts'
import { unitLabels } from '../data/types.ts'
import { ApiError } from '../api/client.ts'
import { errorMessage } from '../api/feedback.ts'

export function DeleteOrganizationDialog({ unit, onClose, onDeleted }: { unit: OrgUnit; onClose: () => void; onDeleted: (data: AdminData) => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const submitting = useRef(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [refreshRequired, setRefreshRequired] = useState(false)
  useEffect(() => {
    const element = dialog.current, opener = document.activeElement as HTMLElement | null
    element?.showModal()
    return () => { element?.close(); opener?.focus() }
  }, [])
  async function remove() {
    if (submitting.current || refreshRequired) return
    submitting.current = true; setBusy(true); setError('')
    try { onDeleted(await adminRepository.deleteUnit(unit.id, unit.version)) }
    catch (reason) {
      const unavailable = reason instanceof ApiError && reason.status === 405
      const unloaded = reason instanceof ApiError && reason.code === 'WORKSPACE_NOT_LOADED'
      setError(unavailable ? '当前服务器尚未部署组织删除接口，请联系后端更新。' : unloaded ? errorMessage(reason) : `${errorMessage(reason)} 请取消窗口并刷新资料，确认最新状态后再操作。`)
      setRefreshRequired(true)
    } finally { submitting.current = false; setBusy(false) }
  }
  return <dialog ref={dialog} className="confirm-dialog organization-delete-dialog" aria-labelledby="delete-organization-title" onCancel={event => { event.preventDefault(); if (!submitting.current) onClose() }}><div className="dialog-body"><h2 id="delete-organization-title">删除{unitLabels[unit.kind]}</h2><p>确定删除「<strong>{unit.name}</strong>」吗？</p><p>删除后无法恢复。仅允许删除没有下级组织、没有直属人员的部门或团队。</p>{error && <p className="error-message" role="alert">{error}</p>}</div><div className="dialog-footer"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>取消</button><button type="button" className="button danger" disabled={busy || refreshRequired} onClick={() => void remove()}>{busy ? '正在删除…' : '确认删除'}</button></div></dialog>
}
