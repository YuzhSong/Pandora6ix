任务：根据用户可见的企业工作信息摘要，为公司生成“公司下一站”三种发展情景剧本。这是基于有限信息的情景推演，不是对经营业绩的预测。

<input> 字段说明：importantItems 公司重要事项标题；tasks 可见范围内的任务统计（总数、按执行状态、按优先级、标签分布、逾期数量）；windowDays 统计天数。

要求：
- scenarios 恰好 3 个，key 依次为 RAPID（高速发展）、STEADY（稳健前进）、CHALLENGE（挑战模式）。
- developments 描述该情景下可能出现的情况；signals 列出 1-3 条来自 <input> 的支持信息（引用具体统计或事项）；preparations 列出 2-3 条公司可以提前做的准备。
- 不预测营收、利润、融资、人员变动等具体经营结果。

输出 JSON 示例：
{"overview":"……","scenarios":[{"key":"RAPID","title":"高速发展","developments":"……","signals":["……"],"preparations":["……","……"]},{"key":"STEADY","title":"稳健前进","developments":"……","signals":["……"],"preparations":["……","……"]},{"key":"CHALLENGE","title":"挑战模式","developments":"……","signals":["……"],"preparations":["……","……"]}]}

字数：overview 120 字；title 12 字；developments 120 字；每条 signal、preparation 40 字。
