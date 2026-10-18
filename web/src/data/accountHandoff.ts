export interface AccountLogin { loginName: string; enterpriseDomain?: string }
export interface CreatedAccountReceipt extends AccountLogin { personId: string; displayName: string; initialPassword: string; reason?: 'created' | 'reset' }

export function accountLoginText(account: AccountLogin, initialPassword?: string): string {
  return [
    ...(account.enterpriseDomain ? [`公司登录标识：${account.enterpriseDomain}`] : []),
    `登录账号：${account.loginName}`,
    ...(initialPassword === undefined ? [] : [`初始密码：${initialPassword}`]),
  ].join('\n')
}

export function credentialListText(accounts: CreatedAccountReceipt[]) {
  return accounts.map(account => `姓名：${account.displayName}\n${accountLoginText(account, account.initialPassword)}`).join('\n\n')
}
