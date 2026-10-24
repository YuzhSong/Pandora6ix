export interface DifferencePart { text: string; changed: boolean }

/** Compare readable Unicode text; bound work for large historical documents. */
export function textDifference(before: string, after: string): { before: DifferencePart[]; after: DifferencePart[] } {
  if (before === after) return { before: [{ text: before, changed: false }], after: [{ text: after, changed: false }] }
  const left = Array.from(before), right = Array.from(after)
  let prefix = 0, suffix = 0
  while (prefix < left.length && prefix < right.length && left[prefix] === right[prefix]) prefix++
  while (suffix < left.length - prefix && suffix < right.length - prefix && left[left.length - suffix - 1] === right[right.length - suffix - 1]) suffix++
  const old = left.slice(prefix, left.length - suffix), next = right.slice(prefix, right.length - suffix)
  const oldParts: DifferencePart[] = [], newParts: DifferencePart[] = []
  function append(parts: DifferencePart[], text: string, changed: boolean) {
    if (!text) return
    const last = parts.at(-1)
    if (last?.changed === changed) last.text += text
    else parts.push({ text, changed })
  }
  const start = left.slice(0, prefix).join(''), end = suffix ? left.slice(-suffix).join('') : ''
  append(oldParts, start, false); append(newParts, start, false)
  if (!old.length || !next.length || old.length * next.length > 250_000) {
    append(oldParts, old.join(''), true); append(newParts, next.join(''), true)
  } else {
    const width = next.length + 1, lengths = new Uint32Array((old.length + 1) * width)
    for (let i = old.length - 1; i >= 0; i--) for (let j = next.length - 1; j >= 0; j--) {
      lengths[i * width + j] = old[i] === next[j] ? 1 + lengths[(i + 1) * width + j + 1] : Math.max(lengths[(i + 1) * width + j], lengths[i * width + j + 1])
    }
    let i = 0, j = 0
    while (i < old.length || j < next.length) {
      if (i < old.length && j < next.length && old[i] === next[j]) { append(oldParts, old[i++], false); append(newParts, next[j++], false) }
      else if (i < old.length && (j === next.length || lengths[(i + 1) * width + j] >= lengths[i * width + j + 1])) append(oldParts, old[i++], true)
      else append(newParts, next[j++], true)
    }
  }
  append(oldParts, end, false); append(newParts, end, false)
  return { before: oldParts, after: newParts }
}
