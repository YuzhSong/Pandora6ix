import { Select } from './Select.tsx'
import type { AdminData } from '../data/types.ts'
import { bulkPersonnelHeaders } from '../data/bulkPersonnel.ts'
import { organizationFilterOptions } from '../data/organizationHierarchy.ts'
import { canHaveSupervisor, supervisorCandidates } from '../data/supervisors.ts'
import { businessRoleOptions, personnelRole, preparePersonnelRow } from '../data/personnelCreation.ts'

export function BulkPersonnelGrid({ rows, data, onChange }: { rows: string[][]; data: AdminData; onChange: (rows: string[][]) => void }) {
  const organizations = organizationFilterOptions(data.units)
  function update(row: number, column: number, value: string) {
    onChange(rows.map((cells, index) => {
      if (index !== row) return cells
      const next = cells.map((cell, offset) => offset === column ? value : cell)
      if (column === 6 && !canHaveSupervisor([personnelRole(value)])) next[8] = ''
      return preparePersonnelRow(next)
    }))
  }
  return <>
    <div className="bulk-entry-table" role="region" aria-label="员工资料填写表" tabIndex={0}>
      <table><thead><tr><th>行</th>{bulkPersonnelHeaders.map((header, index) => <th key={header}>{header}{index < 5 && <span aria-hidden="true"> *</span>}</th>)}<th>操作</th></tr></thead>
        <tbody>{rows.map((cells, row) => <tr key={row}><td>{row + 1}</td>{cells.map((value, column) => {
          if (column === 2) return null
          const label = `第${row + 1}行${bulkPersonnelHeaders[column > 2 ? column - 1 : column]}`
          let role = 'EMPLOYEE' as ReturnType<typeof personnelRole>
          try { role = personnelRole(cells[6]) } catch { /* Keep invalid CSV values visible for correction. */ }
          if (column === 8 && !canHaveSupervisor([role])) return <td key={column}><span aria-label={label}>—</span></td>
          const supervisors = supervisorCandidates(data.people, undefined, [role])
          const options = column === 3 ? [{ value: '', label: '请选择' }, { value: '男', label: '男' }, { value: '女', label: '女' }]
            : column === 6 ? [...businessRoleOptions]
            : column === 7 ? [{ value: '', label: '未分配' }, ...organizations]
              : [{ value: '', label: '未设置' }, ...supervisors.map(person => ({ value: person.loginName, label: `${person.displayName}（${person.loginName}）` }))]
          if (value && !options.some(option => option.value === value)) options.push({ value, label: value })
          return <td key={column}>{[3, 6, 7, 8].includes(column)
            ? <Select label={label} value={column === 6 && !value ? 'EMPLOYEE' : value} options={options} onChange={next => update(row, column, next)} />
            : <input aria-label={label} value={value} autoComplete="off" spellCheck={false} maxLength={column === 4 ? 18 : column === 5 ? 25 : 128} onChange={event => update(row, column, event.target.value)} />}</td>
        })}<td><button type="button" className="text-button" aria-label={`删除第${row + 1}行`} onClick={() => onChange(rows.length === 1 ? [Array<string>(9).fill('')] : rows.filter((_, index) => index !== row))}>删除</button></td></tr>)}</tbody>
      </table>
    </div>
    <button type="button" className="button secondary" disabled={rows.length >= 100} onClick={() => onChange([...rows, Array<string>(9).fill('')])}>添加一行</button>
  </>
}
