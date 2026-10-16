import { useState } from 'react'

const presets = [
  ['蓝色', '#4F70E8'], ['紫色', '#8B5CF6'], ['粉色', '#DB4985'], ['红色', '#DC4545'],
  ['橙色', '#E77D32'], ['黄色', '#EDC85D'], ['绿色', '#38976A'], ['青色', '#299CA6'],
  ['浅蓝', '#BFD9F5'], ['浅紫', '#DDD0F3'], ['浅绿', '#C4E5CF'], ['灰色', '#77808C'],
]
type Hsl = [number, number, number]
function toHsl(hex: string): Hsl {
  const [r, g, b] = [1, 3, 5].map(start => parseInt(hex.slice(start, start + 2), 16) / 255)
  const max = Math.max(r, g, b), min = Math.min(r, g, b), delta = max - min
  const l = (max + min) / 2
  const h = !delta ? 0 : max === r ? ((g - b) / delta + 6) % 6 : max === g ? (b - r) / delta + 2 : (r - g) / delta + 4
  return [Math.round(h * 60), Math.round((delta ? delta / (1 - Math.abs(2 * l - 1)) : 0) * 100), Math.round(l * 100)]
}
function toHex([h, s, l]: Hsl) {
  const a = s / 100 * Math.min(l / 100, 1 - l / 100)
  return '#' + [0, 8, 4].map(n => {
    const k = (n + h / 30) % 12
    return Math.round((l / 100 - a * Math.max(-1, Math.min(k - 3, 9 - k, 1))) * 255).toString(16).padStart(2, '0')
  }).join('').toUpperCase()
}
type EyeDropperConstructor = new () => { open: () => Promise<{ sRGBHex: string }> }

export function TagColorPicker({ value, onChange, onPickingChange }: { value: string; onChange: (color: string) => void; onPickingChange: (picking: boolean) => void }) {
  const [hsl, setHsl] = useState<Hsl>(() => toHsl(value))
  const [picking, setPicking] = useState(false)
  const [message, setMessage] = useState('')
  const EyeDropper = (window as Window & { EyeDropper?: EyeDropperConstructor }).EyeDropper
  const select = (color: string) => { onChange(color); setHsl(toHsl(color)); setMessage('') }
  const adjust = (index: number, value: number) => {
    const next: Hsl = [...hsl]
    next[index] = value
    setHsl(next); onChange(toHex(next))
  }
  async function extract() {
    if (!EyeDropper || picking) return
    setPicking(true); onPickingChange(true); setMessage('')
    try { select((await new EyeDropper().open()).sRGBHex.toUpperCase()) }
    catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) setMessage('取色未完成，请重试或使用下方自定义颜色。') }
    finally { setPicking(false); onPickingChange(false) }
  }
  return <fieldset className="tag-color-picker"><legend>标签颜色 <span className="required" aria-hidden="true">*</span></legend>
    <div className="color-presets">{presets.map(([name, color]) => <button type="button" key={color} aria-label={name} aria-pressed={value.toUpperCase() === color} title={name} style={{ background: color }} onClick={() => select(color)}><span>{value.toUpperCase() === color ? '✓' : ''}</span></button>)}</div>
    <div className="color-picker-actions"><details><summary>自定义颜色</summary><div className="color-sliders">{['色相', '鲜艳度', '明暗'].map((name, index) => <label key={name}>{name}<input type="range" min={0} max={index === 0 ? 359 : 100} value={hsl[index]} onChange={event => adjust(index, Number(event.target.value))} style={{ background: index === 0 ? 'linear-gradient(90deg, red, yellow, lime, cyan, blue, magenta, red)' : index === 1 ? `linear-gradient(90deg, hsl(${hsl[0]} 0% ${hsl[2]}%), hsl(${hsl[0]} 100% ${hsl[2]}%))` : `linear-gradient(90deg, black, hsl(${hsl[0]} ${hsl[1]}% 50%), white)` }} /></label>)}</div></details>
      <button className="button secondary" type="button" disabled={!EyeDropper || picking} onClick={extract}>{picking ? '正在取色…' : '屏幕取色'}</button></div>
    {!EyeDropper && <small>当前浏览器不支持屏幕取色，可使用常用色或自定义颜色。</small>}
    {message && <small role="status">{message}</small>}
  </fieldset>
}
