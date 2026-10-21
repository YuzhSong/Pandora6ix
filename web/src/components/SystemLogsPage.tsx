import { useEffect, useRef, useState } from 'react'
import { apiClient } from '../api/config.ts'
import { createOperationLogReader } from '../api/operationLogs.ts'
import { errorMessage } from '../api/feedback.ts'
import { compareSnapshots, dateRangeValid, emptyLogFilters, logActions, logLabel, logModules, logResults, logSources, logTime } from '../data/operationHistory.ts'
import type { OperationDetail, OperationFilters, OperationPage } from '../data/operationHistory.ts'
import { operationLogWorkbook } from '../data/operationLogWorkbook.ts'
import { textDifference } from '../data/textDifference.ts'
import type { DifferencePart } from '../data/textDifference.ts'
import { downloadBytes } from './downloadText.ts'
import { Select } from './Select.tsx'
import { Icon } from './Icon.tsx'
import { DateRangePicker } from './DateRangePicker.tsx'
import { operationChoices } from '../data/operationChoices.ts'
import './SystemLogsPage.css'

const previewLogs = import.meta.env.DEV && ['localhost', '127.0.0.1', '[::1]'].includes(location.hostname) && new URLSearchParams(location.search).get('previewLogs') === '1'
const reader = createOperationLogReader({ async request<T>(path: string, options?: { signal?: AbortSignal }): Promise<T> {
  if (previewLogs) { const { previewLogRequest } = await import('../../dev/operationLogPreview.ts'); return previewLogRequest<T>(path, options) }
  return apiClient.request<T>(path, options)
} })
function options(labels: Readonly<Record<string, string>>, all: string) { return [{ value: '', label: all }, ...Object.entries(labels).map(([value, label]) => ({ value, label }))] }

export function SystemLogsPage({ refreshKey }: { refreshKey?: unknown }) {
  const [filters, setFilters] = useState<OperationFilters>({ ...emptyLogFilters })
  const [keyword, setKeyword] = useState('')
  const [cursors, setCursors] = useState<(string | undefined)[]>([undefined])
  const [pageIndex, setPageIndex] = useState(0)
  const snapshot = useRef<string | undefined>(undefined)
  const [page, setPage] = useState<OperationPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState('')
  const [exportMessage, setExportMessage] = useState('')
  const exportController = useRef<AbortController | null>(null)

  function resetPaging() { setPageIndex(0); setCursors([undefined]); snapshot.current = undefined }
  function change(key: keyof OperationFilters, value: string) { resetPaging(); setSelected(null); setFilters(previous => ({ ...previous, [key]: value, ...(key === 'module' && !operationChoices(value).some(option => option.value === previous.action) ? { action: '' } : {}) })) }
  function clear() { setKeyword(''); resetPaging(); setFilters({ ...emptyLogFilters }) }
  useEffect(() => {
    const timer = window.setTimeout(() => { if (keyword !== filters.keyword) change('keyword', keyword) }, 300)
    return () => window.clearTimeout(timer)
  }, [keyword, filters.keyword])
  useEffect(() => { resetPaging(); setSelected(null); setRetry(value => value + 1) }, [refreshKey])
  useEffect(() => {
    exportController.current?.abort(); setExporting(false); setExportError(''); setExportMessage('')
    return () => exportController.current?.abort()
  }, [filters, refreshKey])
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError(''); setPage(null)
    if (!dateRangeValid(filters)) { setError('结束日期不能早于开始日期。'); setLoading(false); return () => controller.abort() }
    void reader.list(filters, { cursor: cursors[pageIndex], snapshotToken: snapshot.current, limit: 20, signal: controller.signal }).then(next => {
      if (!controller.signal.aborted) { snapshot.current = next.snapshotToken; setPage(next) }
    }).catch(reason => { if (!controller.signal.aborted) setError(errorMessage(reason)) }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [filters, pageIndex, cursors, retry])

  async function exportLogs() {
    if (exporting || !page?.total) return
    const controller = new AbortController()
    exportController.current?.abort(); exportController.current = controller
    setExporting(true); setExportError(''); setExportMessage('')
    try {
      const records = await reader.all(filters, controller.signal)
      const bytes = await operationLogWorkbook(records)
      controller.signal.throwIfAborted()
      downloadBytes(`系统操作日志-${new Date().toISOString().slice(0, 10)}.xlsx`, bytes, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
      setExportMessage(`已导出 ${records.length} 条记录`)
    } catch (reason) { if (!controller.signal.aborted) setExportError(errorMessage(reason)) }
    finally { if (!controller.signal.aborted) setExporting(false) }
  }
  function nextPage() {
    if (!page?.nextCursor || loading) return
    const next = page.nextCursor
    if (cursors.includes(next)) { setError('日志分页异常，请刷新后重试。'); return }
    setCursors(previous => [...previous.slice(0, pageIndex + 1), next]); setPageIndex(value => value + 1)
  }
  return <section className="panel operation-logs" aria-label="系统操作日志">
    <div className="panel-heading"><div className="log-heading-title"><h2>系统操作日志</h2>{previewLogs && <span className="badge neutral">预览数据</span>}</div><button className="button primary" disabled={loading || !page?.total || exporting || keyword !== filters.keyword} onClick={() => void exportLogs()}>{exporting ? '正在导出…' : '导出 Excel'}</button></div>
    <fieldset className="log-filter-fields" disabled={exporting}>
      <div className="log-filter-row"><label className="search-box"><Icon name="search" size={18} /><input aria-label="搜索操作人、账号或操作内容" placeholder="搜索操作人、账号、操作内容" value={keyword} onChange={event => setKeyword(event.target.value)} />{keyword && <button className="icon-button" aria-label="清空搜索" onClick={() => setKeyword('')}><Icon name="close" size={14} /></button>}</label>
        <Select label="操作模块" value={filters.module} options={options(logModules, '全部模块')} onChange={value => change('module', value)} />
        <Select label="操作类型" value={filters.action} options={operationChoices(filters.module)} popupMinWidth={210} onChange={value => change('action', value)} />
        <Select label="操作来源" value={filters.source} options={options(logSources, '全部来源')} onChange={value => change('source', value)} />
        <Select label="操作结果" value={filters.result} options={options(logResults, '全部结果')} onChange={value => change('result', value)} />
        <DateRangePicker start={filters.startDate} end={filters.endDate} onChange={(startDate, endDate) => { resetPaging(); setSelected(null); setFilters(previous => ({ ...previous, startDate, endDate })) }} />
        {Object.values(filters).some(Boolean) && <button className="text-button" onClick={clear}>重置</button>}
      </div>
    </fieldset>
    {exportError && <p className="error-message log-feedback" role="alert">{exportError}</p>}{exportMessage && <p className="log-feedback" role="status">{exportMessage}</p>}
    {error ? <div className="log-load-state" role="alert"><Icon name="journal" size={30} /><h3>日志加载失败</h3><p>{error}</p><button className="button secondary" onClick={() => { resetPaging(); setRetry(value => value + 1) }}>重新加载</button></div>
      : loading ? <div className="log-load-state" role="status">正在加载操作日志…</div>
      : !page?.items.length ? <div className="log-load-state"><Icon name="journal" size={30} /><h3>{Object.values(filters).some(Boolean) ? '没有符合条件的日志' : '暂无操作日志'}</h3>{Object.values(filters).some(Boolean) && <button className="button secondary" onClick={clear}>重置筛选</button>}</div>
      : <div className="log-table-scroll"><table className="log-table"><thead><tr><th scope="col">操作时间</th><th scope="col">操作记录</th><th scope="col">结果</th><th scope="col">详情</th></tr></thead><tbody>{page.items.map(row => <tr key={row.id}><td><time dateTime={row.occurredAt}>{logTime(row.occurredAt)}</time></td><td><div className="log-record-line"><strong>{row.actor.displayName}</strong><span>{row.summary}</span><span className="badge log-module" data-log-module={row.module}>{logLabel(logModules, row.module)}</span></div></td><td><span className={`badge ${row.result === 'SUCCESS' ? 'log-success' : 'log-failure'}`}>{logResults[row.result]}</span></td><td><button className="text-button" aria-label={`查看${row.actor.displayName}的${logLabel(logActions, row.action)}记录`} onClick={() => setSelected(row.id)}>查看</button></td></tr>)}</tbody></table></div>}
    <div className="table-footer"><span>{page ? `共 ${page.total} 条` : '—'}</span><div className="log-pagination" aria-label="日志分页"><button className="button secondary" disabled={loading || exporting || pageIndex === 0} onClick={() => { setCursors(previous => previous.slice(0, pageIndex)); setPageIndex(value => value - 1) }}>上一页</button><span>第 {pageIndex + 1} 页</span><button className="button secondary" disabled={loading || exporting || !page?.nextCursor} onClick={nextPage}>下一页</button></div></div>
    {selected && <OperationLogDialog id={selected} onClose={() => setSelected(null)} />}
  </section>
}

function OperationLogDialog({ id, onClose }: { id: string; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [activeId, setActiveId] = useState(id)
  const [detail, setDetail] = useState<OperationDetail | null>(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [tab, setTab] = useState<'change' | 'history'>('change')
  const [historyOpen, setHistoryOpen] = useState(false)
  const content = useRef<HTMLDivElement>(null)
  useEffect(() => { const opener = document.activeElement as HTMLElement | null; dialog.current?.showModal(); return () => { if (opener?.isConnected) opener.focus() } }, [])
  useEffect(() => {
    const controller = new AbortController()
    setDetail(null); setError('')
    void reader.detail(activeId, controller.signal).then(value => { if (!controller.signal.aborted) { setDetail(value); content.current?.focus({ preventScroll: true }) } }).catch(reason => { if (!controller.signal.aborted) setError(errorMessage(reason)) })
    return () => controller.abort()
  }, [activeId, retry])
  function switchTab(next: 'change' | 'history') { setTab(next); setHistoryOpen(false); if (next === 'change') setActiveId(id) }
  return <dialog ref={dialog} className="editor-dialog log-detail-dialog" aria-labelledby="log-detail-title" onCancel={event => { event.preventDefault(); onClose() }}>
    <div className="dialog-head"><h2 id="log-detail-title">日志详情</h2><button className="icon-button" aria-label="关闭日志详情" onClick={onClose}><Icon name="close" size={20} /></button></div>
    <div ref={content} tabIndex={-1} className="dialog-body log-detail-content">
      {error ? <div role="alert"><p className="error-message">{error}</p><button className="button secondary" onClick={() => setRetry(value => value + 1)}>重新加载</button></div> : !detail ? <p role="status">正在加载日志详情…</p> : <>
        <div className="log-detail-summary"><div><h3>{detail.actor.displayName} · {detail.summary}</h3><p><time dateTime={detail.occurredAt}>{logTime(detail.occurredAt)}</time><span>{logLabel(logModules, detail.module)}</span></p></div><span className={`badge ${detail.result === 'SUCCESS' ? 'log-success' : 'log-failure'}`}>{logResults[detail.result]}</span></div>
        <div className="log-detail-tabs" role="tablist" aria-label="日志内容" onKeyDown={event => { if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) { event.preventDefault(); const next = event.key === 'Home' ? 'change' : event.key === 'End' ? 'history' : tab === 'change' ? 'history' : 'change'; switchTab(next); event.currentTarget.querySelector<HTMLButtonElement>(`[data-log-tab="${next}"]`)?.focus() } }}><button id="log-change-tab" data-log-tab="change" role="tab" aria-selected={tab === 'change'} aria-controls="log-change-panel" tabIndex={tab === 'change' ? 0 : -1} onClick={() => switchTab('change')}>本次操作</button><button id="log-history-tab" data-log-tab="history" role="tab" aria-selected={tab === 'history'} aria-controls="log-history-panel" tabIndex={tab === 'history' ? 0 : -1} onClick={() => switchTab('history')}>历史版本</button></div>
        {detail.failureReason && <p className="error-message">{detail.failureReason}</p>}
        {tab === 'change' ? <section id="log-change-panel" role="tabpanel" aria-labelledby="log-change-tab"><SnapshotComparison detail={detail} /></section> : <section id="log-history-panel" role="tabpanel" aria-labelledby="log-history-tab">
          {!historyOpen ? <>{detail.versions.length ? <ol className="log-history-list">{detail.versions.map(version => <li key={version.logId}><div><strong>版本 {version.version}</strong><span>{version.actorName} · {logLabel(logActions, version.action)}</span><time dateTime={version.occurredAt}>{logTime(version.occurredAt)}</time></div><button className="text-button" onClick={() => { setActiveId(version.logId); setHistoryOpen(true) }}>查看版本 {version.version}</button></li>)}</ol> : <p className="log-empty-fields">暂无历史版本</p>}</> : <><button className="text-button log-back-history" onClick={() => setHistoryOpen(false)}>返回版本列表</button><SnapshotComparison detail={detail} /></>}
        </section>}
        <details className="log-operation-info"><summary>操作信息</summary><dl className="log-metadata"><div><dt>登录账号</dt><dd>{detail.actor.loginName ?? '—'}</dd></div><div><dt>操作来源</dt><dd>{logLabel(logSources, detail.source)}</dd></div><div><dt>操作对象</dt><dd>{detail.object.label}</dd></div>{detail.requestId && <div><dt>操作编号</dt><dd>{detail.requestId}</dd></div>}</dl></details>
      </>}
    </div><div className="dialog-footer"><button className="button secondary" onClick={onClose}>关闭</button></div>
  </dialog>
}

function DifferenceText({ parts, side }: { parts: DifferencePart[]; side: 'before' | 'after' }) {
  return <>{parts.map((part, index) => part.changed ? <mark key={index} className={`log-diff-${side}`} aria-label={`${side === 'before' ? '修改前差异' : '修改后差异'}：${part.text}`}>{part.text}</mark> : <span key={index}>{part.text}</span>)}</>
}

function SnapshotComparison({ detail }: { detail: OperationDetail }) {
  const fields = compareSnapshots(detail.before, detail.after), comparing = !!detail.before && !!detail.after
  if (!detail.before && !detail.after) return <p className="log-empty-fields">{detail.result === 'FAILURE' ? '本次操作未生成新版本' : '此操作无内容版本'}</p>
  return <div className="log-versions-scroll"><div className={`log-version-pair ${comparing ? '' : 'single-version'}`} aria-label="完整历史内容">
    {detail.before && <h3 className="log-version-heading">{comparing ? '修改前' : '删除前'}<span>版本 {detail.before.version}</span></h3>}{detail.after && <h3 className="log-version-heading">{comparing ? '修改后' : '创建后'}<span>版本 {detail.after.version}</span></h3>}
    {fields.map(field => {
      const diff = textDifference(field.before ?? '', field.after ?? '')
      return <div className="log-version-field-row" key={field.key}>{detail.before && <div className="log-version-field"><h4>{field.label}{comparing && field.changed && <span className="log-field-dot" aria-label="此字段已修改" />}</h4><p>{field.before === null || field.before === '' ? '—' : comparing ? <DifferenceText parts={diff.before} side="before" /> : field.before}</p></div>}{detail.after && <div className="log-version-field"><h4>{field.label}{comparing && field.changed && <span className="log-field-dot" aria-label="此字段已修改" />}</h4><p>{field.after === null || field.after === '' ? '—' : comparing ? <DifferenceText parts={diff.after} side="after" /> : field.after}</p></div>}</div>
    })}
    {!fields.length && <p className="log-empty-fields">无可展示字段</p>}
  </div></div>
}
