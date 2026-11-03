## Why

需求文档 V1.3 把 AI 分析列为扩展能力（第 10、11、13 页：根据个人信息给出未来方向建议、根据公司信息分析未来走向、MBTI 类测试），Android 第四个一级页面“AI地图”至今只是“功能规划中”占位。2026-11 甲方确认 AI 部分由团队自由设计，只在 Android 展示，定位轻松、娱乐、仅供参考，并同意接入 DeepSeek。本变更把占位页做成“AI 探索馆”，首批五项功能一起交付。

## What Changes

- Android “AI地图”页面改为 AI 探索馆首页：顶部“今日幸运签”入口，下分“工作实验室”（我的职业星图、公司下一站）和“灵感游乐场”（职场人格盲盒、AI 塔罗屋）两组卡片；底部导航名称、顺序和五项结构不变。
- **今日幸运签**：按账号与业务日期确定性生成星级、幸运色、关键词和幸运行动，AI 写当日寄语；可选星座，仅用于当次生成，不保存。
- **职场人格盲盒**：原创情景题库，由程序按维度计分得出原创职场人格，AI 写个性化解读；结果为娱乐标签。
- **AI 塔罗屋**：后端随机抽取三张大阿卡纳牌（过去/现在/未来，含正逆位），AI 结合用户问题解读。
- **我的职业星图**：读取本人近 30 天日志与本人任务，只把脱敏摘要和关键词发给 DeepSeek，生成职业标签、能力方向、三条发展路线和行动建议。
- **公司下一站**：读取当前用户已有权限可见的企业重要事项与任务统计，只发脱敏摘要和关键词，生成高速发展、稳健前进、挑战模式三种情景剧本。
- 后端新增 DeepSeek 网关与 `/api/v1/ai/*` 只读生成接口：API Key 只在服务器环境变量中；统一提示词模板、JSON 结构化输出、结果校验与兜底文案、超时与每人每日调用上限。
- 所有结果页显著标注“娱乐与参考，不构成评价或预测”。

## Capabilities

### New Capabilities
- `ai-explorer-backend`：DeepSeek 接入、提示词与结构化输出规范、数据最小化与权限边界、五项 AI 生成接口、兜底与限流。
- `android-ai-explorer`：Android AI 探索馆首页与五个功能页的交互、加载/失败/兜底状态和免责声明。

### Modified Capabilities

无。日志、任务、导图、权限等既有规范不变；AI 只读取现有接口已授权的数据。

## Impact

- 后端：新增 `service/ai/**`（网关、提示词、摘要构建、各功能服务）、`AiController`、`application.yml` 中 `pandora.deepseek.*` 配置、`backend/openapi.json`、提示词与静态题库/牌库资源、单元与集成测试。**不新增数据库表**，不写业务数据。
- Android：`MainActivity.kt` 中 `AiMapScreen` 替换为 `ui/ai/**` 新页面；新增 `data/AiRepository.kt` 与模型；沿用 Design System Token 与组件。
- Web：不改动。
- 外部依赖：DeepSeek 开放平台（`https://api.deepseek.com`，第三方服务，数据会离开本公司服务器）；不新增 Java/Android 库。
- 运维：云端 `/opt/Pandora/config/backend.env` 增加 `PANDORA_DEEPSEEK_API_KEY` 等变量；密钥不进 Git、APK、GitHub Secrets 或日志。
