import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { authService } from '../api/config.ts'
import { errorMessage } from '../api/feedback.ts'
import { Icon } from './Icon.tsx'
import { ValidatedForm } from './ValidatedForm.tsx'

export function AuthPage({ brand, message }: { brand: ReactNode; message: string }) {
  const [initializing, setInitializing] = useState(false)
  const [enterpriseDomain, setDomain] = useState('')
  const [loginName, setLoginName] = useState('')
  const [password, setPassword] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [timezone, setTimezone] = useState('Asia/Shanghai')
  const [bootstrapToken, setBootstrapToken] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const replay = useRef({ command: '', key: crypto.randomUUID() })
  return <main className="entry-page"><div className="entry-brand">{brand}</div><div className="entry-content"><section className="entry-intro"><span className="entry-eyebrow">6ix · Pandora</span><h1>让团队管理<br /><span>更清晰。</span></h1><p>在一个工作台里，维护组织、人员<br className="desktop-break" />与公司的重要事项。</p><div className="entry-features"><span><Icon name="organization" />组织结构</span><span><Icon name="people" />人员维护</span><span><Icon name="shield" />角色配置</span></div></section><section className="entry-card auth-card"><span className="auth-eyebrow">ADMIN WORKSPACE</span><h2>{initializing ? '初始化公司' : '管理员登录'}</h2><p>{initializing ? '首次部署由负责人员提供初始化授权码。' : '欢迎回来，登录你的公司工作台。'}</p>
    <ValidatedForm key={initializing ? 'initialize' : 'login'} onSubmit={async event => {
      event.preventDefault(); if (busy) return; setBusy(true); setError(''); setNotice('')
      try {
        if (initializing) {
          const command = { loginDomain: enterpriseDomain.trim(), name: companyName.trim(), businessTimezone: timezone.trim(), adminLoginName: loginName.trim(), adminPassword: password, adminDisplayName: displayName.trim() }
          const fingerprint = JSON.stringify(command)
          if (fingerprint !== replay.current.command) replay.current = { command: fingerprint, key: crypto.randomUUID() }
          await authService.initialize(command, bootstrapToken, replay.current.key)
          setInitializing(false); setBootstrapToken(''); setNotice('公司与管理员已创建，请登录。')
        }
        await authService.login({ enterpriseDomain: enterpriseDomain.trim(), loginName: loginName.trim(), password })
      } catch (reason) { setError(errorMessage(reason)) }
      finally { setBusy(false); setPassword('') }
    }}><fieldset disabled={busy}>
      <label><span>企业登录域 <span className="required">*</span></span><input required maxLength={128} pattern={initializing ? '[a-z0-9][a-z0-9-]{0,127}' : undefined} autoComplete="organization" value={enterpriseDomain} onChange={event => setDomain(event.target.value)} placeholder="例如：pandora-team" /></label>
      {initializing && <><label><span>公司名称 <span className="required">*</span></span><input required maxLength={128} value={companyName} onChange={event => setCompanyName(event.target.value)} /></label><label><span>管理员姓名 <span className="required">*</span></span><input required maxLength={128} autoComplete="name" value={displayName} onChange={event => setDisplayName(event.target.value)} /></label><label><span>业务时区 <span className="required">*</span></span><input required maxLength={64} value={timezone} onChange={event => setTimezone(event.target.value)} placeholder="Asia/Shanghai" /></label></>}
      <label><span>登录名 <span className="required">*</span></span><input required maxLength={128} autoComplete="username" value={loginName} onChange={event => setLoginName(event.target.value)} /></label>
      <label><span>{initializing ? '管理员初始密码' : '密码'} <span className="required">*</span></span><input type="password" required minLength={initializing ? 10 : undefined} maxLength={72} autoComplete={initializing ? 'new-password' : 'current-password'} value={password} onChange={event => setPassword(event.target.value)} /></label>
      {initializing && <label><span>初始化授权码 <span className="required">*</span></span><input type="password" required autoComplete="off" value={bootstrapToken} onChange={event => setBootstrapToken(event.target.value)} /></label>}
      {(notice || message) && <p role="status">{notice || message}</p>}{error && <p role="alert" className="error-message">{error}</p>}
      <button className="button primary entry-button" type="submit">{busy ? '正在处理…' : initializing ? '创建公司并登录' : '登录工作台'}<Icon name="arrow" /></button>
      <button className="text-button" type="button" onClick={() => { setInitializing(!initializing); setError(''); setNotice(''); setPassword(''); setBootstrapToken('') }}>{initializing ? '返回登录' : '首次创建公司'}</button>
    </fieldset></ValidatedForm></section></div><footer>Pandora · 企业工作管理平台</footer></main>
}
