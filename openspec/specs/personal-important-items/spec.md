# personal-important-items Specification

## Purpose
描述 Android 低保真原型中个人十大重要事项的编辑入口、新增与数量上限、删除确认、点击文字编辑，以及长按整行拖动排序和拖动时其他条目实时让位的交互要求。
## Requirements
### Requirement: 编辑入口

系统 SHALL 在「个人十大重要事项」面板提供编辑入口，进入个人事项编辑界面。

#### Scenario: 打开编辑器

- **WHEN** 用户点击个人十大重要事项旁的编辑按钮
- **THEN** 打开个人事项编辑界面

### Requirement: 事项编辑

系统 SHALL 以「正式事项 + 候选事项」模型编辑个人十大重要事项：候选事项 SHALL 来自本人最近十个自然日（含今天）已发布日报的完整工作条目；正式事项 SHALL 通过勾选候选产生、通过垃圾桶移回候选，总数不得超过 10 条；SHALL 保留点击正式事项文字编辑内容与长按整行拖动排序的能力；保存后导图页 SHALL 更新。系统 SHALL NOT 提供手动输入新增事项的入口。

#### Scenario: 勾选候选进入正式事项

- **WHEN** 候选区某日报条目的方块被打钩且正式事项未满 10 条
- **THEN** 该条目进入正式事项列表末尾，并从候选区消失

#### Scenario: 正式事项已满时勾选

- **WHEN** 正式事项已有 10 条，用户尝试勾选候选
- **THEN** 方块不可勾选并提示需先移出正式事项

#### Scenario: 删除事项

- **WHEN** 用户点击正式事项右侧垃圾桶并确认弹窗
- **THEN** 该事项移出正式列表并回到候选区（弹窗文案说明可重新打钩恢复），不再提供其他删除入口

#### Scenario: 新增事项

- **WHEN** 用户在候选区为某日报条目打钩
- **THEN** 该条目追加为正式事项且总数不超过 10 条；SHALL NOT 提供手动输入式新增入口

#### Scenario: 点击文字编辑

- **WHEN** 用户点击某一正式事项的文字
- **THEN** 该行进入可编辑状态以修改内容

#### Scenario: 保存后导图更新

- **WHEN** 用户在编辑页完成勾选 / 移出 / 排序后点击「保存」
- **THEN** 导图页「个人十大重要事项」面板内容与编辑结果一致

#### Scenario: 长按拖动调整顺序

- **WHEN** 用户长按某一正式事项整行并上下拖动到目标位置后松手
- **THEN** 该事项移动到目标位置，列表顺序更新

### Requirement: 长按拖动排序

系统 SHALL 支持在编辑界面长按事项整行后上下拖动以调整优先级顺序；拖动过程中被拖行跟随手指，其余行实时让位。

#### Scenario: 长按拖动调整顺序

- **WHEN** 用户长按某一事项整行并上下拖动到目标位置后松手
- **THEN** 该事项移动到目标位置，列表顺序更新

### Requirement: 编辑界面去除冗余图标

编辑界面 SHALL NOT 在删除图标旁显示笔图标；内容编辑通过点击事项文字触发。

#### Scenario: 查看编辑行

- **WHEN** 用户查看编辑界面任一行
- **THEN** 仅显示删除图标，没有笔图标

### Requirement: 候选事项来源

候选事项 SHALL 为本人最近十个自然日（含今天，共 10 个自然日）已发布日报的完整工作条目，按日期倒序展示，且 SHALL 排除已进入正式事项的条目；日志页新发布的日报 SHALL 实时进入候选池。

#### Scenario: 展示候选

- **WHEN** 用户打开编辑页
- **THEN** 正式事项下方以分割线隔开展示全部候选事项，每行左侧为可打钩的圆形（与正式事项序号同尺寸）并附日报日期

#### Scenario: 窗口外日报不进候选

- **WHEN** 某日报的日期早于今天之前第 9 个自然日或晚于今天
- **THEN** 该日报条目不出现在候选区

#### Scenario: 新发布日报实时进入候选

- **WHEN** 用户在日志页发布今日日报后回到编辑页
- **THEN** 该日报条目出现在候选区首位

### Requirement: Stable candidate identity

The prototype SHALL identify each formal personal item by its source log id independently of editable display text. Candidate exclusion SHALL use the source id. Removing and reselecting an item within its eligible date window SHALL restore its edited text during the same application session. Separate source logs with equal content SHALL retain separate identities. The existing ten-item cap and inclusive ten-day source window SHALL remain enforced.

#### Scenario: Edit a selected item
- **WHEN** a user edits the text of a formal personal item
- **THEN** the original source log remains excluded from candidates and cannot be selected twice

#### Scenario: Remove and restore an edited item
- **WHEN** a user moves an edited item back to candidates and selects it again during the same session
- **THEN** its source identity and edited text are restored, including after leaving and revisiting the editor

#### Scenario: Equal text from distinct sources
- **WHEN** two eligible source logs contain identical text and one is selected
- **THEN** only that source is excluded and the other remains selectable

#### Scenario: Date boundary and maximum size
- **WHEN** a source log lies outside today and the preceding nine days, or ten formal items are already selected
- **THEN** the out-of-window log is absent from candidates and an eleventh formal item cannot be added
