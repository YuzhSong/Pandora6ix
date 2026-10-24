export const roleLabels = {
  ADMIN: '系统管理员',
  FOUNDER: '创始人',
  DEPARTMENT_MANAGER: '部门负责人',
  TEAM_LEAD: '团队负责人',
  EMPLOYEE: '普通员工',
} as const

export type Role = keyof typeof roleLabels
export type UnitKind = 'COMPANY' | 'DEPARTMENT' | 'TEAM'
export const unitLabels: Record<UnitKind, string> = { COMPANY: '公司', DEPARTMENT: '部门', TEAM: '团队' }
export interface Enterprise { id: string; name: string; version: number; loginDomain?: string; businessTimezone?: string | null }
export interface OrgUnit { id: string; name: string; kind: UnitKind; parentId: string | null; version: number }
export interface Person {
  id: string; displayName: string; loginName: string; jobTitle: string
  orgUnitId: string; supervisorId: string | null; roles: Role[]; version: number
  status?: 'ACTIVE' | 'DISABLED'
  employmentStatus?: 'IN_SERVICE' | 'DEPARTED'
  departureDate?: string | null
  gender?: 'MALE' | 'FEMALE' | null
  idNumber?: string | null
  phoneNumber?: string | null
}
export interface Tag { id: string; name: string; colorHex: string; version: number }
export interface CompanyImportantItem { id: string; title: string; content: string; displayOrder: number; publisherUserId: string; version: number }
export interface AdminData { schemaVersion: 2; currentUserId: string; enterprise: Enterprise; units: OrgUnit[]; people: Person[]; tags: Tag[]; companyItems: CompanyImportantItem[] }
export type UnitInput = Omit<OrgUnit, 'id' | 'version'>
export type PersonInput = Omit<Person, 'id' | 'version' | 'status'>
export type TagInput = Omit<Tag, 'id' | 'version'>
export type CompanyItemInput = Pick<CompanyImportantItem, 'title' | 'content' | 'displayOrder'>

export interface AdminRepository {
  replaceManager(personId: string, successorId: string, expectedVersion: number, successorExpectedVersion: number, successorJobTitle: string, formerJobTitle: string): Promise<AdminData>
  departPerson(personId: string, departureDate: string, expectedVersion: number): Promise<AdminData>
  changePersonJob(personId: string, input: JobChangeInput, expectedVersion: number): Promise<AdminData>
  personnelChanges(personId: string): Promise<PersonnelChange[]>
  credentialHandoffs(userId?: string): Promise<CredentialHandoff[]>
  load(): Promise<AdminData>
  getWorkspace(): Promise<AdminData | null>
  initializeEnterprise(name: string, administratorName: string): Promise<AdminData>
  saveEnterprise(name: string, expectedVersion: number): Promise<AdminData>
  saveUnit(input: UnitInput, id?: string, expectedVersion?: number): Promise<AdminData>
  deleteUnit(id: string, expectedVersion: number): Promise<AdminData>
  resetPersonPassword(personId: string, newPassword: string, expectedVersion: number): Promise<AdminData>
  savePerson(input: PersonInput, id?: string, expectedVersion?: number, initialPassword?: string): Promise<AdminData>
  saveRoles(personId: string, roles: Role[], expectedVersion: number): Promise<AdminData>
  saveTag(input: TagInput, id?: string, expectedVersion?: number): Promise<AdminData>
  saveCompanyItem(input: CompanyItemInput, id?: string, expectedVersion?: number, idempotencyKey?: string): Promise<AdminData>
}

export interface JobChangeInput { jobTitle: string; orgUnitId: string; supervisorId: string | null; roles: Role[] }
export interface PersonnelChange { id: string; actorName: string | null; actorLoginName: string | null; recordedAt: string; effectiveDate: string; kind: 'DEPARTURE' | 'JOB_CHANGE'; beforeValues: Record<string, unknown>; afterValues: Record<string, unknown> }

export interface CredentialHandoff {
  personId: string; displayName: string; loginName: string; enterpriseDomain: string
  status: 'AVAILABLE' | 'CHANGED' | 'EXPIRED' | 'UNAVAILABLE'
  initialPassword?: string; expiresAt?: string
}
