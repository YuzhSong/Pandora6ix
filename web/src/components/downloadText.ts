export function downloadText(filename: string, text: string, type = 'text/plain;charset=utf-8') {
  downloadBlob(filename, new Blob(['\ufeff', text], { type }))
}

export function downloadBytes(filename: string, bytes: Uint8Array<ArrayBuffer>, type: string) {
  downloadBlob(filename, new Blob([bytes], { type }))
}

function downloadBlob(filename: string, blob: Blob) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url; link.download = filename; link.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
