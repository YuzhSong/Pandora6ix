import { useState } from 'react'
import type { ReactNode } from 'react'
import { authService } from '../api/config.ts'
import { errorMessage } from '../api/feedback.ts'
import { Icon } from './Icon.tsx'
import { ValidatedForm } from './ValidatedForm.tsx'

export function AuthPage({ brand, message }: { brand: ReactNode; message: string }) {
  const [enterpriseDomain, setDomain] = useState('')
  const [loginName, setLoginName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  return <main className="entry-page"><div className="entry-brand">{brand}</div><div className="entry-content"><section className="entry-intro"><span className="entry-eyebrow">6ix · Pandora</span><h1>让团队管理<br /><span>更清晰。</span></h1><div className="entry-features"><span><Icon name="organization" />组织结构</span><span><Icon name="people" />人员维护</span><span><Icon name="shield" />角色配置</span></div></section><section className="entry-card auth-card"><span className="auth-eyebrow">ADMIN WORKSPACE</span><h2>管理员登录</h2><p>欢迎回来，登录你的公司工作台。</p>
    <ValidatedForm onSubmit={async event => {
      event.preventDefault(); if (busy) return; setBusy(true); setError('')
      try {
        await authService.login({ enterpriseDomain: enterpriseDomain.trim(), loginName: loginName.trim(), password })
        setPassword('')
      } catch (reason) { setError(errorMessage(reason)) }
      finally { setBusy(false) }
    }}><fieldset disabled={busy}>
      <label><span>公司登录标识 <span className="required">*</span></span><input required maxLength={128} autoComplete="organization" value={enterpriseDomain} onChange={event => setDomain(event.target.value)} placeholder="请输入所属公司的登录标识" /></label>
      <label><span>管理员登录名 <span className="required">*</span></span><input required maxLength={128} autoComplete="username" value={loginName} onChange={event => setLoginName(event.target.value)} placeholder="例如：admin" /></label>
      <label><span>密码 <span className="required">*</span></span><input type="password" required maxLength={72} autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} /></label>
      {message && <p role="status">{message}</p>}{error && <p role="alert" className="error-message">{error}</p>}
      <button className="button primary entry-button" type="submit">{busy ? '正在登录…' : '登录工作台'}<Icon name="arrow" /></button>
      <p className="panel-note">仅供本公司管理员使用。员工账号由管理员新增人员时统一创建，无需自行注册。</p>
    </fieldset></ValidatedForm></section></div><footer>Pandora · 企业工作管理平台</footer></main>
}
