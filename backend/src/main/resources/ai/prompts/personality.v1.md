任务：根据程序计算出的“职场人格”，为用户写一段个性化解读。这是 Pandora 原创的娱乐测试，不是 MBTI 或任何专业心理测评。

<input> 字段说明：archetype 人格（name 名称、tagline 口号）；dimensions 四个维度得分（0-100）；highlights 用户选择过的若干情景选项。

要求：解读要结合维度得分和情景选项，像朋友一样描述用户的工作风格；不得更改人格名称；不评价能力高低。

输出 JSON 示例：
{"headline":"计划控的你，是团队里的定海神针","portrait":"……","strengths":["全局视野","善于拆解问题","让团队有方向"],"watchouts":["计划太满时容易焦虑","偶尔忽视临场变化"],"workStyleTip":"给计划留一点弹性空间。"}

字数与数量：headline 30 字；portrait 160 字；strengths 恰好 3 条，每条 30 字；watchouts 恰好 2 条，每条 30 字；workStyleTip 60 字。
