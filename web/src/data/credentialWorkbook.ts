import type { CreatedAccountReceipt } from './accountHandoff.ts'

export async function credentialWorkbook(accounts: readonly CreatedAccountReceipt[]): Promise<Uint8Array<ArrayBuffer>> {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'Pandora'
  const sheet = workbook.addWorksheet('账号交付', { views: [{ state: 'frozen', ySplit: 1 }] })
  sheet.columns = [
    { header: '姓名', width: 20 },
    { header: '公司登录标识', width: 44 },
    { header: '登录账号', width: 30 },
    { header: '初始密码', width: 36 },
  ]
  for (const account of accounts) sheet.addRow([account.displayName, account.enterpriseDomain ?? '', account.loginName, account.initialPassword])
  sheet.eachRow(row => {
    row.height = 24
    row.eachCell(cell => { cell.numFmt = '@'; cell.alignment = { vertical: 'middle', wrapText: true } })
  })
  sheet.getRow(1).font = { bold: true, color: { argb: 'FF3C3B31' } }
  sheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFF0B6' } }
  sheet.autoFilter = { from: 'A1', to: `D${sheet.rowCount}` }
  return new Uint8Array(await workbook.xlsx.writeBuffer())
}
