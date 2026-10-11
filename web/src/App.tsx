import { useEffect, useRef, useState } from 'react'
import { adminRepository, BINDING_KEY, CATALOG_KEY, DATA_KEY } from './data/adminRepository.ts'
import type { AdminData } from './data/types.ts'
import { AdminEditor } from './components/AdminEditor.tsx'
import type { EditRequest } from './components/AdminEditor.tsx'
import { OrganizationPage, PeoplePage, TagsPage } from './components/AdminPages.tsx'
import { Icon } from './components/Icon.tsx'
import type { IconName } from './components/Icon.tsx'
import { CompanyItemsPage, AccountPage, SettingsPage } from './components/WorkspacePages.tsx'
import { defaultPreferences, preferenceStore } from './data/preferences.ts'
import type { DisplayPreferences } from './data/preferences.ts'
import './App.css'
import './theme.css'

const navigation: { id: string; label: string; icon: IconName }[] = [
  { id: 'organizations', label: '组织管理', icon: 'organization' },
  { id: 'people', label: '人员与角色', icon: 'people' },
  { id: 'tags', label: '标签管理', icon: 'tags' },
  { id: 'company-items', label: '公司重要事项', icon: 'layers' },
]
const auxiliaryPages = [{ id: 'account', label: '我的账号' }, { id: 'settings', label: '设置' }]
const SESSION_KEY = 'pandora-admin-demo-session'
function route() { const hash = location.hash.slice(1); return hash === 'enterprise' ? 'organizations' : hash === 'roles' ? 'people' : [...navigation, ...auxiliaryPages].some(item => item.id === hash) ? hash : 'people' }
function Brand() { return <div className="brand"><span className="brand-mark">P<span /></span><div>Pandora<span>企业工作管理平台</span></div></div> }

export default function App() {
  const [active, setActive] = useState(route)
  const navigationRef = useRef<HTMLElement>(null)
  const [session, setSession] = useState(() => { try { return sessionStorage.getItem(SESSION_KEY) === 'active' } catch { return false } })
  const [data, setData] = useState<AdminData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [externalUpdate, setExternalUpdate] = useState(false)
  useEffect(() => {
    const notify = (event: StorageEvent) => { if (event.storageArea === localStorage && (event.key === DATA_KEY || event.key === CATALOG_KEY || event.key === BINDING_KEY || event.key === null)) setExternalUpdate(true) }
    window.addEventListener('storage', notify)
    return () => window.removeEventListener('storage', notify)
  }, [])
  const [entryError, setEntryError] = useState('')
  const [editor, setEditor] = useState<EditRequest | null>(null)
  const [toast, setToast] = useState('')
  const [logoutConfirm, setLogoutConfirm] = useState(false)
  const [preferences, setPreferences] = useState<DisplayPreferences>(defaultPreferences)
  const [preferenceError, setPreferenceError] = useState('')
  useEffect(() => {
    const nav = navigationRef.current
    const selected = nav?.querySelector<HTMLElement>('[aria-current="page"]')
    if (!nav || !selected) return
    const left = selected.getBoundingClientRect().left - nav.getBoundingClientRect().left + nav.scrollLeft
    nav.scrollTo({ left: Math.max(0, left - (nav.clientWidth - selected.offsetWidth) / 2) })
  }, [active, session, preferences.fontSize])
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
  async function load() {
    setLoading(true); setError('')
    try { setData(await adminRepository.getWorkspace()); setExternalUpdate(false) }
    catch (reason) { setError(reason instanceof Error ? reason.message : '加载失败，请重试。') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])
  useEffect(() => {
    const onHash = () => {
      if (editor) {
        if (route() !== active) { location.replace(`#${active}`); setToast('请先保存或关闭编辑窗口') }
        return
      }
      setActive(route())
    }
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [active, editor])
  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 4000)
    return () => window.clearTimeout(timer)
  }, [toast])
  function enter() {
    try { sessionStorage.setItem(SESSION_KEY, 'active'); setSession(true); setEntryError('') }
    catch { setEntryError('浏览器会话存储不可用，请允许本站点使用存储后重试。') }
  }
  function logout() {
    try { sessionStorage.removeItem(SESSION_KEY); setSession(false); setLogoutConfirm(false); setToast('') }
    catch { setError('无法退出工作台，请检查浏览器存储权限。'); setLogoutConfirm(false) }
  }
  const currentName = data?.people.find(person => person.id === data.currentUserId)?.displayName ?? '管理员'
  const page = [...navigation, ...auxiliaryPages].find(item => item.id === active) ?? navigation[0]
  if (!data && (loading || error)) return <main className="company-setup"><Brand /><section className="panel setup-card">{error ? <><h1>暂时无法加载资料</h1><p className="error-message" role="alert">{error}</p><button className="button primary" onClick={() => void load()}>重新加载</button></> : <p role="status">正在加载管理数据…</p>}</section></main>
  if (!loading && !error && !data) return <CompanySetup onInitialized={next => { setData(next); setToast('公司已初始化') }} />
  if (!session) return <main className="entry-page"><div className="entry-brand"><Brand /></div><div className="entry-content"><section className="entry-intro"><h1>Pandora<br />管理工作台</h1><p>企业人员、组织和基础资料管理。</p><div className="entry-features"><span><Icon name="organization" />组织结构</span><span><Icon name="people" />人员维护</span><span><Icon name="shield" />角色配置</span></div></section><section className="entry-card"><span className="large-icon"><Icon name="shield" size={28} /></span><h2>{data?.enterprise.name}</h2><p>公司管理工作台</p><p className="entry-description">集中维护企业人员、组织、标签与公司重要事项。</p><button className="button primary entry-button" onClick={enter}>进入工作台<Icon name="arrow" /></button>{entryError && <p role="alert" className="error-message">{entryError}</p>}<small><Icon name="shield" size={15} />账号认证服务正在接入中</small></section></div><footer>Pan<span>dora</span> · 企业工作管理平台</footer></main>
  return <div className="admin-layout">
    <header className="workspace-header">
      <div className="workspace-header-row"><Brand /><span className="workspace-enterprise"><Icon name="enterprise" size={16} /><span>{data?.enterprise.name ?? '当前企业'}</span></span><div className="workspace-account"><button className="account-link" aria-label={`${currentName}，我的账号`} onClick={() => { location.hash = 'account' }}><span className="avatar small-avatar">{currentName.slice(0, 1)}</span><span>{currentName}</span></button><button className="icon-button settings-button" aria-label="设置" aria-current={active === 'settings' ? 'page' : undefined} onClick={() => { location.hash = 'settings' }}><Icon name="settings" size={20} /></button><button className="icon-button" aria-label="退出工作台" onClick={() => setLogoutConfirm(true)}><Icon name="logout" size={19} /></button></div></div>
      <nav ref={navigationRef} className="primary-navigation" aria-label="管理端导航">{navigation.map(item => <a key={item.id} href={`#${item.id}`} className={active === item.id ? 'active' : ''} aria-current={active === item.id ? 'page' : undefined}><Icon name={item.icon} size={17} /><span>{item.label}</span></a>)}</nav>
    </header>
    <div className="main-area"><main className="page-content"><div className="page-heading"><div><h1>{page.label}</h1></div><button className="button secondary" onClick={() => void load()} disabled={loading || !!editor}><Icon name="refresh" size={16} />刷新数据</button></div>
      {externalUpdate && <div className="update-notice" role="status"><span>资料已在其他页面更新。{editor ? '请先保存或关闭当前编辑窗口。' : '刷新后查看最新资料。'}</span><button className="text-button" disabled={!!editor || loading} onClick={() => void load()}>刷新资料</button></div>}
      {error && data && <div className="error-message" role="alert">刷新失败：{error} 当前显示上次加载的资料，恢复后才能编辑。</div>}
      {error && !data ? <section className="panel empty-state"><h2>暂时无法加载数据</h2><p role="alert">{error}</p><button className="button primary" onClick={() => void load()}>重新加载</button></section> : !data ? <div className="panel loading-state" role="status">正在加载管理数据…</div> : <fieldset className="workspace-content" disabled={loading || !!error}>
        {active === 'people' && <PeoplePage key={data.enterprise.id} data={data} onEdit={setEditor} />}{active === 'organizations' && <OrganizationPage key={data.enterprise.id} data={data} onEdit={setEditor} />}{active === 'tags' && <TagsPage key={data.enterprise.id} data={data} onEdit={setEditor} />}{active === 'company-items' && <CompanyItemsPage key={data.enterprise.id} data={data} onEdit={setEditor} />}{active === 'account' && <AccountPage data={data} />}{active === 'settings' && <SettingsPage preferences={preferences} error={preferenceError} onChange={changePreferences} />}
      </fieldset>}<footer className="page-footer"><span>Pandora 管理端</span></footer></main></div>
    {editor && data && <AdminEditor request={editor} data={data} onClose={() => setEditor(null)} onSaved={next => { setData(next); setExternalUpdate(false); setEditor(null); setToast('已保存') }} />}
    {toast && <div className="toast" role="status"><Icon name="check" size={18} />{toast}<button className="icon-button" aria-label="关闭保存提示" onClick={() => setToast('')}><Icon name="close" size={16} /></button></div>}
    {logoutConfirm && <LogoutDialog onCancel={() => setLogoutConfirm(false)} onConfirm={logout} />}
  </div>
}

function LogoutDialog({ onCancel, onConfirm }: { onCancel: () => void; onConfirm: () => void }) {
  const [element, setElement] = useState<HTMLDialogElement | null>(null)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null; element?.showModal(); return () => { opener?.focus() } }, [element])
  return <dialog ref={setElement} className="confirm-dialog" aria-labelledby="logout-title" onCancel={event => { event.preventDefault(); onCancel() }}><div className="dialog-body"><span className="large-icon"><Icon name="logout" /></span><h2 id="logout-title">退出工作台？</h2><p>已保存的资料会保留，重新进入后可以继续管理。</p></div><div className="dialog-footer"><button className="button secondary" onClick={onCancel}>取消</button><button className="button primary" onClick={onConfirm}>确认退出</button></div></dialog>
}

function CompanySetup({ onInitialized }: { onInitialized: (data: AdminData) => void }) {
  const [name, setName] = useState('')
  const [administratorName, setAdministratorName] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  return <main className="company-setup"><Brand /><section className="panel setup-card"><h1>初始化公司</h1><form onSubmit={async event => { event.preventDefault(); if (saving) return; setSaving(true); setError(''); try { onInitialized(await adminRepository.initializeEnterprise(name, administratorName)) } catch (reason) { setError(reason instanceof Error ? reason.message : '初始化失败') } finally { setSaving(false) } }}><fieldset disabled={saving}><label><span>公司名称 <span className="required" aria-hidden="true">*</span></span><input required maxLength={128} value={name} onChange={event => setName(event.target.value)} autoComplete="organization" /></label><label><span>管理员姓名 <span className="required" aria-hidden="true">*</span></span><input required maxLength={128} value={administratorName} onChange={event => setAdministratorName(event.target.value)} autoComplete="name" /></label>{error && <p className="error-message" role="alert">{error}</p>}<button className="button primary" type="submit">{saving ? '正在初始化…' : '创建公司工作台'}</button></fieldset></form></section></main>
}
