export function calendarDate(value: Date): string { return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}` }
export function localDate(value: string): Date { return new Date(`${value}T12:00:00`) }
export function parseCalendarDate(value: string): string | null {
  const match = /^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$/.exec(value.trim())
  if (!match) return null
  const year = Number(match[1]), month = Number(match[2]), day = Number(match[3])
  if (year < 1000 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31) return null
  const date = new Date(year, month - 1, day, 12)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? calendarDate(date) : null
}
export function parseCalendarMonth(value: string): string | null {
  const match = /^(\d{4})(?:年|[-/.])(\d{1,2})月?$/.exec(value.trim())
  if (!match) return null
  const date = parseCalendarDate(`${match[1]}-${match[2]}-01`)
  return date?.slice(0, 7) ?? null
}
export function calendarDays(month: string): string[] {
  const first = localDate(`${month}-01`), offset = (first.getDay() + 6) % 7
  first.setDate(first.getDate() - offset)
  return Array.from({ length: 42 }, (_, index) => { const day = new Date(first); day.setDate(first.getDate() + index); return calendarDate(day) })
}
export function shiftMonth(month: string, offset: number): string {
  const date = localDate(`${month}-01`); date.setMonth(date.getMonth() + offset); return calendarDate(date).slice(0, 7)
}
export function rangePreset(kind: 'today' | 'week' | 'month', today: string): [string, string] {
  const first = localDate(today)
  if (kind === 'week') first.setDate(first.getDate() - 6)
  if (kind === 'month') first.setDate(1)
  return [calendarDate(first), today]
}
