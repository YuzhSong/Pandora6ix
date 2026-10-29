import type { createApiClient } from './client.ts'
import { ApiError } from './client.ts'

const supportedTypes = ['image/png', 'image/jpeg', 'image/webp']
export const avatarDataUrlLimit = 768_000
export function validateAvatarFile(file: Pick<File, 'type' | 'size'>) {
  if (!supportedTypes.includes(file.type)) throw new Error('请选择 PNG、JPEG 或 WebP 图片。')
  if (!file.size || file.size > 5 * 1024 * 1024) throw new Error('头像图片不能为空，且不能超过 5 MB。')
}
export function validateAvatarDataUrl(dataUrl: string) {
  if (!/^data:image\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}$/.test(dataUrl) || dataUrl.length > avatarDataUrlLimit) throw new Error('头像图片格式无效或过大，请重新选择。')
}
export async function prepareAvatar(file: File): Promise<string> {
  validateAvatarFile(file)
  let image: ImageBitmap
  try { image = await createImageBitmap(file) } catch { throw new Error('无法读取该图片，请重新选择。') }
  try {
    if (!image.width || !image.height || image.width * image.height > 100_000_000) throw new Error('图片尺寸过大或无效，请选择较小的图片。')
    const scale = Math.min(1, 512 / Math.max(image.width, image.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(image.width * scale)); canvas.height = Math.max(1, Math.round(image.height * scale))
    const context = canvas.getContext('2d')
    if (!context) throw new Error('当前浏览器无法处理头像图片。')
    context.fillStyle = '#ffffff'; context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    const result = canvas.toDataURL('image/jpeg', 0.85)
    validateAvatarDataUrl(result)
    return result
  } finally { image.close() }
}
export function createAvatarService(client: Pick<ReturnType<typeof createApiClient>, 'request'>) {
  return {
    async load(personId: string, signal?: AbortSignal): Promise<Blob | null> {
      try {
        const blob = await client.request<Blob>(`/users/${encodeURIComponent(personId)}/avatar`, { responseFormat: 'blob', headers: { Accept: supportedTypes.join(', ') }, signal })
        if (!blob || !supportedTypes.includes(blob.type) || blob.size > 768_000) throw new Error('头像返回格式不正确。')
        return blob
      } catch (reason) { if (reason instanceof ApiError && reason.status === 404) return null; throw reason }
    },
    async save(personId: string, dataUrl: string) {
      validateAvatarDataUrl(dataUrl)
      await client.request(`/users/${encodeURIComponent(personId)}/avatar`, { method: 'PUT', body: { dataUrl } })
    },
  }
}
