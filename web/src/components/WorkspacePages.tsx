import { useEffect, useRef, useState } from 'react'
import type { AdminData, CompanyImportantItem } from '../data/types.ts'
import { roleLabels } from '../data/types.ts'
import type { DisplayPreferences } from '../data/preferences.ts'
import type { EditRequest } from './AdminEditor.tsx'
import { Icon } from './Icon.tsx'
import { adminRepository } from '../data/repository.ts'
import { errorMessage } from '../api/feedback.ts'
import { ItemOrderSaveError, moveCompanyItem, saveCompanyItemOrder, sortCompanyItems } from '../data/companyItemOrder.ts'

export function CompanyItemsPage({ data, onEdit, onUpdated, onPendingChange }: { data: AdminData; onEdit: (request: EditRequest) => void; onUpdated: (data: AdminData) => void; onPendingChange: (pending: boolean) => void }) {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const [detail, setDetail] = useState<CompanyImportantItem | null>(null)
  const [draft, setDraft] = useState<CompanyImportantItem[] | null>(null)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [needsReload, setNeedsReload] = useState(false)
  const [dragging, setDragging] = useState<string | null>(null)
  const filtered = !!query.trim()
  const ordered = draft ?? sortCompanyItems(data.companyItems)
  const items = ordered.filter(item => `${item.title} ${item.content}`.toLowerCase().includes(query.trim().toLowerCase()))
  const pageCount = Math.max(1, Math.ceil(items.length / 10))
  const currentPage = Math.min(page, pageCount)
  const visibleItems = items.slice((currentPage - 1) * 10, currentPage * 10)
  const canMove = !filtered && !saving && !needsReload
  useEffect(() => { onPendingChange(!!draft || saving || needsReload) }, [draft, saving, needsReload, onPendingChange])
  useEffect(() => () => onPendingChange(false), [onPendingChange])
  useEffect(() => {
    if (!draft) return
    const protect = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', protect)
    return () => window.removeEventListener('beforeunload', protect)
  }, [draft])
  function move(id: string, target: number) {
    if (!canMove || ordered[target]?.id === id) return
    setDraft(moveCompanyItem(ordered, id, target)); setMessage('顺序已调整，点击“保存排序”后生效。'); setError('')
    setPage(Math.floor(target / 10) + 1)
  }
  async function reload() {
    setSaving(true)
    try { onUpdated(await adminRepository.load()); setDraft(null); setNeedsReload(false) }
    catch (reason) { setError(`无法确认服务器顺序：${errorMessage(reason)} 请重试刷新，确认后才能继续编辑。`); setNeedsReload(true) }
    finally { setSaving(false) }
  }
  async function saveOrder() {
    if (!draft || saving || needsReload) return
    setSaving(true); setMessage(''); setError('')
    try {
      const next = await saveCompanyItemOrder(adminRepository, draft)
      if (next) onUpdated(next)
      setDraft(null); setMessage('排序已保存，第一项为顺序 1。')
    } catch (reason) {
      const completed = reason instanceof ItemOrderSaveError ? reason.completed : 0
      const cause = reason instanceof ItemOrderSaveError ? reason.cause : reason
      setError(`排序未全部保存${completed ? `，已有 ${completed} 项保存成功` : '，部分请求可能已被服务器处理'}。${errorMessage(cause)} 正在重新读取实际顺序。`)
      setNeedsReload(true)
      await reload()
    } finally { setSaving(false) }
  }
  return <section className="panel" aria-label="公司重要事项">
    <div className="panel-heading"><h2>公司重要事项<span className="count-badge">{data.companyItems.length}</span></h2><button className="button primary" disabled={!!draft || saving || needsReload} onClick={() => onEdit({ kind: 'company' })}><Icon name="plus" size={18} />新增事项</button></div>
    <div className="filters"><label className="search-box"><Icon name="search" size={18} /><input aria-label="搜索重要事项" placeholder="搜索标题或内容" disabled={!!draft || saving || needsReload} value={query} onChange={event => { setQuery(event.target.value); setPage(1) }} />{query && <button className="icon-button" aria-label="清空事项搜索" onClick={() => { setQuery(''); setPage(1) }}><Icon name="close" size={15} /></button>}</label><span className="result-count">共 {items.length} 条 · {filtered ? '清空搜索后可调整顺序' : '拖动卡片或使用移动按钮调整顺序'}</span></div>
    {draft && <div className="order-toolbar"><span>待保存排序 · 最上方为顺序 1</span><button className="button primary" disabled={saving || needsReload} onClick={() => void saveOrder()}>{saving ? '正在保存…' : '保存排序'}</button><button className="button secondary" disabled={saving || needsReload} onClick={() => { setDraft(null); setMessage(''); setError('') }}>取消排序</button></div>}
    {message && <p className="panel-note" role="status">{message}</p>}{error && <p className="error-message" role="alert">{error}</p>}{needsReload && <button className="button secondary" disabled={saving} onClick={() => void reload()}>重新读取实际顺序</button>}
    <div className="company-list">{visibleItems.map(item => {
      const index = ordered.findIndex(value => value.id === item.id)
      return <article className={`company-card${dragging === item.id ? ' is-dragging' : ''}`} key={item.id} draggable={canMove} onDragStart={event => { if (!canMove) { event.preventDefault(); return }; event.dataTransfer.effectAllowed = 'move'; event.dataTransfer.setData('text/plain', item.id); setDragging(item.id) }} onDragEnd={() => setDragging(null)} onDragOver={event => { if (canMove && dragging && dragging !== item.id) { event.preventDefault(); event.dataTransfer.dropEffect = 'move' } }} onDrop={event => { event.preventDefault(); if (dragging && canMove) move(dragging, index); setDragging(null) }}>
        <div className="company-meta"><span className="badge blue">公司事项</span><span>顺序 {draft ? index + 1 : item.displayOrder}</span>{canMove && <span className="card-drag-hint" aria-hidden="true">↕ 拖动排序</span>}</div>
        <h3><button className="item-title" onClick={() => setDetail(item)}>{item.title}</button></h3><p className="content-preview">{item.content || '暂无补充说明'}</p>
        <div className="company-order-actions"><button type="button" className="text-button" disabled={!canMove || index === 0} aria-label={`置顶事项：${item.title}`} onClick={() => move(item.id, 0)}>置顶</button><button type="button" className="text-button" disabled={!canMove || index === 0} aria-label={`上移事项：${item.title}`} onClick={() => move(item.id, index - 1)}>上移</button><button type="button" className="text-button" disabled={!canMove || index === ordered.length - 1} aria-label={`下移事项：${item.title}`} onClick={() => move(item.id, index + 1)}>下移</button></div>
        <div className="company-actions"><span>发布人：{data.people.find(person => person.id === item.publisherUserId)?.displayName}</span><div><button className="text-button" aria-label={`查看事项：${item.title}`} onClick={() => setDetail(item)}>查看详情</button><button className="text-button" disabled={!!draft || saving || needsReload} aria-label={`编辑事项：${item.title}`} onClick={() => onEdit({ kind: 'company', item })}>编辑</button></div></div>
      </article>
    })}</div>
    {!items.length && <div className="empty-state"><Icon name="layers" size={30} /><h3>{filtered ? '没有找到符合条件的事项' : '暂无公司重要事项'}</h3>{filtered && <button className="button secondary" onClick={() => { setQuery(''); setPage(1) }}>清空搜索</button>}</div>}
    {items.length > 0 && <div className="table-footer"><span>共 {items.length} 条，显示 {(currentPage - 1) * 10 + 1}–{Math.min(currentPage * 10, items.length)} 条</span><div className="pagination"><button aria-label="上一页事项" disabled={saving || currentPage <= 1} onClick={() => setPage(currentPage - 1)}>‹</button><span>{currentPage} / {pageCount}</span><button aria-label="下一页事项" disabled={saving || currentPage >= pageCount} onClick={() => setPage(currentPage + 1)}>›</button></div></div>}
    {detail && <ItemDetail item={detail} onClose={() => setDetail(null)} />}
  </section>
}

function ItemDetail({ item, onClose }: { item: CompanyImportantItem; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null; dialog.current?.showModal(); return () => opener?.focus() }, [])
  return <dialog ref={dialog} className="editor-dialog" aria-labelledby="item-detail-title" onCancel={event => { event.preventDefault(); onClose() }}><div className="dialog-head"><div><span className="eyebrow">公司重要事项</span><h2 id="item-detail-title">{item.title}</h2></div><button className="icon-button" aria-label="关闭事项详情" onClick={onClose}><Icon name="close" /></button></div><div className="dialog-body full-content">{item.content || '暂无补充说明'}</div><div className="dialog-footer"><button className="button secondary" onClick={onClose}>关闭</button></div></dialog>
}

export function AccountPage({ data, onChangePassword, onChangeAvatar }: { data: AdminData; onChangePassword: () => void; onChangeAvatar: () => void }) {
  const person = data.people.find(item => item.id === data.currentUserId)!
  return <div className="profile-layout"><section className="panel account-card"><div className="panel-heading"><h2>我的账号</h2><Icon name="people" /></div><div className="account-identity"><button type="button" className="avatar avatar-edit-button" aria-label="修改我的头像" title="点击修改头像" onClick={onChangeAvatar}>{person.displayName.slice(0, 1)}</button><div><h3>{person.displayName}</h3><span>{person.loginName}</span></div></div><dl className="info-list"><div><dt>所属企业</dt><dd>{data.enterprise.name}</dd></div><div><dt>企业登录域</dt><dd>{data.enterprise.loginDomain}</dd></div><div><dt>所属组织</dt><dd>{data.units.find(unit => unit.id === person.orgUnitId)?.name || '未分配组织'}</dd></div><div><dt>职位</dt><dd>{person.jobTitle || '未设置'}</dd></div><div><dt>直属上级</dt><dd>{data.people.find(item => item.id === person.supervisorId)?.displayName || '未设置'}</dd></div><div><dt>业务角色</dt><dd>{person.roles.map(role => roleLabels[role]).join('、')}</dd></div></dl><div className="password-section"><h3>账号安全</h3><p>修改成功后，当前会话会失效，请重新登录。</p><button className="button secondary" onClick={onChangePassword}>修改密码</button></div></section></div>
}

export function SettingsPage({ preferences, error, onChange }: { preferences: DisplayPreferences; error: string; onChange: (preferences: DisplayPreferences) => void }) {
  return <section className="panel account-card"><div className="panel-heading"><div><h2>显示设置</h2></div><Icon name="layers" /></div><fieldset className="preference-picker"><legend>界面主题</legend>{([['light', '浅色'], ['dark', '深色'], ['system', '跟随系统']] as const).map(([value, label]) => <label key={value}><input type="radio" name="theme" value={value} checked={preferences.theme === value} onChange={() => onChange({ ...preferences, theme: value })} />{label}</label>)}</fieldset><fieldset className="preference-picker"><legend>文字大小</legend>{([['small', '较小'], ['standard', '标准'], ['large', '较大']] as const).map(([value, label]) => <label key={value}><input type="radio" name="fontSize" value={value} checked={preferences.fontSize === value} onChange={() => onChange({ ...preferences, fontSize: value })} />{label}</label>)}</fieldset>{error && <p className="error-message" role="alert">{error}</p>}</section>
}
