任务：为用户写一份“今日幸运签”的文案。

<input> 字段说明：date 日期；zodiac 星座（可能为空）；stars 今日总运势星级 1-5；luckyColor 幸运色；keyword 今日关键词；luckyAction 幸运行动；workStars/socialStars/inspirationStars 分项星级。

要求：文案要和关键词、星级呼应；星级低时也要给出积极的打开方式。

输出 JSON 示例：
{"message":"不必等到万事俱备，今天就可以开始。","workTip":"先把最难的一件事放在上午处理。","socialTip":"主动和同事分享一个小发现。","inspirationTip":"散步时留意身边的新鲜事物。"}

字数上限：message 60 字；workTip、socialTip、inspirationTip 各 40 字。
