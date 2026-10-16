import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { Icon } from './Icon.tsx'

interface Option { value: string; label: string }
export function Select({ label, value, options, onChange, disabled = false, required = false }: {
  label: string; value: string; options: Option[]; onChange: (value: string) => void; disabled?: boolean; required?: boolean
}) {
  const id = useId()
  const anchor = useRef<HTMLButtonElement>(null)
  const popup = useRef<HTMLDivElement>(null)
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const selected = options.findIndex(option => option.value === value)
  useEffect(() => {
    const element = popup.current!
    const toggle = () => setOpen(element.matches(':popover-open'))
    const close = () => element.hidePopover()
    element.addEventListener('toggle', toggle)
    window.addEventListener('resize', close)
    const scroll = (event: Event) => { if (!element.contains(event.target as Node)) close() }
    window.addEventListener('scroll', scroll, true)
    return () => { element.removeEventListener('toggle', toggle); window.removeEventListener('resize', close); window.removeEventListener('scroll', scroll, true) }
  }, [])
  useEffect(() => { if (disabled) popup.current?.hidePopover() }, [disabled])
  function show(index = Math.max(0, selected)) {
    if (disabled || !options.length) return
    const bounds = anchor.current!.getBoundingClientRect(), element = popup.current!
    const below = innerHeight - bounds.bottom - 16, above = bounds.top - 16
    const upward = below < Math.min(220, options.length * 42 + 12) && above > below
    element.style.width = `${bounds.width}px`
    element.style.left = `${Math.max(8, Math.min(bounds.left, innerWidth - bounds.width - 8))}px`
    element.style.maxHeight = `${Math.max(80, Math.min(300, upward ? above : below))}px`
    element.style.top = upward ? 'auto' : `${bounds.bottom + 6}px`
    element.style.bottom = upward ? `${innerHeight - bounds.top + 6}px` : 'auto'
    setActive(index); element.showPopover()
  }
  function choose(index: number) {
    if (!options[index]) return
    onChange(options[index].value); popup.current?.hidePopover(); anchor.current?.focus()
  }
  function key(event: KeyboardEvent) {
    if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
      event.preventDefault()
      const next = event.key === 'Home' ? 0 : event.key === 'End' ? options.length - 1 : open ? (active + (event.key === 'ArrowDown' ? 1 : -1) + options.length) % options.length : Math.max(0, selected)
      if (open) setActive(next); else show(next)
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault(); if (open) choose(active); else show()
    } else if (event.key === 'Escape' || event.key === 'Tab') {
      popup.current?.hidePopover()
      if (event.key === 'Escape') { event.preventDefault(); anchor.current?.focus() }
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      const index = options.findIndex(option => option.label.toLocaleLowerCase().startsWith(event.key.toLocaleLowerCase()))
      if (index >= 0) { event.preventDefault(); if (open) setActive(index); else show(index) }
    }
  }
  useEffect(() => { if (open) popup.current?.children[active]?.scrollIntoView({ block: 'nearest' }) }, [active, open])
  return <div className="select-control"><button ref={anchor} type="button" className="select-trigger" role="combobox" aria-label={label} aria-required={required} aria-expanded={open} aria-controls={`${id}-list`} aria-haspopup="listbox" aria-activedescendant={open ? `${id}-option-${active}` : undefined} disabled={disabled || !options.length} onClick={() => open ? popup.current?.hidePopover() : show()} onKeyDown={key}><span>{options[selected]?.label ?? '请选择'}</span><Icon name="chevron" size={15} /></button>
    <div ref={popup} id={`${id}-list`} popover="auto" role="listbox" aria-label={label} className="select-popup" onKeyDown={key}>{options.map((option, index) => <button key={option.value} id={`${id}-option-${index}`} role="option" aria-selected={option.value === value} tabIndex={-1} type="button" className={index === active ? 'select-option highlighted' : 'select-option'} onMouseEnter={() => setActive(index)} onClick={() => choose(index)}><span>{option.label}</span>{option.value === value && <Icon name="check" size={15} />}</button>)}</div>
  </div>
}
