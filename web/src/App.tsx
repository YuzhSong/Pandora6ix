import { useEffect, useRef, useState } from 'react'
import { adminRepository } from './data/repository.ts'
import { authService } from './api/config.ts'
import { ApiError } from './api/client.ts'
import { errorMessage } from './api/feedback.ts'
import { sessionStore } from './api/session.ts'
import type { AdminData, OrgUnit, Person } from './data/types.ts'
import { DeleteOrganizationDialog } from './components/DeleteOrganizationDialog.tsx'
import { AdminEditor } from './components/AdminEditor.tsx'
import type { EditRequest } from './components/AdminEditor.tsx'
import { OrganizationPage, PeoplePage } from './components/AdminPages.tsx'
import { Icon } from './components/Icon.tsx'
import type { IconName } from './components/Icon.tsx'
import { CompanyItemsPage, AccountPage, SettingsPage } from './components/WorkspacePages.tsx'
import { BulkPersonnelDialog, ExportAccountsDialog, ResetEmployeePasswordDialog } from './components/CredentialTools.tsx'
import { AccountCreatedDialog } from './components/AccountInformation.tsx'
import type { CreatedAccountReceipt } from './data/accountHandoff.ts'
import { AvatarDialog } from './components/AvatarUpload.tsx'
import { AuthPage } from './components/AuthPage.tsx'
import { SystemLogsPage } from './components/SystemLogsPage.tsx'
import { PasswordDialog } from './components/PasswordDialog.tsx'
import { defaultPreferences, preferenceStore } from './data/preferences.ts'
import type { DisplayPreferences } from './data/preferences.ts'
import './App.css'
import './theme.css'

const navigation: { id: string; label: string; icon: IconName }[] = [
  { id: 'organizations', label: '组织管理', icon: 'organization' },
  { id: 'people', label: '人员与角色', icon: 'people' },
  { id: 'company-items', label: '公司重要事项', icon: 'layers' },
  { id: 'system-logs', label: '系统日志', icon: 'journal' },
]
const auxiliaryPages = [{ id: 'account', label: '我的账号' }, { id: 'settings', label: '设置' }]
function route() { const hash = location.hash.slice(1); return hash === 'enterprise' ? 'organizations' : hash === 'roles' ? 'people' : [...navigation, ...auxiliaryPages].some(item => item.id === hash) ? hash : 'organizations' }
function Brand() { return <div className="brand"><img className="brand-mark" src="/pandora-logo.png" alt="" width={44} height={44} /><div>Pandora<span>企业工作管理平台</span></div></div> }
function hasSession() { try { return !!sessionStore.read() } catch { return false } }

export default function App() {
  const [active, setActive] = useState(route)
  const navigationRef = useRef<HTMLElement>(null)
  const loadSerial = useRef(0)
  const [session, setSession] = useState(hasSession)
  const [authMessage, setAuthMessage] = useState('')
  const [data, setData] = useState<AdminData | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [editor, setEditor] = useState<EditRequest | null>(null)
  const [toast, setToast] = useState('')
  const [logoutConfirm, setLogoutConfirm] = useState(false)
  const [logoutBusy, setLogoutBusy] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [passwordDialog, setPasswordDialog] = useState(false)
  const [itemsPending, setItemsPending] = useState(false)
  const [avatarDialog, setAvatarDialog] = useState(false)
  const [accountReceipt, setAccountReceipt] = useState<CreatedAccountReceipt | null>(null)
  const [credentialTool, setCredentialTool] = useState<'bulk' | 'export' | null>(null)
  const [resetPerson, setResetPerson] = useState<Person | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<OrgUnit | null>(null)
  const [selectedOrganizationId, setSelectedOrganizationId] = useState('org-company')
  const [preferences, setPreferences] = useState<DisplayPreferences>(defaultPreferences)
  const [preferenceError, setPreferenceError] = useState('')

  useEffect(() => sessionStore.subscribe(() => {
    const authenticated = hasSession()
    setSession(authenticated)
    if (authenticated) {
      location.replace('#organizations')
      setActive('organizations')
    }
    if (!authenticated) {
      loadSerial.current++
      setData(null); setCredentialTool(null); setResetPerson(null); setAccountReceipt(null); setDeleteTarget(null); setEditor(null); setPasswordDialog(false); setAvatarDialog(false); setLogoutConfirm(false)
      setPreferences(defaultPreferences); setToast(''); setError(''); setLoading(false)
      setAuthMessage('登录状态已失效，请重新登录。')
    }
  }), [])
  async function load() {
    const serial = ++loadSerial.current
    setLoading(true); setError('')
    try {
      const next = await adminRepository.getWorkspace()
      if (serial === loadSerial.current && hasSession()) setData(next)
    } catch (reason) {
      if (serial !== loadSerial.current) return
      if (reason instanceof ApiError && (reason.status === 401 || reason.status === 403)) {
        sessionStore.clear(); setAuthMessage(errorMessage(reason))
      } else setError(errorMessage(reason))
    } finally { if (serial === loadSerial.current) setLoading(false) }
  }
  useEffect(() => { if (session) void load() }, [session])
  useEffect(() => {
    if (!session) return
    const expiresAt = sessionStore.read()?.expiresAt
    if (!expiresAt) return
    const timer = window.setTimeout(() => sessionStore.clear(), Math.max(0, Date.parse(expiresAt) - Date.now()))
    return () => window.clearTimeout(timer)
  }, [session])
  useEffect(() => {
    const nav = navigationRef.current, selected = nav?.querySelector<HTMLElement>('[aria-current="page"]')
    if (!nav || !selected) return
    const left = selected.getBoundingClientRect().left - nav.getBoundingClientRect().left + nav.scrollLeft
    nav.scrollTo({ left: Math.max(0, left - (nav.clientWidth - selected.offsetWidth) / 2) })
  }, [active, session, preferences.fontSize, data?.enterprise.id])
  useEffect(() => { window.scrollTo({ top: 0 }) }, [active])
  useEffect(() => {
    if (!data?.currentUserId) return
    try { setPreferences(preferenceStore.load(data.currentUserId)); setPreferenceError('') }
    catch (reason) { setPreferenceError(reason instanceof Error ? reason.message : '无法读取显示设置') }
  }, [data?.currentUserId])
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () => { document.documentElement.dataset.theme = preferences.theme === 'system' ? media.matches ? 'dark' : 'light' : preferences.theme; document.documentElement.dataset.fontSize = preferences.fontSize }
    apply(); media.addEventListener('change', apply)
    return () => media.removeEventListener('change', apply)
  }, [preferences])
  function changePreferences(next: DisplayPreferences) {
    if (!data) return
    try { setPreferences(preferenceStore.save(data.currentUserId, next)); setPreferenceError(''); setToast('显示设置已保存') }
    catch (reason) { setPreferenceError(reason instanceof Error ? reason.message : '显示设置保存失败') }
  }
  useEffect(() => {
    const onHash = () => {
      if (editor || passwordDialog || avatarDialog || itemsPending || !!deleteTarget || !!accountReceipt || !!credentialTool || !!resetPerson) { if (route() !== active) { location.replace(`#${active}`); setToast('请先保存、取消排序或关闭编辑窗口') }; return }
      setActive(route())
    }
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [active, editor, passwordDialog, avatarDialog, itemsPending, deleteTarget, accountReceipt, credentialTool, resetPerson])
  useEffect(() => { if (toast) { const timer = window.setTimeout(() => setToast(''), 4000); return () => window.clearTimeout(timer) } }, [toast])
  async function logout() {
    if (logoutBusy) return
    setLogoutBusy(true); setLogoutError('')
    try { await authService.logout(); setAuthMessage('') }
    catch (reason) { setLogoutError(errorMessage(reason)) }
    finally { setLogoutBusy(false) }
  }
  function saved(next: AdminData, account?: CreatedAccountReceipt) {
    const me = next.people.find(person => person.id === next.currentUserId)
    if (!me?.roles.includes('ADMIN') || me.status === 'DISABLED') {
      void authService.logout().catch(() => sessionStore.clear())
      sessionStore.clear(); setAuthMessage('此账号的管理员权限已变更，请使用管理员账号登录。'); return
    }
    setData(next); setEditor(null); setAccountReceipt(account ?? null); setToast(account ? '' : '已保存到服务器')
  }
  if (!session) return <AuthPage brand={<Brand />} message={authMessage} />
  if (!data) return <main className="company-setup"><Brand /><section className="panel setup-card">{error ? <><h1>暂时无法加载资料</h1><p className="error-message" role="alert">{error}</p><button className="button primary" onClick={() => void load()}>重新加载</button><button className="button secondary" disabled={logoutBusy} onClick={() => void logout()}>退出登录</button>{logoutError && <p role="alert">{logoutError}</p>}</> : <p role="status">正在加载公司资料…</p>}</section></main>
  const currentName = data.people.find(person => person.id === data.currentUserId)?.displayName ?? '管理员'
  const page = [...navigation, ...auxiliaryPages].find(item => item.id === active) ?? navigation[0]
  return <div className="admin-layout">
    <header className="workspace-header"><div className="workspace-header-row"><Brand /><span className="workspace-enterprise"><Icon name="enterprise" size={16} /><span>{data.enterprise.name}</span></span><div className="workspace-account"><div className="account-link"><button type="button" className="avatar small-avatar avatar-edit-button" aria-label="我的账号" title="我的账号" onClick={() => { location.hash = 'account' }}>{currentName.slice(0, 1)}</button><button className="account-name" aria-label={`${currentName}，我的账号`} onClick={() => { location.hash = 'account' }}>{currentName}</button></div><button className="icon-button settings-button" aria-label="设置" aria-current={active === 'settings' ? 'page' : undefined} onClick={() => { location.hash = 'settings' }}><Icon name="settings" size={20} /></button><button className="icon-button" aria-label="退出登录" onClick={() => { setLogoutError(''); setLogoutConfirm(true) }}><Icon name="logout" size={19} /></button></div></div>
      <nav ref={navigationRef} className="primary-navigation" aria-label="管理端导航">{navigation.map(item => <a key={item.id} href={`#${item.id}`} className={active === item.id ? 'active' : ''} aria-current={active === item.id ? 'page' : undefined}><Icon name={item.icon} size={17} /><span>{item.label}</span></a>)}</nav>
    </header>
    <div className="main-area"><main className="page-content"><div className="page-heading"><h1>{page.label}</h1><button className="button secondary" onClick={() => void load()} disabled={loading || !!editor || passwordDialog || avatarDialog || itemsPending || !!deleteTarget || !!accountReceipt || !!credentialTool || !!resetPerson}><Icon name="refresh" size={16} />刷新数据</button></div>
      {error && <div className="error-message" role="alert">刷新失败：{error} 当前显示上次加载的资料，恢复后才能编辑。</div>}
      <fieldset className="workspace-content" disabled={loading || !!error}>
        {active === 'system-logs' && <SystemLogsPage key={`${data.enterprise.id}:${data.currentUserId}`} refreshKey={data} data={data} />}
        {active === 'people' && <PeoplePage key={data.enterprise.id} onUpdated={setData} data={data} onEdit={setEditor} onCredentialTool={setCredentialTool} onResetPassword={setResetPerson} />}{active === 'organizations' && <OrganizationPage key={data.enterprise.id} data={data} onEdit={setEditor} selectedId={selectedOrganizationId} onSelect={setSelectedOrganizationId} onDelete={setDeleteTarget} />}{active === 'company-items' && <CompanyItemsPage key={data.enterprise.id} data={data} onEdit={setEditor} onUpdated={setData} onPendingChange={setItemsPending} />}{active === 'account' && <AccountPage data={data} onChangePassword={() => setPasswordDialog(true)} onChangeAvatar={() => setAvatarDialog(true)} />}{active === 'settings' && <SettingsPage preferences={preferences} error={preferenceError} onChange={changePreferences} />}
      </fieldset><footer className="page-footer"><span>Pandora 管理端</span></footer></main></div>
    {editor && <AdminEditor request={editor} data={data} onClose={() => setEditor(null)} onSaved={saved} />}
    {deleteTarget && <DeleteOrganizationDialog unit={deleteTarget} onClose={() => setDeleteTarget(null)} onDeleted={next => { setData(next); setSelectedOrganizationId(deleteTarget.parentId ?? ''); setDeleteTarget(null); setToast('组织已删除') }} />}
    {credentialTool === 'bulk' && <BulkPersonnelDialog data={data} onClose={() => setCredentialTool(null)} onCreated={next => { setData(next) }} />}
    {credentialTool === 'export' && <ExportAccountsDialog onClose={() => setCredentialTool(null)} />}
    {resetPerson && <ResetEmployeePasswordDialog person={resetPerson} onClose={() => setResetPerson(null)} onReset={(next, account) => { setResetPerson(null); saved(next, account) }} />}
    {accountReceipt && <AccountCreatedDialog account={accountReceipt} onClose={() => setAccountReceipt(null)} />}
    {avatarDialog && <AvatarDialog onClose={() => setAvatarDialog(false)} />}
    {passwordDialog && <PasswordDialog onClose={() => setPasswordDialog(false)} onChanged={() => setAuthMessage('密码已修改，请使用新密码登录。')} />}
    {toast && <div className="toast" role="status"><Icon name="check" size={18} />{toast}<button className="icon-button" aria-label="关闭保存提示" onClick={() => setToast('')}><Icon name="close" size={16} /></button></div>}
    {logoutConfirm && <LogoutDialog busy={logoutBusy} error={logoutError} onCancel={() => setLogoutConfirm(false)} onConfirm={() => void logout()} />}
  </div>
}

function LogoutDialog({ busy, error, onCancel, onConfirm }: { busy: boolean; error: string; onCancel: () => void; onConfirm: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null; dialog.current?.showModal(); return () => opener?.focus() }, [])
  return <dialog ref={dialog} className="confirm-dialog" aria-labelledby="logout-title" onCancel={event => { event.preventDefault(); if (!busy) onCancel() }}><div className="dialog-body"><h2 id="logout-title">退出登录？</h2><p>服务器会撤销当前会话，已保存的资料会保留。</p>{error && <p className="error-message" role="alert">{error}</p>}</div><div className="dialog-footer"><button className="button secondary" disabled={busy} onClick={onCancel}>取消</button><button className="button primary" disabled={busy} onClick={onConfirm}>{busy ? '正在退出…' : '确认退出'}</button></div></dialog>
}
