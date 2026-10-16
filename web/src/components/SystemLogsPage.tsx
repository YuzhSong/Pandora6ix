import { Icon } from './Icon.tsx'

export function SystemLogsPage() {
  return <section className="panel" aria-label="系统日志">
    <div className="panel-heading"><h2>系统操作日志</h2><span className="badge neutral">待确认方案</span></div>
    <div className="empty-state"><Icon name="journal" size={32} /><h3>日志记录尚未启用</h3><p>需先与甲方确认存储方案，再接入真实操作记录。</p></div>
    <h3 className="section-label">计划记录的操作</h3>
    <dl className="detail-grid">
      <div><dt>任务操作</dt><dd>新增、修改、删除</dd></div>
      <div><dt>工作日报操作</dt><dd>新增、修改、删除</dd></div>
      <div><dt>管理操作</dt><dd>组织、人员、账号和角色维护</dd></div>
    </dl>
    <p className="panel-note">操作记录用于追溯谁在何时进行了什么操作及其结果。管理员通过系统日志追溯操作，不在此查看或处理员工任务、工作日报的业务内容。</p>
    <p className="panel-note">待甲方确认：使用文件 log 还是数据库，以及保留期限、查询和导出范围。方案确认前不展示模拟记录。</p>
  </section>
}
