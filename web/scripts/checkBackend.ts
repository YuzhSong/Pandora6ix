import { ApiError } from '../src/api/client.ts'
import { createHealthClient } from '../src/api/health.ts'

// Pass the Web origin to test the proxy, or BACKEND_URL to test a service directly.
// This read-only check sends no login credentials and never writes business data.
const baseUrl = process.argv[2] || process.env.BACKEND_URL || 'http://127.0.0.1:8080'
try {
  const url = new URL(baseUrl)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('请提供无凭证的 HTTP(S) 地址，例如 http://localhost:5173/backend。')
  }
  await createHealthClient({ baseUrl, timeoutMs: 5000 }).check()
  console.log('后端健康检查通过：status=UP。')
  console.log('这只说明服务存活；登录和管理业务接口仍需独立验收。')
} catch (error) {
  if (error instanceof ApiError) {
    console.error(`后端健康检查失败 [${error.code}]：${error.message}`)
    if (error.requestId) console.error(`请求标识：${error.requestId}`)
  } else console.error(error instanceof Error ? error.message : '后端检查失败。')
  process.exitCode = 1
}
