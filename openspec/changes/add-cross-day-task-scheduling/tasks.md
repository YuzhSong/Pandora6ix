# Tasks: add-cross-day-task-scheduling

## 1. 服务端放开跨天

- [x] 1.1 `TaskService.time()`：区间改为 `startDate+startTime` → `endDate+endTime`，允许跨天
- [x] 1.2 同日仍要求结束晚于开始，违反由 409 政策改为 422 `VALIDATION_FAILED`
- [x] 1.3 ALL_DAY 跨多天保持支持（不带时刻）

## 2. 测试

- [x] 2.1 改写旧断言：跨天 TIMED 由「期望 409」改为「期望 201」
- [x] 2.2 同日同刻 / 不递增改为期望 422
- [x] 2.3 新增用例：ALL_DAY 跨多天落库（end_date 正确、分钟为空）
- [x] 2.4 运行 `mvn test` 全量通过

## 3. 移动端提示

- [x] 3.1 删除「暂不支持跨天任务」的 Q-04 映射
- [x] 3.2 新增 `VALIDATION_FAILED` 提示（同日不递增 / 日期倒置 / HH:mm / ALL_DAY 不带时刻）
- [x] 3.3 「补充今日工作」按钮颜色与「回到今年」统一为 `ThemeButton`

## 4. 文档

- [x] 4.1 `backend/README.md` Q-04 行改为「跨天已确认放开」
- [x] 4.2 `backend/ANDROID_INTEGRATION.md` Q-04 行同步
- [ ] 4.3 需求文档 V1.3 的 Q-04 描述由甲方更新（本次不擅自改动甲方确认稿）

## 5. 验证

- [x] 5.1 后端测试通过
- [x] 5.2 `:app:compileDebugKotlin` 通过
- [ ] 5.3 真机验收：跨天分钟任务可提交并在日历分段显示
