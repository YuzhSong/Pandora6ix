import type { CredentialHandoff } from './types.ts'
import { availableHandoffAccounts, accountIdentifier } from './accountHandoff.ts'

export function searchDeliveryAccounts(items: CredentialHandoff[], keyword: string) {
  const query = keyword.trim().toLocaleLowerCase()
  return items.filter(item => item.status === 'AVAILABLE' && item.initialPassword !== undefined && (!query || `${item.displayName} ${accountIdentifier(item)}`.toLocaleLowerCase().includes(query)))
}

export async function prepareCredentialDelivery(read: () => Promise<CredentialHandoff[]>, personIds: readonly string[]) {
  const ids = [...new Set(personIds)]
  if (!ids.length) throw new Error('请先选择要交付的账号。')
  const current = new Map(availableHandoffAccounts(await read()).map(account => [account.personId, account]))
  if (ids.some(id => !current.has(id))) throw new Error('部分所选账号的初始密码已不可用，请刷新后重新选择。')
  return ids.map(id => current.get(id)!)
}
