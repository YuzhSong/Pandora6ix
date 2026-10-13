import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { createBackendProxy } from './dev/backendProxy.ts'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = createBackendProxy(process.env.BACKEND_PROXY_TARGET || env.BACKEND_PROXY_TARGET || 'http://127.0.0.1:8080')
  return { plugins: [react()], server: { proxy } }
})
