# Pandora6ix development guide

## Scope and guardrails

- Keep `android/`, `web/`, and `backend/` independently buildable.
- Do not add product features, business APIs, database tables, credentials, or infrastructure services without a confirmed requirement and an OpenSpec change.
- Never commit secrets, signing material, or Android `local.properties`.

## OpenSpec workflow

Every non-trivial requirement or behavior change is managed under `openspec/`.

1. Create a kebab-case change with `openspec new change <name>` (or `/opsx:propose` in Codex).
2. Follow `openspec status --change <name> --json` and CLI-provided instructions. Complete proposal, specification deltas, design, and tasks before implementation.
3. Implement approved tasks; run module checks and `openspec validate <name> --strict` before review.
4. Submit a PR linking the issue and OpenSpec change. A non-author reviews ordinary PRs.
5. After merge and validation, run `openspec archive <name>` to update main specs; `--skip-specs` is only for genuine infrastructure, tooling, or document-only changes.

Do not invent OpenSpec commands or hand-create a fake OpenSpec layout. The checked-in `.codex/skills/openspec-*` skills provide Codex entry points.

## Branch conventions

- `main` is the stable integration branch.
- `LoFi` is reserved for Android low-fidelity prototyping; do not create a `prototype/` directory.
- Use focused feature or documentation branches, then a PR into `main`.
- Stakeholders review LoFi work before only reusable code is selectively merged through a PR.

## Commit 细则

### 提交格式

使用 `<type>(<scope>): <summary>`；跨模块且无法明确归属时可省略 scope。

- `type` 使用小写英文，按下表选择。
- `scope` 表示受影响模块或领域，优先使用 `android`、`web`、`backend`、`docs`、`openspec`、`ci`、`deploy`；稳定的业务领域名也可使用，例如 `task`、`journal`。
- `summary` 使用中文或英文均可，以动词描述具体结果，避免“更新代码”“修复问题”等空泛表述；末尾不加句号，标题尽量控制在 72 个字符以内。
- 有兼容性破坏时使用 `<type>(<scope>)!: <summary>`，并在正文添加 `BREAKING CHANGE: ...`，说明影响和迁移方式。此标记不能替代需求确认与 OpenSpec 评审。

| type | 适用内容 |
|---|---|
| `feat` | 新增已确认的功能或行为 |
| `fix` | 修复缺陷 |
| `refactor` | 不改变对外行为的代码重构 |
| `perf` | 性能优化 |
| `style` | 仅格式、空白、导入顺序等调整；界面功能或视觉变化按实际目的使用 `feat` 或 `fix` |
| `test` | 新增或调整测试 |
| `docs` | 文档、规范或 OpenSpec 提案变更 |
| `build` | 构建配置、依赖或打包调整 |
| `ci` | CI 工作流与自动检查调整 |
| `chore` | 不属于以上类型的维护工作 |
| `revert` | 撤销既有提交，正文注明原提交 SHA 与原因 |

### 提交边界与正文

- 一个 commit 只解决一个可独立说明、评审和回退的问题；同一问题所需的实现、测试和文档应一起提交。
- 不混入无关重构、批量格式化、生成文件或其他人的未完成修改。提交前检查暂存区，优先按路径或补丁暂存，不默认使用 `git add .`。
- 非简单变更在标题后空一行写正文：说明原因、关键行为变化、验证命令与结果。未执行或失败的检查必须如实记录原因，不得写成已通过。
- 有对应工作项时，在正文中记录 `Refs: #<issue>` 和 `OpenSpec: <change-name>`；没有 issue 时不虚构编号。仅在确实应关闭 issue 时使用 `Closes: #<issue>`。
- OpenSpec 任务状态应与实际完成情况一致；实现提交不能提前归档尚未合并、验证的变更。

示例（验证结果须按实际执行情况填写）：

```text
fix(android): 修复月历切换后每日事项未刷新的问题

切换月份时重新计算事项范围，避免继续显示上个月的数据。
补充月份边界的回归测试。

Validation: 在此记录实际执行的命令与结果
OpenSpec: <change-name>
```

### 提交前检查

1. 用 `git status --short` 确认工作区状态，用 `git diff --cached` 核对即将提交的完整内容，并运行 `git diff --cached --check` 检查空白错误。
2. 按受影响模块运行必要的构建与测试，命令以各模块 README 和 CI 为准；非简单变更还需运行 `openspec validate <change-name> --strict`。
3. 确认未暂存凭据、密钥、签名材料、Android `local.properties`、本地数据库、日志及临时构建产物；仓库明确跟踪的生成文件按现有流程更新。
4. 不使用 `--no-verify` 绕过检查。修改已推送的提交或重写共享历史前须取得明确授权；不自动执行 force push。
5. 用户仅要求编写或修改文件时，不自动 commit、push 或合并；用户明确要求提交后，仅提交本次任务范围内的文件。
