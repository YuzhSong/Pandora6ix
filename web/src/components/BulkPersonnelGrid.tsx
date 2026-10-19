import { Select } from './Select.tsx'
import type { AdminData } from '../data/types.ts'
import { bulkPersonnelHeaders } from '../data/bulkPersonnel.ts'
import { organizationFilterOptions } from '../data/organizationHierarchy.ts'
import { supervisorCandidates } from '../data/supervisors.ts'

export function BulkPersonnelGrid({ rows, data, onChange }: { rows: string[][]; data: AdminData; onChange: (rows: string[][]) => void }) {
  const organizations = organizationFilterOptions(data.units)
  const supervisors = supervisorCandidates(data.people, undefined, ['EMPLOYEE'])
  function update(row: number, column: number, value: string) {
    onChange(rows.map((cells, index) => index === row ? cells.map((cell, offset) => offset === column ? value : cell) : cells))
  }
  return <>
    <div className="bulk-entry-table" role="region" aria-label="员工资料填写表" tabIndex={0}>
      <table><thead><tr><th>行</th>{bulkPersonnelHeaders.map((header, index) => <th key={header}>{header}{index < 6 && <span aria-hidden="true"> *</span>}</th>)}<th>操作</th></tr></thead>
        <tbody>{rows.map((cells, row) => <tr key={row}><td>{row + 1}</td>{cells.map((value, column) => {
          const label = `第${row + 1}行${bulkPersonnelHeaders[column]}`
          const options = column === 3 ? [{ value: '', label: '请选择' }, { value: '男', label: '男' }, { value: '女', label: '女' }]
            : column === 7 ? [{ value: '', label: '未分配' }, ...organizations]
              : [{ value: '', label: '未设置' }, ...supervisors.map(person => ({ value: person.loginName, label: `${person.displayName}（${person.loginName}）` }))]
          if (value && !options.some(option => option.value === value)) options.push({ value, label: value })
          return <td key={column}>{[3, 7, 8].includes(column)
            ? <Select label={label} value={value} options={options} onChange={next => update(row, column, next)} />
            : <input aria-label={label} value={value} autoComplete="off" spellCheck={false} maxLength={column === 4 ? 18 : column === 5 ? 25 : column === 2 ? 72 : 128} onChange={event => update(row, column, event.target.value)} />}</td>
        })}<td><button type="button" className="text-button" aria-label={`删除第${row + 1}行`} onClick={() => onChange(rows.length === 1 ? [Array<string>(9).fill('')] : rows.filter((_, index) => index !== row))}>删除</button></td></tr>)}</tbody>
      </table>
    </div>
    <button type="button" className="button secondary" disabled={rows.length >= 100} onClick={() => onChange([...rows, Array<string>(9).fill('')])}>添加一行</button>
  </>
}
