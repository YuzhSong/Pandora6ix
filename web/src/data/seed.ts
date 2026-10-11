import type { AdminData } from './types.ts'

export function createSeed(): AdminData {
  return {
    schemaVersion: 2,
    currentUserId: 'u-admin',
    enterprise: { id: 'enterprise-demo', name: '潘多拉科技', version: 1 },
    companyItems: [],
    units: [
      { id: 'org-company', name: '潘多拉科技', kind: 'COMPANY', parentId: null, version: 1 },
      { id: 'org-product', name: '产品研发部', kind: 'DEPARTMENT', parentId: 'org-company', version: 1 },
      { id: 'org-operations', name: '运营管理部', kind: 'DEPARTMENT', parentId: 'org-company', version: 1 },
      { id: 'org-web', name: 'Web 开发组', kind: 'TEAM', parentId: 'org-product', version: 1 },
      { id: 'org-mobile', name: '移动开发组', kind: 'TEAM', parentId: 'org-product', version: 1 },
      { id: 'org-service', name: '客户服务组', kind: 'TEAM', parentId: 'org-operations', version: 1 },
    ],
    people: [
      { id: 'u-admin', displayName: '管理员', loginName: 'admin', jobTitle: '系统管理员', orgUnitId: 'org-company', supervisorId: null, roles: ['ADMIN'], version: 1 },
      { id: 'u-founder', displayName: '林知远', loginName: 'lin.zhiyuan', jobTitle: '企业负责人', orgUnitId: 'org-company', supervisorId: null, roles: ['FOUNDER'], version: 1 },
      { id: 'u-product', displayName: '陈思宁', loginName: 'chen.sining', jobTitle: '研发负责人', orgUnitId: 'org-product', supervisorId: 'u-founder', roles: ['DEPARTMENT_MANAGER'], version: 1 },
      { id: 'u-operations', displayName: '周予安', loginName: 'zhou.yuan', jobTitle: '运营负责人', orgUnitId: 'org-operations', supervisorId: 'u-founder', roles: ['DEPARTMENT_MANAGER'], version: 1 },
      { id: 'u-web', displayName: '许清和', loginName: 'xu.qinghe', jobTitle: 'Web 团队负责人', orgUnitId: 'org-web', supervisorId: 'u-product', roles: ['TEAM_LEAD'], version: 1 },
      { id: 'u-mobile', displayName: '陆星辰', loginName: 'lu.xingchen', jobTitle: '移动团队负责人', orgUnitId: 'org-mobile', supervisorId: 'u-product', roles: ['TEAM_LEAD'], version: 1 },
      { id: 'u-cy', displayName: '程瑶', loginName: 'cheng.yao', jobTitle: '前端开发工程师', orgUnitId: 'org-web', supervisorId: 'u-web', roles: ['EMPLOYEE'], version: 1 },
      { id: 'u-designer', displayName: '苏晚晴', loginName: 'su.wanqing', jobTitle: '产品设计师', orgUnitId: 'org-product', supervisorId: 'u-product', roles: ['EMPLOYEE'], version: 1 },
      { id: 'u-developer', displayName: '江予白', loginName: 'jiang.yubai', jobTitle: 'Android 开发工程师', orgUnitId: 'org-mobile', supervisorId: 'u-mobile', roles: ['EMPLOYEE'], version: 1 },
      { id: 'u-support', displayName: '宋一禾', loginName: 'song.yihe', jobTitle: '客户服务专员', orgUnitId: 'org-service', supervisorId: 'u-operations', roles: ['EMPLOYEE'], version: 1 },
    ],
    tags: [
      { id: 'tag-product', name: '产品研发', colorHex: '#4F70E8', version: 1 },
      { id: 'tag-review', name: '评审协作', colorHex: '#8B5CF6', version: 1 },
      { id: 'tag-customer', name: '客户沟通', colorHex: '#12A88A', version: 1 },
      { id: 'tag-operation', name: '日常运营', colorHex: '#E99B36', version: 1 },
      { id: 'tag-important', name: '重要事项', colorHex: '#E86B73', version: 1 },
    ],
  }
}
