import { useEffect, useRef, useState } from 'react'
import type { CredentialHandoff } from '../data/types.ts'
import { adminRepository } from '../data/repository.ts'
import { prepareCredentialDelivery } from '../data/credentialDelivery.ts'
import { credentialListText } from '../data/accountHandoff.ts'
import { errorMessage } from '../api/feedback.ts'
import { downloadBytes } from './downloadText.ts'

export function CredentialDeliveryActions({ personIds, disabled = false, reload, copyLabel = '复制所选', downloadLabel = '导出所选 Excel', onBusyChange }: {
  personIds: readonly string[]; disabled?: boolean; reload?: () => Promise<CredentialHandoff[]>; copyLabel?: string; downloadLabel?: string; onBusyChange?: (busy: boolean) => void
}) {
  const [feedback, setFeedback] = useState(''), [busy, setBusy] = useState(false)
  const mounted = useRef(true), pending = useRef(false)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  async function deliver(download: boolean) {
    if (pending.current || disabled || !personIds.length) return
    pending.current = true; setBusy(true); setFeedback(''); onBusyChange?.(true)
    try {
      const accounts = await prepareCredentialDelivery(reload ?? (() => adminRepository.credentialHandoffs()), [...personIds])
      if (!mounted.current) return
      if (download) {
        const { credentialWorkbook } = await import('../data/credentialWorkbook.ts')
        const bytes = await credentialWorkbook(accounts)
        if (!mounted.current) return
        downloadBytes('员工账号交付清单.xlsx', bytes, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
      } else await navigator.clipboard.writeText(credentialListText(accounts))
      if (mounted.current) setFeedback(download ? `已导出 ${accounts.length} 人` : `已复制 ${accounts.length} 人`)
    } catch (reason) { if (mounted.current) setFeedback(errorMessage(reason)) }
    finally { pending.current = false; if (mounted.current) { setBusy(false); onBusyChange?.(false) } }
  }
  return <div className="credential-delivery-actions"><div><button className="button secondary" type="button" disabled={disabled || busy || !personIds.length} onClick={() => void deliver(false)}>{copyLabel}</button><button className="button primary" type="button" disabled={disabled || busy || !personIds.length} onClick={() => void deliver(true)}>{busy ? '正在处理…' : downloadLabel}</button></div>{feedback && <p role="status">{feedback}</p>}</div>
}
