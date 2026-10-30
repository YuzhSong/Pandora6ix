import { logActions, logLabel, logModules, logResults, logSources, logTime } from './operationHistory.ts'
import type { OperationRecord } from './operationHistory.ts'

export async function operationLogWorkbook(records: readonly OperationRecord[]): Promise<Uint8Array<ArrayBuffer>> {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'Pandora'
  const sheet = workbook.addWorksheet('系统操作日志', { views: [{ state: 'frozen', ySplit: 1 }] })
  sheet.columns = [
    { header: '操作时间（北京时间）', width: 26 }, { header: '操作人', width: 18 }, { header: '登录账号', width: 24 },
    { header: '模块', width: 18 }, { header: '操作类型', width: 18 }, { header: '操作对象', width: 32 },
    { header: '操作内容', width: 58 }, { header: '来源', width: 14 }, { header: '结果', width: 12 },
  ]
  for (const row of records) sheet.addRow([logTime(row.occurredAt), row.actor.displayName ?? '未记录', row.actor.loginName ?? '', logLabel(logModules, row.module), logLabel(logActions, row.action), row.object.label ?? '未记录', row.summary, logLabel(logSources, row.source), logResults[row.result]])
  sheet.eachRow(row => { row.height = 32; row.eachCell(cell => { cell.numFmt = '@'; cell.alignment = { vertical: 'middle', wrapText: true } }) })
  sheet.getRow(1).font = { bold: true, color: { argb: 'FF3C3B31' } }
  sheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFF0B6' } }
  sheet.autoFilter = { from: 'A1', to: `I${sheet.rowCount}` }
  return new Uint8Array(await workbook.xlsx.writeBuffer())
}
