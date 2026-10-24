import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { calendarDate, calendarDays, localDate, parseCalendarDate, parseCalendarMonth, rangePreset, shiftMonth } from '../data/calendarRange.ts'
import { Icon } from './Icon.tsx'

export function DateRangePicker({ start, end, onChange }: { start: string; end: string; onChange: (start: string, end: string) => void }) {
  const id = useId(), anchor = useRef<HTMLButtonElement>(null), popup = useRef<HTMLDivElement>(null)
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
  const [opened, setOpened] = useState(false), [from, setFrom] = useState(start), [to, setTo] = useState(end), [month, setMonth] = useState((start || today).slice(0, 7))
  const [monthDraft, setMonthDraft] = useState<string | null>(null), [inputError, setInputError] = useState('')
  const normalizedFrom = parseCalendarDate(from), normalizedTo = parseCalendarDate(to)
  const validRange = !!normalizedFrom && !!normalizedTo && normalizedFrom <= normalizedTo
  useEffect(() => {
    const element = popup.current!
    const toggle = () => setOpened(element.matches(':popover-open'))
    const close = () => element.hidePopover()
    const scroll = (event: Event) => { if (!element.contains(event.target as Node)) close() }
    element.addEventListener('toggle', toggle); window.addEventListener('resize', close); window.addEventListener('scroll', scroll, true)
    return () => { element.removeEventListener('toggle', toggle); window.removeEventListener('resize', close); window.removeEventListener('scroll', scroll, true) }
  }, [])
  function open() {
    const bounds = anchor.current!.getBoundingClientRect(), element = popup.current!, width = Math.min(340, innerWidth - 24)
    setFrom(start); setTo(end); setMonth((start || today).slice(0, 7)); setMonthDraft(null); setInputError('')
    element.style.width = `${width}px`; element.style.left = `${Math.max(12, Math.min(bounds.left, innerWidth - width - 12))}px`
    const below = innerHeight - bounds.bottom - 12
    element.style.top = below >= Math.min(480, bounds.top) ? `${bounds.bottom + 8}px` : 'auto'
    element.style.bottom = below >= Math.min(480, bounds.top) ? 'auto' : `${innerHeight - bounds.top + 8}px`
    element.style.maxHeight = `${Math.max(below, bounds.top - 12)}px`
    element.showPopover()
  }
  function commit(first: string, last: string) { onChange(first, last); popup.current?.hidePopover(); anchor.current?.focus() }
  function choose(day: string) {
    setInputError(''); setMonthDraft(null)
    if (!normalizedFrom || to) { setFrom(day); setTo('') }
    else if (day < normalizedFrom) { setFrom(day); setTo(normalizedFrom) }
    else setTo(day)
    if (!day.startsWith(month)) setMonth(day.slice(0, 7))
  }
  function finishDate(which: 'from' | 'to') {
    const raw = which === 'from' ? from : to, parsed = parseCalendarDate(raw)
    if (raw && !parsed) { setInputError('请输入有效日期，例如 2026-10-20'); return }
    if (parsed) { if (which === 'from') setFrom(parsed); else setTo(parsed); setMonth(parsed.slice(0, 7)); setMonthDraft(null) }
    const first = which === 'from' ? parsed : normalizedFrom, last = which === 'to' ? parsed : normalizedTo
    setInputError(first && last && first > last ? '结束日期不能早于开始日期' : '')
  }
  function finishMonth() {
    if (monthDraft === null) return
    const parsed = parseCalendarMonth(monthDraft)
    if (!parsed) { setInputError('请输入有效年月，例如 2024-02'); return }
    setMonth(parsed); setMonthDraft(null); setInputError('')
  }
  function moveMonth(offset: number) { const next = shiftMonth(month, offset); if (Number(next.slice(0, 4)) >= 1000 && Number(next.slice(0, 4)) <= 9999) setMonth(next); setMonthDraft(null); setInputError('') }
  function key(event: KeyboardEvent<HTMLButtonElement>, value: string) {
    if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End', 'PageUp', 'PageDown'].includes(event.key)) return
    event.preventDefault()
    const date = localDate(value), weekday = (date.getDay() + 6) % 7
    if (event.key === 'PageUp' || event.key === 'PageDown') { const day = date.getDate(); date.setDate(1); date.setMonth(date.getMonth() + (event.key === 'PageUp' ? -1 : 1)); const last = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate(); date.setDate(Math.min(day, last)) }
    else date.setDate(date.getDate() + ({ ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7, Home: -weekday, End: 6 - weekday }[event.key] ?? 0))
    const next = calendarDate(date); setMonth(next.slice(0, 7))
    requestAnimationFrame(() => popup.current?.querySelector<HTMLButtonElement>(`[data-date="${next}"]`)?.focus())
  }
  return <div className="log-date-picker"><button ref={anchor} type="button" className="log-date-trigger" aria-label="选择日期范围" aria-expanded={opened} aria-controls={id} aria-haspopup="dialog" onClick={() => opened ? popup.current?.hidePopover() : open()}><svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M7 2v6M17 2v6M3 11h18" /></svg><span>{start || end ? `${start || '不限'} 至 ${end || '不限'}` : '全部日期'}</span><Icon name="chevron" size={14} /></button>
    <div ref={popup} id={id} popover="auto" role="dialog" aria-label="日期范围" className="log-calendar" onKeyDown={event => { if (event.key === 'Escape') { event.preventDefault(); popup.current?.hidePopover(); anchor.current?.focus() } }}>
      <div className="log-calendar-presets">{(['today', 'week', 'month'] as const).map((kind, index) => <button key={kind} type="button" onClick={() => { const [first, last] = rangePreset(kind, today); setFrom(first); setTo(last); setMonth(first.slice(0, 7)); setMonthDraft(null); setInputError('') }}>{['今天', '最近7天', '本月'][index]}</button>)}</div>
      <div className="log-calendar-range"><input type="text" aria-label="开始日期" placeholder="开始日期" value={from} maxLength={10} onChange={event => { setFrom(event.target.value); setInputError('') }} onBlur={() => finishDate('from')} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); finishDate('from') } }} /><span>至</span><input type="text" aria-label="结束日期" placeholder="结束日期" value={to} maxLength={10} onChange={event => { setTo(event.target.value); setInputError('') }} onBlur={() => finishDate('to')} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); finishDate('to') } }} /></div>
      <div className="log-calendar-month"><button type="button" aria-label="上个月" onClick={() => moveMonth(-1)}><span className="log-calendar-prev"><Icon name="chevron" size={16} /></span></button><input type="text" aria-label="显示年月" value={monthDraft ?? `${Number(month.slice(0, 4))}年${Number(month.slice(5))}月`} onFocus={event => event.currentTarget.select()} onChange={event => { setMonthDraft(event.target.value); setInputError('') }} onBlur={finishMonth} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); finishMonth() } }} /><button type="button" aria-label="下个月" onClick={() => moveMonth(1)}><Icon name="chevron" size={16} /></button></div>
      <div className="log-calendar-week" aria-hidden="true">{'一二三四五六日'.split('').map(day => <span key={day}>{day}</span>)}</div>
      <div className="log-calendar-grid" role="group" aria-label={`${month}日期`}>{calendarDays(month).map(day => <button type="button" key={day} data-date={day} className={`${!day.startsWith(month) ? 'other-month' : ''} ${day === normalizedFrom || day === normalizedTo ? 'range-end' : normalizedFrom && normalizedTo && day > normalizedFrom && day < normalizedTo ? 'in-range' : ''}`} aria-label={day} aria-pressed={day === normalizedFrom || day === normalizedTo} aria-current={day === today ? 'date' : undefined} onClick={() => choose(day)} onKeyDown={event => key(event, day)}>{localDate(day).getDate()}</button>)}</div>
      {inputError && <p className="log-calendar-error" role="alert">{inputError}</p>}
      <div className="log-calendar-footer"><button type="button" className="text-button" onClick={() => commit('', '')}>全部日期</button><button type="button" className="button primary" disabled={!validRange || (monthDraft !== null && !parseCalendarMonth(monthDraft))} onClick={() => { if (normalizedFrom && normalizedTo && validRange) commit(normalizedFrom, normalizedTo) }}>确定</button></div>
    </div>
  </div>
}
