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
  const [bootstrapToken, setBootstrapToken] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const replay = useRef({ command: '', key: crypto.randomUUID() })
  return <main className="entry-page"><div className="entry-brand">{brand}</div><div className="entry-content"><section className="entry-intro"><span className="entry-eyebrow">6ix · Pandora</span><h1>让团队管理<br /><span>更清晰。</span></h1><div className="entry-features"><span><Icon name="organization" />组织结构</span><span><Icon name="people" />人员维护</span><span><Icon name="shield" />角色配置</span></div></section><section className="entry-card auth-card"><span className="auth-eyebrow">ADMIN WORKSPACE</span><h2>{initializing ? '开通公司' : '管理员登录'}</h2><p>{initializing ? '部署人员专用：建立公司及首位管理员，需要平台提供的开通授权。已有公司无需重复开通。' : '欢迎回来，登录你的公司工作台。'}</p>
    <ValidatedForm key={initializing ? 'initialize' : 'login'} onSubmit={async event => {
      event.preventDefault(); if (busy) return; setBusy(true); setError(''); setNotice('')
      try {
        if (initializing) {
          const command = { loginDomain: enterpriseDomain.trim(), name: companyName.trim(), businessTimezone: 'Asia/Shanghai', adminLoginName: loginName.trim(), adminPassword: password, adminDisplayName: displayName.trim() }
          const fingerprint = JSON.stringify(command)
          if (fingerprint !== replay.current.command) replay.current = { command: fingerprint, key: crypto.randomUUID() }
          await authService.initialize(command, bootstrapToken, replay.current.key)
          setInitializing(false); setBootstrapToken(''); setPassword(''); setNotice('公司已开通，请使用首位管理员账号登录。')
          return
        }
        await authService.login({ enterpriseDomain: enterpriseDomain.trim(), loginName: loginName.trim(), password })
        setPassword('')
      } catch (reason) { setError(errorMessage(reason)) }
      finally { setBusy(false) }
    }}><fieldset disabled={busy}>
      <label><span>公司登录标识 <span className="required">*</span></span><input required maxLength={128} pattern={initializing ? '[a-z0-9][a-z0-9-]{0,127}' : undefined} autoComplete="organization" value={enterpriseDomain} onChange={event => setDomain(event.target.value)} placeholder="请输入所属公司的登录标识" /></label>
      {initializing && <><label><span>公司名称 <span className="required">*</span></span><input required maxLength={128} value={companyName} onChange={event => setCompanyName(event.target.value)} placeholder="例如：6ix" /><small>显示在工作台中，以后可以修改；修改名称不会改变登录标识。</small></label><label><span>首位管理员姓名 <span className="required">*</span></span><input required maxLength={128} autoComplete="name" value={displayName} onChange={event => setDisplayName(event.target.value)} /><small>这位管理员的显示姓名，后续可为其他人员配置管理员角色。</small></label></>}
      <label><span>{initializing ? '首位管理员登录名' : '管理员登录名'} <span className="required">*</span></span><input required maxLength={128} autoComplete="username" value={loginName} onChange={event => setLoginName(event.target.value)} placeholder="例如：admin" />{initializing && <small>用于登录的账号名，与上面的显示姓名不同。</small>}</label>
      <label><span>{initializing ? '管理员初始密码' : '密码'} <span className="required">*</span></span><input type="password" required minLength={initializing ? 10 : undefined} maxLength={72} autoComplete={initializing ? 'new-password' : 'current-password'} value={password} onChange={event => setPassword(event.target.value)} /></label>
      {initializing && <label><span>初始化授权码 <span className="required">*</span></span><input type="password" required autoComplete="off" value={bootstrapToken} onChange={event => setBootstrapToken(event.target.value)} /><small>由部署负责人提供，仅用于获准创建公司，不是账号密码。未开放初始化的服务无法使用此入口。</small></label>}
      {(notice || (!initializing && message)) && <p role="status">{notice || message}</p>}{error && <p role="alert" className="error-message">{error}</p>}
      <button className="button primary entry-button" type="submit">{busy ? '正在处理…' : initializing ? '开通公司及首位管理员' : '登录工作台'}<Icon name="arrow" /></button>
      {initializing ? <button className="text-button" type="button" onClick={() => { setInitializing(false); setError(''); setNotice(''); setPassword(''); setBootstrapToken('') }}>返回管理员登录</button> : <details className="company-onboarding"><summary>公司还未开通？</summary><p>请联系平台管理员开通公司，获取公司登录标识和首位管理员账号。已有公司的其他成员由公司管理员添加。</p><button className="text-button" type="button" onClick={() => { setInitializing(true); setError(''); setNotice(''); setPassword(''); setBootstrapToken('') }}>部署人员入口</button></details>}
    </fieldset></ValidatedForm></section></div><footer>Pandora · 企业工作管理平台</footer></main>
}
