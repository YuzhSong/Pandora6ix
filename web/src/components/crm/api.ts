import { useEffect, useState } from 'react'
import { apiClient } from '../../api/config.ts'
import { errorMessage } from '../../api/feedback.ts'

export type Person = { id: string; displayName: string }
export type Row = { id: string; version: number }
export type Labels = Record<string, string>
export const customerKinds = { PERSON: '个人', COMPANY: '企业' }
export const businessKinds = { MALL: '线上商城', PERFORMANCE: '演出与票务', PRIVATE_DINING: '私宴', VISIT: '企业参访', SALON: '沙龙', OTHER: '其他' }
export const businessStatuses = { PLANNED: '计划中', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }

export function crmPath(path: string, params: Record<string, string | number> = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) if (value !== '') query.set(key, String(value))
  return '/crm/' + path + (query.size ? '?' + query : '')
}

export function useCrmQuery<T>(path: string | null, refresh: unknown = 0) {
  const [result, setResult] = useState<{ path: string; refresh: unknown; data?: T; error?: string }>()
  useEffect(() => {
    if (!path) return
    const controller = new AbortController()
    apiClient.request<T>(path, { signal: controller.signal }).then(
      data => { if (!controller.signal.aborted) setResult({ path, refresh, data }) },
      error => { if (!controller.signal.aborted) setResult({ path, refresh, error: errorMessage(error) }) },
    )
    return () => controller.abort()
  }, [path, refresh])
  return result?.path === path && result.refresh === refresh ? result : { data: undefined, error: undefined }
}
