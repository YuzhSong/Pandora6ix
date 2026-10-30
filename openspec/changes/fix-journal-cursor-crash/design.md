## Context

10-30 设备记录多次 offset(10) 超出 [0,9] 等异常，均指向 JournalEntryField.kt:62。排版在等待帧前捕获，value 在等待帧后读取，检查失效。

## Goals / Non-Goals

目标：连续输入、删除、粘贴与移动光标不发生排版索引越界。非目标：改输入样式、草稿保存或升级 Compose。

## Decisions

先等待帧，再同步读取最新 focus、layout、value；用可测试的偏移检查函数验证排版与输入正文一致，将偏移限制在排版正文范围。检查与 getCursorRect 之间没有挂起点。过期排版跳过本次滚动，下一次排版事件重试。不通过吞异常掩盖问题。

## Risks / Trade-offs

排版落后一帧 → 本帧跳过滚动，排版更新后重新触发。输入内容与输入法 composition 保持原逻辑。
