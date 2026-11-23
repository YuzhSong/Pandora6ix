import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { createBackendProxy } from './dev/backendProxy.ts'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = createBackendProxy(
    mode === 'dev2' ? 'http://127.0.0.1:19082' : process.env.BACKEND_PROXY_TARGET || env.BACKEND_PROXY_TARGET || 'http://127.0.0.1:8080',
    mode === 'dev2' ? '/health' : process.env.BACKEND_HEALTH_PATH || env.BACKEND_HEALTH_PATH || '/health',
  )
  return { plugins: [react()], optimizeDeps: { include: ['exceljs'] }, server: { proxy, ...(mode === 'dev2' ? { host: '127.0.0.1', port: 15182, strictPort: true } : {}) } }
})
