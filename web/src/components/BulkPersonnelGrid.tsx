import { followingOrganization } from '../data/personnelHistory.ts'
import { supervisorOptionLabel } from '../data/personnelDisplay.ts'
import { Select } from './Select.tsx'
import type { AdminData, Person } from '../data/types.ts'
import { bulkPersonnelHeaders } from '../data/bulkPersonnel.ts'
import { organizationFilterOptions } from '../data/organizationHierarchy.ts'
import { organizationSupervisor } from '../data/supervisors.ts'
import { businessRoleOptions, defaultPersonnelRole, generateLoginName, organizationAllowsRole, personnelRole, preparePersonnelRow } from '../data/personnelCreation.ts'

export function BulkPersonnelGrid({ rows, data, onChange }: { rows: string[][]; data: AdminData; onChange: (rows: string[][]) => void }) {
  const organizations = organizationFilterOptions(data.units)
  const occupants: Pick<Person, 'orgUnitId' | 'roles' | 'employmentStatus'>[] = [...data.people]
  const displayedRoles = rows.map(cells => {
    const organization = cells[7]
    try {
      const role = cells[6] ? personnelRole(cells[6]) : defaultPersonnelRole(data.units, occupants, organization)
      if (role === 'DEPARTMENT_MANAGER') occupants.push({ orgUnitId: organization, roles: [role] })
      return role
    } catch { return 'EMPLOYEE' as const }
  })
  function update(row: number, column: number, value: string) {
    onChange(rows.map((cells, index) => {
      if (index !== row) return cells
      const next = cells.map((cell, offset) => offset === column ? value : cell)
      const accounts = [...data.people, ...rows.filter((_, i) => i !== row).map(item => ({ loginName: item[1] }))]
      if (column === 0 && (!cells[1] || cells[1] === generateLoginName(cells[0], accounts))) next[1] = generateLoginName(value, accounts)
      if (column === 6 || column === 7) next[8] = ''
      return preparePersonnelRow(next)
    }))
  }
  return <>
    <div className="bulk-entry-table" role="region" aria-label="员工资料填写表" tabIndex={0}>
      <table><thead><tr><th>行</th>{bulkPersonnelHeaders.map((header, index) => <th key={header}>{header}{index < 5 && <span aria-hidden="true"> *</span>}</th>)}<th>操作</th></tr></thead>
        <tbody>{rows.map((cells, row) => <tr key={row}><td>{row + 1}</td>{cells.map((value, column) => {
          if (column === 2) return null
          const label = `第${row + 1}行${bulkPersonnelHeaders[column > 2 ? column - 1 : column]}`
          const role = displayedRoles[row]
          if (column === 8) {
            const legacy = data.people.find(person => person.loginName === value)
            const organization = cells[7] || followingOrganization(data, [role], legacy?.id ?? '') || ''
            const supervisor = organizationSupervisor(data, organization, [role])
            return <td key={column}><span aria-label={label}>{supervisor ? supervisorOptionLabel(data, supervisor) : '未设置'}</span></td>
          }
          const options: Parameters<typeof Select>[0]['options'] = column === 3 ? [{ value: '', label: '请选择' }, { value: '男', label: '男' }, { value: '女', label: '女' }]
            : column === 6 ? [...businessRoleOptions]
            : organizations.filter(option => {
              const unit = data.units.find(candidate => candidate.id === option.value)
              const eligibleRole = cells[6] ? role : defaultPersonnelRole(data.units, data.people, option.value)
              return organizationAllowsRole(eligibleRole, unit?.kind)
            })
          if (value && !options.some(option => option.value === value)) options.push({ value, label: value })
          return <td key={column}>{[3, 6, 7].includes(column)
            ? <Select label={label} value={column === 6 && !value ? role : value} options={options} onChange={next => update(row, column, next)} />
            : <input aria-label={label} value={value} autoComplete="off" spellCheck={false} maxLength={column === 4 ? 18 : column === 5 ? 25 : 128} onChange={event => update(row, column, event.target.value)} />}</td>
        })}<td><button type="button" className="text-button" aria-label={`删除第${row + 1}行`} onClick={() => onChange(rows.length === 1 ? [Array<string>(9).fill('')] : rows.filter((_, index) => index !== row))}>删除</button></td></tr>)}</tbody>
      </table>
    </div>
    <button type="button" className="button secondary" disabled={rows.length >= 100} onClick={() => onChange([...rows, Array<string>(9).fill('')])}>添加一行</button>
  </>
}
