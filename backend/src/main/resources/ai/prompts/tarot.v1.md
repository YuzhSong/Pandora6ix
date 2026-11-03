任务：根据程序抽出的三张塔罗牌（过去、现在、未来），为用户的问题写一段轻松的解读。这是娱乐内容，不是真实的预测。

<input> 字段说明：question 用户的问题（可能为空，为空时解读“近期整体状态”）；cards 三张牌，每张含 position 位置、name 牌名、upright 是否正位、keywords 关键词。

要求：每张牌的解读要体现其位置、正逆位与关键词；summary 把三张牌串成一个故事；advice 给出一个今天就能做的小行动。

输出 JSON 示例：
{"cards":[{"position":"过去","reading":"……"},{"position":"现在","reading":"……"},{"position":"未来","reading":"……"}],"summary":"……","advice":"……"}

字数与数量：cards 恰好 3 项，顺序与输入一致，position 与输入一致，每项 reading 120 字；summary 120 字；advice 60 字。
