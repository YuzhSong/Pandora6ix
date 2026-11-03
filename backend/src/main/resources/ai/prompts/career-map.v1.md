任务：根据用户近 30 天的工作记录摘要，生成一份“我的职业星图”。这只反映记录中体现出的工作方向，不是对用户能力的评价。

<input> 字段说明：windowDays 统计天数；journalCount 日志篇数；entryCount 工作条目数；journalTags 日志标签与出现次数；tasks 本人任务统计（按状态、优先级、标签）；taskTitles 部分任务标题摘录；entryExcerpts 部分工作条目摘录（已脱敏，“同事”代指他人）。

要求：
- label 是一个有趣的职业角色称号（如“全栈探索者”“项目推进者”）。
- abilities 列出 3-6 个记录中体现的能力方向，level 只能是 HIGH、MEDIUM、LOW、INSUFFICIENT 之一，表示“记录中的体现程度”，evidence 说明来自哪些记录。
- routes 恰好 3 条发展路线，key 依次为 A、B、C，每条给 3 个可执行的小行动。

输出 JSON 示例：
{"label":"全栈探索者","summary":"……","abilities":[{"name":"软件开发","level":"HIGH","evidence":"近 30 天多次记录页面开发与接口联调"}],"routes":[{"key":"A","title":"技术专家","description":"……","actions":["……","……","……"]},{"key":"B","title":"……","description":"……","actions":["……","……","……"]},{"key":"C","title":"……","description":"……","actions":["……","……","……"]}]}

字数：label 12 字；summary 120 字；ability name 12 字、evidence 40 字；route title 12 字、description 80 字、每个 action 30 字。
