import type { StorageAdapter } from './adminRepository.ts'

export type Theme = 'light' | 'dark' | 'system'
export type FontSize = 'small' | 'standard' | 'large'
export interface DisplayPreferences { theme: Theme; fontSize: FontSize }
export const defaultPreferences: DisplayPreferences = { theme: 'light', fontSize: 'standard' }

export function preferenceKey(userId: string) { return `pandora-display:${userId}` }
function valid(value: DisplayPreferences) {
  return value && ['light', 'dark', 'system'].includes(value.theme) && ['small', 'standard', 'large'].includes(value.fontSize)
}
export function createPreferenceStore(storage: StorageAdapter) {
  return {
    load(userId: string): DisplayPreferences {
      try {
        const raw = storage.getItem(preferenceKey(userId))
        if (!raw) return { ...defaultPreferences }
        const value = JSON.parse(raw) as DisplayPreferences
        if (!valid(value)) throw new Error('Invalid preferences')
        return { theme: value.theme, fontSize: value.fontSize }
      } catch { throw new Error('无法读取显示设置，请检查浏览器存储权限。') }
    },
    save(userId: string, value: DisplayPreferences): DisplayPreferences {
      if (!valid(value)) throw new Error('请选择有效的主题与字号。')
      const next = { theme: value.theme, fontSize: value.fontSize }
      try { storage.setItem(preferenceKey(userId), JSON.stringify(next)) }
      catch { throw new Error('显示设置保存失败，请检查浏览器存储权限或可用空间。') }
      return next
    },
  }
}

export const preferenceStore = createPreferenceStore({
  getItem: key => window.localStorage.getItem(key),
  setItem: (key, value) => window.localStorage.setItem(key, value),
})
