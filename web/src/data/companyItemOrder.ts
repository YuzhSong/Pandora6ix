import type { AdminData, AdminRepository, CompanyImportantItem } from './types.ts'

export function sortCompanyItems(items: CompanyImportantItem[]) {
  return [...items].sort((a, b) => a.displayOrder - b.displayOrder || a.id.localeCompare(b.id))
}

export function moveCompanyItem(items: CompanyImportantItem[], id: string, targetIndex: number) {
  const index = items.findIndex(item => item.id === id)
  if (index < 0 || targetIndex < 0 || targetIndex >= items.length) return items
  const next = [...items]
  const [moving] = next.splice(index, 1)
  next.splice(targetIndex, 0, moving)
  return next
}

export class ItemOrderSaveError extends Error {
  completed: number
  constructor(completed: number, cause: unknown) {
    super('排序保存失败，请刷新确认实际顺序。', { cause })
    this.completed = completed
  }
}

export async function saveCompanyItemOrder(repository: Pick<AdminRepository, 'saveCompanyItem'>, ordered: CompanyImportantItem[]) {
  let next: AdminData | null = null
  let completed = 0
  try {
    for (const [index, item] of ordered.entries()) {
      if (item.displayOrder === index + 1) continue
      next = await repository.saveCompanyItem({ title: item.title, content: item.content, displayOrder: index + 1 }, item.id, item.version)
      completed++
    }
    return next
  } catch (cause) { throw new ItemOrderSaveError(completed, cause) }
}
