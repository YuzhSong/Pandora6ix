import { useCallback, useEffect, useRef, useState } from 'react'
import { adminRepository } from '../data/repository.ts'
import type { CredentialHandoff } from '../data/types.ts'
import { errorMessage } from '../api/feedback.ts'

export const handoffLabels: Record<CredentialHandoff['status'], string> = { AVAILABLE: '可导出', CHANGED: '已改密', EXPIRED: '已过期', UNAVAILABLE: '无交付记录' }

export function useCredentialHandoffs(userId?: string, enabled = true) {
  const [items, setItems] = useState<CredentialHandoff[]>([])
  const [error, setError] = useState(''), [loading, setLoading] = useState(enabled)
  const serial = useRef(0), active = useRef(false)
  const read = useCallback(async () => {
    const request = ++serial.current
    if (!active.current) throw new Error('窗口已关闭。')
    try {
      const values = await adminRepository.credentialHandoffs(userId)
      if (!active.current || request !== serial.current) throw new Error('请求已取消。')
      setItems(values); setError(''); return values
    } catch (reason) {
      if (active.current && request === serial.current) { setItems([]); setError(errorMessage(reason)) }
      throw reason
    } finally { if (active.current && request === serial.current) setLoading(false) }
  }, [userId])
  const refresh = useCallback(() => { setLoading(true); return read() }, [read])
  useEffect(() => {
    if (!enabled) return
    active.current = true
    const update = () => { if (!document.hidden) void read().catch(() => undefined) }
    void read().catch(() => undefined)
    const timer = window.setInterval(update, 5000)
    window.addEventListener('focus', update); document.addEventListener('visibilitychange', update)
    return () => { active.current = false; window.clearInterval(timer); window.removeEventListener('focus', update); document.removeEventListener('visibilitychange', update) }
  }, [enabled, read])
  return { items, error, loading, refresh }
}
