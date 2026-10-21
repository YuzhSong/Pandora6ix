# 系统操作日志后端对接清单

本轮只实现 Web。以下为拟定接口契约，需要后端确认并实现，当前不存在这些接口；不代表已完成真实采集。依据 2026-10-20 甲方沟通，历史详情采用完整业务快照，覆盖附件原先“默认不展示正文”的描述。管理员可以只读查看历史内容，不增加任务、日报处理或恢复权限。数据库保存操作记录，程序故障由技术日志排查。

## 1. 采集和数据库要求

- 后端统一覆盖 Web / Android。任务创建、派发、修改、删除、进度更新、提交完成、审核、驳回、验收；工作日报新增、修改、删除；组织与人员维护、账号启停与权限变更、登录失败。现有接口尚不支持的操作，应在对应功能落地时补采集，不能仅为了采集而开放业务操作权限。
- 后端确定操作人、公司、服务端时间、来源、操作结果和请求编号。来源识别规则需与 Android 一起确认，不把来源标记用于权限判断。
- 独立的只追加操作记录，保存当时的操作人名称、对象名称及完整快照；历史对象删除、人员改名或离职不能破坏历史展示。不得通过外键级联删除历史。
- 成功操作和快照在同一事务提交；失败操作无虚构成功版本，失败记录独立持久化，返回适合管理员阅读的失败原因，不返回堆栈。
- 修改保存完整 before/after，新增 before=null，删除 after=null。快照包含当时业务字段和关联显示名称，而不是日后从实时业务表拼接。版本号对应对象历史版本，删除不复用版本号。
- “全部字段”指完整、可授权查看的业务字段，不是数据库所有列：禁止密码、密码哈希、初始密码、令牌、密钥、完整身份证号码等敏感数据；电话号码等个人资料按批准的规则脱敏。字段标签、枚举、日期、人员列表均返回中文可读值，保留必要换行。
- 仅 ADMIN 可查询本人公司数据，详情和历史版本必须逐项校验公司；越权返回 403/404。响应 Cache-Control: no-store，复用现有认证和响应信封。不可仅在前端隐藏字段。
- 保留期、归档和失败日志写入异常的处理策略待甲方最终确认。本轮不实现保留期任务，不默认声称历史已补齐。历史全量版本只能从启用采集之后产生，既有对象可选建立标明时间的基线。

## 2. 日志列表

`GET /api/v1/admin/operation-logs`

查询参数：

| 参数 | 含义 |
| --- | --- |
| keyword | 模糊搜索操作人名称、登录账号、对象显示名称和操作摘要；不搜索快照正文 |
| from / to | ISO 8601 时刻，from 包含，to 不包含；Web 日期按北京时间转换，结束日期包含整天 |
| module / action / source / result | 枚举过滤，空值表示全部 |
| limit | Web 列表固定 20 条，导出逐页 200，后端至少支持 200 |
| cursor | 不透明分页游标，首次不传 |
| snapshotToken | 首次不传，由服务端返回；之后绑定筛选条件继续分页，固定数据集 |

按 occurredAt DESC、稳定唯一序号 DESC 排序。首次冻结结果集，后续复用快照令牌和 total，不能仅用时间戳排序。过期令牌返回 409，前端提示重新加载。导出复用同一列表接口，从第一页遍历全部记录；默认导出摘要，不批量导出业务快照。大数据量可后续增加服务端导出任务，不能静默截断。

以下 JSON 均为现有成功信封中的 `data` 部分，示例仅用于契约说明：

```json
{
  "items": [{
    "id": "log-1025",
    "occurredAt": "2026-10-20T02:30:00Z",
    "actor": { "id": "u-3", "displayName": "张三", "loginName": "zhangsan" },
    "module": "TASK", "action": "UPDATE", "source": "ANDROID", "result": "SUCCESS",
    "object": { "type": "TASK", "id": "task-1024", "label": "任务 #1024" },
    "summary": "修改任务 #1024 的截止日期"
  }],
  "total": 1,
  "nextCursor": null,
  "snapshotToken": "server-generated-opaque-token"
}
```

字段必须显式存在。身份未知的登录失败可 actor.id=null、loginName=null，displayName 使用“未认证用户”；不把攻击者提交的密码或完整请求体写入摘要。技术 ID 和不透明令牌不作为业务字段展示。

## 3. 单条详情及历史版本

`GET /api/v1/admin/operation-logs/{id}`

返回完整列表行字段，另包含：

```json
{
  "requestId": "request-20261020-001",
  "failureReason": null,
  "before": {
    "version": 1,
    "fields": [
      { "key": "title", "label": "任务名称", "value": "准备验收材料" },
      { "key": "deadline", "label": "截止时间", "value": "2026-10-21 18:00" },
      { "key": "description", "label": "任务内容", "value": "整理材料清单\n核对附件" },
      { "key": "assignees", "label": "执行人员", "value": "李四、王五" }
    ]
  },
  "after": {
    "version": 2,
    "fields": [
      { "key": "title", "label": "任务名称", "value": "准备验收材料" },
      { "key": "deadline", "label": "截止时间", "value": "2026-10-22 18:00" },
      { "key": "description", "label": "任务内容", "value": "整理材料清单\n核对附件" },
      { "key": "assignees", "label": "执行人员", "value": "李四、王五" }
    ]
  },
  "versions": [
    { "logId": "log-1025", "version": 2, "occurredAt": "2026-10-20T02:30:00Z", "actorName": "张三", "action": "UPDATE" },
    { "logId": "log-1024", "version": 1, "occurredAt": "2026-10-19T02:00:00Z", "actorName": "李四", "action": "CREATE" }
  ]
}
```

- before/after 的 fields 为全部可展示业务字段，稳定 key，用于比对；label 为中文标签，value 只能是已格式化字符串或 null，不能原样返回关系 ID、枚举或 JSON 对象。
- 字段按业务顺序返回；相同 key 不重复；旧字段移除也要保留在 before。新字段、删除字段、空值变化均可比对。
- versions 为同一对象的完整已保存版本，按新到旧排序，切换版本使用其 logId 请求本接口；包括当前成功记录。失败操作不进入成功版本列表；失败记录可以关联已有版本，但不能虚构新版本。
- 无内容版本的登录、导出、密码重置等操作 before/after=null，versions=[]。失败请求没有成功提交的快照时同样返回 null。
- 必须返回历史快照，不能对象删除后返回 404。仅日志本身不存在或无权访问时返回 404。

## 4. 枚举

- module: TASK / WORK_LOG / ORGANIZATION / PERSONNEL / ACCOUNT / AUTH。
- action: CREATE / UPDATE / DELETE / DISPATCH / PROGRESS / SUBMIT / APPROVE / REJECT / ACCEPT / ENABLE / DISABLE / ROLE_CHANGE / PASSWORD_RESET / LOGIN / EXPORT / DEPARTURE / JOB_CHANGE / MANAGER_REPLACEMENT。
- source: WEB / ANDROID / SYSTEM。
- result: SUCCESS / FAILURE。

新增枚举须同步 Web 中文映射，未知模块/动作临时显示“其他”，不直接展示内部编码。

## 5. 联调验收

1. 两个公司分别操作，只能查到自己公司的记录；员工无访问权限，过期会话退出。
2. Web 和 Android 各创建、修改、删除工作日报，历史完整，变更前后值正确，删除后仍可查。
3. 任务各类已实现操作均被采集，失败不生成成功版本，事务回滚不留下假成功。
4. 人员改名、离职、组织删除之后，旧记录仍显示当时的名称，敏感字段不出现在接口和文件中。
5. 生成超过一页记录，查询筛选和完整 Excel 导出一致；导出期间新增记录不导致重复或遗漏；过期游标提示重试。
6. 不补造上线前不存在的历史版本，不把 Web 的独立测试样例当作采集结果。

## 6. 本地完整页面预览

根据用户授权，开发模式的完整管理端可使用 `?previewLogs=1#system-logs` 查看模拟日志。仅在 Vite DEV 且访问 localhost / 127.0.0.1 / [::1] 并显式传入该参数时启用；页面显示“预览数据”。只有日志列表和详情采用本地样例，其余页面继续使用真实本地后端。没有参数的入口仍请求真实日志接口，失败时不自动切换样例。

样例保存在 web/dev/operationLogPreview.ts，使用 DEV 分支动态导入；正式构建不包含该模块或样例内容。样例不写入数据库，不代表已经采集了真实操作。筛选、固定 20 条翻页、版本对比和 Excel 导出均可在此模式中预览。
