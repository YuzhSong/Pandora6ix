## 1. 运维与配置

- [x] 1.1 云端 `/opt/Pandora/config/backend.env` 写入 `PANDORA_DEEPSEEK_API_KEY`（root-only，不入 Git），验证服务器可访问 `https://api.deepseek.com`
  - 2026-11-03 运维完成：仅新增/更新 `PANDORA_DEEPSEEK_API_KEY`、`PANDORA_DEEPSEEK_BASE_URL`、`PANDORA_DEEPSEEK_MODEL`，原文件备份为 `backend.env.bak-20261103-115923-294490`，权限 root:root 0600，服务器请求 `/models` 返回 200；未重启后端。
- [x] 1.2 `application.yml` 增加 `pandora.deepseek.*` 与 `pandora.ai.daily-quota` 绑定；`deploy/README.md` 补充变量说明
- [x] 1.3 `.gitignore` 忽略本地密钥文件 `deepseek.md`

## 2. 后端基础

- [x] 2.1 `DeepSeekGateway`：JSON 模式请求、超时、1 次重试、未配置降级、安全日志
- [x] 2.2 提示词资源 `ai/prompts/_rules.v1.md` 与五个功能模板；模板加载与 `<input>` 数据封装
- [x] 2.3 输出校验器（必填字段、数组数量、长度截断）与统一响应包装（aiGenerated/quotaExceeded/disclaimer）
- [x] 2.4 每人每日内存限流与幸运签缓存
- [x] 2.5 `AiRedactor` 脱敏与 `AiDigestBuilder`（职业星图、公司下一站，复用现有读取与权限）

## 3. 后端功能与接口

- [x] 3.1 幸运签：确定性表与 hash 选择、兜底寄语
- [x] 3.2 人格盲盒：原创 12 题题库、4 维计分、8 种原创人格与兜底解读
- [x] 3.3 塔罗：22 张大阿卡纳牌库、SecureRandom 抽牌、正逆位、兜底释义
- [x] 3.4 职业星图与公司下一站服务（无数据时不调用模型）
- [x] 3.5 `AiController` 六个接口，鉴权与错误契约；更新 `backend/openapi.json`
  - 生成器按控制器文件名排序，新增 `AiController` 使 openapi.json 路径顺序整体前移；语义比对确认既有 69 个接口与全部既有 schema 无变化，新增 6 个路径、8 个 schema。
- [x] 3.6 测试：网关降级/超时/坏 JSON 兜底（Mock HTTP）、计分确定性、抽牌不重复、脱敏、权限范围、401、限流
  - `AiInfrastructureTests` 6 项、`AiExplorerIntegrationTests` 7 项（本地假 DeepSeek 服务，校验发送内容不含同事姓名、手机号、邮箱、网址与密钥）。

## 4. Android

- [x] 4.1 `AiModels.kt` 与 `AiRepository.kt`（`/ai/*` 读超时 45s）
- [x] 4.2 `AiExplorerScreen` 首页替换 `AiMapScreen`，底部导航不变
- [x] 4.3 今日幸运签页（可选星座）
- [x] 4.4 职场人格盲盒（逐题、进度、结果卡、本机保存最近结果）
- [x] 4.5 AI 塔罗屋（问题输入与敏感信息提示、三张翻牌动画、解读卡）
- [x] 4.6 我的职业星图与公司下一站（首次数据说明确认、结构化卡片、路线/情景展开）
- [x] 4.7 加载、兜底提示、限额提示、网络失败重试、系统返回回首页、免责声明
- [x] 4.8 单元测试：模型解析、状态映射；五主题与 360dp 截图检查
  - `AiModelsTest` 5 项。截图：暖米、薄荷两套主题下首页与各功能页，360dp 首页；其余三套主题未逐页截图（页面只使用语义 Token）。

## 5. 验证与交付

- [x] 5.1 `./mvnw test`、`./gradlew.bat :app:assembleDebug :app:testDebugUnitTest` 通过
  - 后端全量 166 项中 165 项通过；`journalEditWindowIgnoresCompanyTimezoneChanges` 在全量运行中失败一次、单独重跑通过，属既有的时间敏感用例，与本变更无关。Android 127 项单测全部通过。
- [x] 5.2 `openspec validate add-android-ai-explorer --strict` 通过
- [x] 5.3 本地无密钥时五项功能均返回兜底内容；配置密钥后真实调用验证（不记录密钥）
  - 无密钥兜底由集成/单元测试覆盖；本地隔离后端接入真实 DeepSeek，五项功能首次调用均成功，耗时 1.3–4.4 秒，幸运签同日重复请求命中缓存。
- [ ] 5.4 PR 合入 dev 后在云端验证五项功能，归档变更
