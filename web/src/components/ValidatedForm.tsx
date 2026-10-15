import { useEffect, useId, useState } from 'react'
import type { FormEventHandler, ReactNode } from 'react'
import { createPortal } from 'react-dom'

type Field = HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
type FieldError = { field: Field; label: HTMLLabelElement | null; message: string; id: string }

function fieldMessage(field: Field) {
  const label = field.labels?.[0]
  const name = (field.getAttribute('aria-label') || label?.firstChild?.textContent || '此项').replace(/\*/g, '').trim()
  if (field.required && (!field.value || (field.type !== 'password' && !field.value.trim()))) return `请填写${name}`
  if (field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) {
    if (field.minLength > 0 && field.value && field.value.length < field.minLength) return `${name}至少需要 ${field.minLength} 个字符`
  }
  if (field.validity.badInput) return `请填写有效的${name}`
  if (field.validity.rangeUnderflow && field instanceof HTMLInputElement) return `${name}不能小于 ${field.min}`
  if (field.validity.rangeOverflow && field instanceof HTMLInputElement) return `${name}不能大于 ${field.max}`
  if (field.validity.stepMismatch) return `${name}请填写整数`
  if (field.validity.patternMismatch) return `${name}格式不正确，请按照示例填写`
  if (!field.validity.valid) return `请检查${name}的格式`
  return ''
}

/** Keep native constraints, present their errors alongside the fields. */
export function ValidatedForm({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) {
  const prefix = useId()
  const [errors, setErrors] = useState<FieldError[]>([])
  const validate = (form: HTMLFormElement) => Array.from(form.elements).flatMap((element, index) => {
    if (!(element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement) || !element.willValidate) return []
    const message = fieldMessage(element)
    return message ? [{ field: element, label: element.labels?.[0] ?? null, message, id: `${prefix}-error-${index}` }] : []
  })
  useEffect(() => {
    const restore = errors.map(({ field, id }) => {
      const describedBy = field.getAttribute('aria-describedby')
      const invalid = field.getAttribute('aria-invalid')
      field.setAttribute('aria-invalid', 'true')
      field.setAttribute('aria-describedby', [describedBy, id].filter(Boolean).join(' '))
      return () => {
        if (invalid === null) field.removeAttribute('aria-invalid'); else field.setAttribute('aria-invalid', invalid)
        if (describedBy === null) field.removeAttribute('aria-describedby'); else field.setAttribute('aria-describedby', describedBy)
      }
    })
    return () => restore.forEach(cleanup => cleanup())
  }, [errors])
  return <form noValidate onSubmit={event => {
    const next = validate(event.currentTarget)
    setErrors(next)
    if (next.length) { event.preventDefault(); next[0].field.focus(); return }
    onSubmit(event)
  }} onInput={event => { if (errors.length) setErrors(validate(event.currentTarget)) }}>
    {children}
    {errors.map(({ label, message, id }) => label ? createPortal(<small className="field-error" id={id} aria-hidden="true">{message}</small>, label, id) : <p className="field-error" id={id} key={id}>{message}</p>)}
    {errors.length > 0 && <span className="validation-announcement" role="alert">{errors.map(error => error.message).join('；')}</span>}
  </form>
}
