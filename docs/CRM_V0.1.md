# Pandora6ix 二期 CRM V0.1

## 本机运行

| 项目 | 二期设置 |
| --- | --- |
| 源码 | `dev2`，起点为 `origin/dev` 的 `000bf2c840d9b8d0043a1a691ca94138ee3bb4e4` |
| 后端 | `http://127.0.0.1:19082`，Spring `dev2` profile |
| Web | `http://127.0.0.1:15182`，Vite `dev2` mode |
| Android | `com.pandora6ix.app.dev2`，模拟器访问 `http://10.0.2.2:19082/api/v1` |
| 数据库 | 当前用户目录下 `.pandora6ix-dev2/data/crm.mv.db` |
| 合同附件 | 当前用户目录下 `.pandora6ix-dev2/attachments/` |
| 虚构账号、运行日志 | 当前用户目录下 `.pandora6ix-dev2/run/` |

需要 Java 21、Node.js、Python 3.12+ 和 Android SDK 35。分别在以下目录执行。

```powershell
# backend
.\mvnw.cmd verify

# 仓库根目录，在独立终端保持运行
python deploy/crm_demo.py

# web
npm.cmd ci
npm.cmd run dev -- --mode dev2

# android
.\gradlew.bat :app:assembleDev2
```

启动器使用现有企业和员工 API 创建虚构组织与三个账号。打开本机 `.pandora6ix-dev2/run/demo-access.json` 可查看生成的企业域和密码。Web 录入用 `admin@<企业域>`，Android 登录分别填写企业域、登录名 `staff` 或 `observer`、密码。虚构员工的默认密码由每次独立初始化随机生成。服务运行期间在启动终端用 Ctrl+C 关停，等待 Java 正常退出再做备份。首次初始化使用随机启动密钥，后续启动会关闭公共初始化。

安装 `android/app/build/outputs/apk/dev2/app-dev2.apk` 到 Android Studio 模拟器。仓库原有 debug 构建继续保留一期地址。使用已有 Android SDK 时，可通过 `ANDROID_HOME` 指定路径。本机编译时先关闭模拟器，并使用以下内存配置。若环境内存不同，可调整堆上限。

```powershell
.\gradlew.bat :app:assembleDev2 --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.jvmargs=-Xmx4400m -Dfile.encoding=UTF-8'
```

本轮部署目标为本机演示。二期 profile 固定数据库位置，默认监听回环地址。Vite 二期模式固定代理到二期后端，生产打包可用 `npm.cmd run build -- --mode dev2`。如另行获批部署，需在独立用户及存储目录下运行二期后端并为 Web 配置同源的 `/api/v1` 反代，同时重新设置 Android 演示构建地址。现有 `deploy-dev.yml` 只监听 `dev` 的发布。为二期备份时一起复制停服后的文件库及附件目录。

## 演示验收

1. Web 管理员进入「CRM 管理」，新建虚构企业客户，将负责人选为虚构员工林晨。
2. 在「业务记录」新建沙龙，选择上一步客户，将林晨加入参与员工并填写「现场接待」。打开详情核对业务状态及参与关系，回到客户详情核对业务历史。
3. Android 用 `staff` 登录，在「我的」进入「我参与的业务」和「我的客户」，查看沙龙职责和本人客户。
4. 在「会员权益」登记年卡及有效期，新增权益和服务记录，将服务状态更新为已完成。
5. 在「劳动合同」新建林晨的虚构合同，将到期日设在未来 30 天内。上传虚构 PDF，再通过临期筛选、合同详情及下载验证。
6. 使用 `observer` 登录。其本人目录为空，请求上述客户、业务、合同详情及附件均返回 404。用另一企业账号重复验证可确认企业边界。

## 数据库变更

CRM 迁移位于 `backend/src/main/resources/db/crm/`，仅二期 profile 加载。Flyway 的历史 `V1–V17` 继续使用原校验和。

| 迁移 | 对象与关系 |
| --- | --- |
| V18 | `crm_customer`，客户主档、企业与负责人 |
| V19 | `crm_business`、`crm_participant`，业务链接一个客户、一名主负责人及多名员工 |
| V20 | `crm_membership`、`crm_benefit`、`crm_benefit_use`，会员资格、权益和逐次服务 |
| V21 | `crm_employment_contract`，员工合同期限、附件引用及版本 |

CRM 关联使用企业复合外键和现有 `app_user`，离职人员的历史记录可保留。编辑业务时一次事务更新记录及参与人；附件替换先持久化新文件，再提交数据库引用，提交后清理旧文件。停服后联合备份数据库和附件。

## API 与数据权限

前缀 `/api/v1/crm`。除 PDF 字节下载外，成功体为 `{data, requestId}`。请求使用一期的 Bearer 会话，企业及用户由服务端解析。编辑提交 `version`，并发修改返回 409。空结果返回空数组，列表每页最多 100 条，使用从 0 开始的 `offset`。

| 资源 | 方法 | 字段与筛选 |
| --- | --- | --- |
| `/capabilities`、`/people` | GET | 当前用户管理能力；本企业在职负责人及参与人候选 |
| `/customers`、`/customers/{id}` | GET、POST、PUT | `name,kind,category,contact,notes,ownerUserId`；按 `q,kind,mine` 筛选 |
| `/businesses`、`/businesses/{id}` | GET、POST、PUT | `title,kind,status,eventDate,eventTime,customerId,ownerUserId,notes,participants[{userId,responsibility}]`；按 `q,kind,status,customerId,mine` 筛选 |
| `/memberships`、`/memberships/{id}` | GET、POST、PUT | `customerId,membershipType,startDate,endDate`；按客户名 `q` 或 `customerId` 筛选 |
| `/memberships/{id}/benefits`、`/memberships/{id}/benefits/{benefit}` | GET 列表、POST、PUT | `name,notes` |
| `/benefits/{id}/uses`、`/benefits/{id}/uses/{use}` | GET 列表、POST、PUT | `serviceDate,status,notes` |
| `/contracts`、`/contracts/{id}` | GET、POST、PUT | `employeeUserId,title,startDate,endDate,notes`；按员工名 `q` 或 `expiry` 筛选 |
| `/contracts/{id}/attachment` | GET、PUT | 下载响应为 PDF；上传为 multipart `file` 和 `version`，最多 10 MiB |

客户类型为 `PERSON/COMPANY`。业务类型为 `MALL/PERFORMANCE/PRIVATE_DINING/VISIT/SALON/OTHER`，状态为 `PLANNED/IN_PROGRESS/COMPLETED/CANCELLED`。服务状态为 `SCHEDULED/COMPLETED/CANCELLED`。日期格式 `YYYY-MM-DD`，可选开始时间使用 `HH:mm`，均按企业业务口径展示。合同状态为 `UPCOMING/ACTIVE/EXPIRING/EXPIRED`。

| 身份 | 客户及会员 | 业务 | 劳动合同及附件 |
| --- | --- | --- | --- |
| 本企业 ADMIN | 读写本企业记录 | 读写本企业记录 | 读写本企业记录 |
| 本企业 FOUNDER | 读写本企业记录 | 读写本企业记录 | 读取本人合同 |
| 部门经理、组长 | 本人及现有组织管理范围内负责的记录 | 本人负责或参与，以及管理范围内员工负责的记录 | 读取本人合同 |
| 普通员工 | 本人负责的记录 | 本人负责或参与的记录 | 读取本人合同 |
| 其他企业用户 | 统一 404 | 统一 404 | 统一 404 |

管理页沿用 Web 的 ADMIN 登录规则。Android 查询入口以本人范围为主，员工自助合同界面留待确认。业务详情可显示关联客户名称供执行员工识别，客户档案、联系方式和会员记录另行鉴权。业务备注对获准查看该业务的员工可见。操作日志记录 CRM 创建、编辑及附件上传，保留操作者、目标 ID 和版本。联系方式、合同内容和附件字节只从对应受控业务接口读取。

## 待甲方确认

- 是否拆分创始人、股东、运营合伙人、资源合伙人、行政与财务的数据范围，哪些人可看客户联系方式和劳动合同。
- 是否需要同一业务关联多位客户，是否要给跨部门项目中的业务负责人或参与人额外授予客户权限。
- 会员有效期、续期、权益余额和扣减规则，以及商城会员、订单的对接字段与同步方式。当前先手工登记资格和每次服务。
- 劳动合同类别、附件格式、续签办法、临期天数、通知对象及渠道。现阶段使用按企业业务日计算的 30 天页面提醒。
- 客户合并、业务和合同历史留存、员工自助界面，以及活动参与记录如何进入人工绩效流程。支付、财务结算、客户商业合同和自动绩效属于以后单独评估的范围。

## 验证命令

```powershell
# 仓库根目录
openspec validate add-phase2-crm --strict
python backend/scripts/generate-openapi.py --check

# backend
.\mvnw.cmd verify

# web
npm.cmd run lint
npm.cmd test
npm.cmd run build
npm.cmd run build -- --mode dev2

# android
.\gradlew.bat :app:assembleDev2 :app:testDebugUnitTest
```


## 验证结果

| 项目 | 实测结果 |
| --- | --- |
| 二期文件库 | 独立文件库从空库迁移到 V21。临时 V17 文件库追加四条迁移后保留企业、员工和标签，再次迁移执行 0 条 |
| 一期后端边界 | 默认 profile 独立启动只执行 V1–V17，`/health` 200，未认证请求 `/api/v1/me` 为 401，CRM 为 404。既有 OpenAPI 81 个操作校验通过 |
| 后端测试 | `verify` 全量 190 项通过。补充部门经理范围测试后，CRM、迁移和隔离共 8 项定向测试通过 |
| Web | 两种模式均构建成功，161 项测试通过，lint 无错误并保留已有警告。使用本机 Chromium 和 Playwright 验证四个管理页面、390px 表单、CRM 日志筛选与 PDF 下载字节 |
| Android | `assembleDev2` 成功。API 35 模拟器使用虚构员工账号，看到了同一条 Web 业务、参与职责和本人客户 |
| 越权 | 无关员工读取客户、业务、合同及附件返回 404；集成测试检查跨企业、组织范围、合同本人权限、版本冲突和附件替换 |
| Android 一期单测 | `dev2` 的 `testDebugUnitTest` 执行 192 项，191 通过，`DesignTokensTest.statusMappingFollowsDesignSystem` 失败。进行中状态期望 `INFO`，实际为 `NEUTRAL` |

本轮仅用虚构数据演示。当前分支保留 OpenSpec 变更供评审和甲方核对。

## 分模块提交记录

| 模块 | 提交 |
| --- | --- |
| OpenSpec、隔离及客户 API | `bdaf2c5` |
| 业务及员工参与 | `96342fa` |
| Web 客户及业务 | `2ef0ef9`、`08f3046` |
| Android 本人查询 | `4ab26b8` |
| 会员权益和劳动合同 | `b510fd0`、`864ae33`、`e4f6e69` |
| Web 操作日志 | `d6e53b2` |
| 迁移及权限回归 | `e30ab00` |
| 本机启动器 | `6170471` |
