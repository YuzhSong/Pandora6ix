# Pandora6ix

Pandora6ix（潘多拉）是面向企业员工与管理者的任务协作与工作日志系统，由 Android 客户端、Web 管理端和后端 API 组成。

员工通过移动端管理任务、记录日报和查看工作安排；管理者在组织权限范围内派发任务、审批申请、跟踪执行与验收结果；管理员通过 Web 端维护企业、组织和人员信息。

## 项目状态

项目已从工程基础进入业务联调与持续完善阶段：

- **Android**：围绕导图、视图、日志、AI地图和我的五个入口，提供任务协作、日报、成员查看与个人设置；任务已覆盖创建、审批、执行和验收闭环。AI地图目前为规划入口。
- **Web**：企业、组织、人员和标签管理，以及业务信息查询与操作记录。
- **Backend**：身份认证与组织权限、任务审批与执行闭环、日报、通知、人员资料和持久化存储。
- **UI 规范**：已完成 Android 全局审计、Design System 和分批改造路线，后续页面统一工作以此为依据。

`dev` 汇集当前开发成果，线上版本随部署节奏更新。

## 技术栈

- Android：Kotlin、Jetpack Compose、Material 3
- Web：React、TypeScript、Vite
- Backend：Java 21、Spring Boot、Maven
- 数据存储：H2 持久化数据库、Flyway 迁移

## 仓库结构

```text
android/   Android 移动客户端
web/       Web 管理端
backend/   后端 API、数据库迁移与接口契约
docs/      产品需求、架构设计、UI 规范与验证文档
openspec/  变更提案、主规范与归档记录
deploy/    部署配置与运维脚本
.github/   CI 与协作模板
```

## 本地开发

三个模块可独立运行，分别使用各自目录中的构建配置。

### Android

使用 Android Studio 打开 `android/`，配置 Android SDK 和 JDK 21，选择 `app` 的 `debug` 变体运行。

```powershell
cd android
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

### Web

```powershell
cd web
npm ci
npm run dev
```

使用 `npm test` 运行测试，`npm run build` 生成生产构建。环境与联调配置见 [Web 开发说明](web/README.md)。

### Backend

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

需要 JDK 21，默认使用本地 H2 文件数据库并在启动时执行 Flyway 迁移。使用 `./mvnw.cmd verify` 运行检查；启动配置见 [后端说明](backend/README.md)，接口契约见 [OpenAPI](backend/openapi.json)。

## 文档入口

- [文档索引](docs/README.md)：产品需求、技术架构、数据库、接口与验证资料。
- [Android UI 审计](docs/UI_AUDIT_REPORT.md)：现有页面与组件的检查结果。
- [Android Design System](docs/ANDROID_UI_DESIGN_SYSTEM.md)：统一视觉、布局和组件规范。
- [UI 改造路线](docs/UI_REFACTOR_ROADMAP.md)：分批统一的优先级与验收方式。
- [OpenSpec 主规范](openspec/specs/)与[变更记录](openspec/changes/)：业务约定及演进过程。

## 开发协作

`main` 为稳定分支，`dev` 为日常集成分支，`LoFi` 用于 Android 低保真原型。开发分支通过 PR 汇入 `dev`，稳定版本再集成到 `main`。

非简单变更先建立 OpenSpec，完成实现和模块验证后提交 PR，经评审合并并归档规范。具体流程见 [AGENTS.md](AGENTS.md)。
