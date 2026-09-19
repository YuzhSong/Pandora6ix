package com.pandora6ix.app.mock

import java.util.Calendar

data class DemoDate(val year: Int, val month: Int, val day: Int) : Comparable<DemoDate> {
    override fun compareTo(other: DemoDate): Int = ordinal().compareTo(other.ordinal())
    fun ordinal(): Int = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.let { (it.timeInMillis / 86_400_000L).toInt() }
    fun plusDays(days: Int): DemoDate = Calendar.getInstance().apply {
        set(year, month - 1, day, 12, 0, 0)
        add(Calendar.DAY_OF_MONTH, days)
    }.let { DemoDate(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH)) }
    fun plusMonths(months: Int): DemoDate = Calendar.getInstance().apply {
        set(year, month - 1, 1, 12, 0, 0)
        add(Calendar.MONTH, months)
    }.let { DemoDate(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, day) }
    fun monthLabel(): String = "${year}年${month}月"
    fun shortLabel(): String = "${month}月${day}日"
    fun weekdayLabel(): String = listOf("一", "二", "三", "四", "五", "六", "日")[((Calendar.getInstance().apply {
        set(year, month - 1, day)
    }.get(Calendar.DAY_OF_WEEK) + 5) % 7)]
    fun shortLabelWithWeekday(): String = "${shortLabel()} 周${weekdayLabel()}"
}

fun daysBetween(start: DemoDate, end: DemoDate): Int = end.ordinal() - start.ordinal()

fun mondayOfWeek(date: DemoDate): DemoDate {
    val calendar = Calendar.getInstance().apply { set(date.year, date.month - 1, date.day) }
    val offset = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return date.plusDays(-offset)
}

data class WorkTask(
    val id: String,
    val title: String,
    val start: DemoDate,
    val end: DemoDate,
    val status: String,
    val assignee: String,
    val colorIndex: Int
)

data class WorkLog(
    val id: String,
    val content: String,
    val user: String,
    val date: DemoDate,
    val time: String
)

object MockData {
    val demoToday = DemoDate(2026, 9, 19)
    const val userName = "林小满"
    const val department = "产品研发部"
    const val position = "产品研发部总经理"
    val teamLeaders = listOf("前端团队长 · 周岚", "后端团队长 · 陈默", "测试团队长 · 许安")

    val companyHighlights = listOf(
        "本周项目验收",
        "完成系统测试",
        "准备阶段性汇报",
        "完善团队知识库",
        "确认季度目标",
        "更新项目风险清单",
        "组织客户回访",
        "发布版本说明",
        "复盘迭代成果",
        "安排下周计划"
    )
    val companyTasks = listOf(
        WorkTask("t1", "完成登录模块", DemoDate(2026, 9, 16), DemoDate(2026, 9, 20), "进行中", "周岚", 0),
        WorkTask("t2", "提交数据库设计", DemoDate(2026, 9, 18), DemoDate(2026, 9, 22), "待开始", "陈默", 1),
        WorkTask("t3", "完成需求评审", DemoDate(2026, 9, 19), DemoDate(2026, 9, 21), "进行中", "许安", 2),
        WorkTask("t4", "准备演示材料", DemoDate(2026, 9, 23), DemoDate(2026, 9, 26), "待开始", "周岚", 3),
        WorkTask("t5", "发布测试版本", DemoDate(2026, 9, 27), DemoDate(2026, 9, 30), "待开始", "陈默", 1),
        WorkTask("t6", "完善埋点方案", DemoDate(2026, 9, 20), DemoDate(2026, 9, 24), "待开始", "许安", 2),
        WorkTask("t7", "编写用户手册", DemoDate(2026, 9, 22), DemoDate(2026, 9, 25), "待开始", "周岚", 3),
        WorkTask("t8", "整理反馈清单", DemoDate(2026, 9, 24), DemoDate(2026, 9, 28), "待开始", "陈默", 0),
        WorkTask("t9", "准备发布检查", DemoDate(2026, 9, 26), DemoDate(2026, 9, 29), "待开始", "许安", 1),
        WorkTask("t10", "版本复盘会议", DemoDate(2026, 9, 29), DemoDate(2026, 10, 1), "待开始", "周岚", 2)
    )
    val logs = listOf(
        WorkLog("l1", "梳理首页四象限交互，确认面板文案。", userName, DemoDate(2026, 9, 19), "09:30"),
        WorkLog("l2", "完成登录模块的 Compose 页面骨架。", userName, DemoDate(2026, 9, 19), "14:10"),
        WorkLog("l3", "同步本周任务进度并记录风险事项。", userName, DemoDate(2026, 9, 18), "17:20"),
        WorkLog("l4", "参与产品需求评审，整理待确认问题。", userName, DemoDate(2026, 9, 17), "11:00"),
        WorkLog("l5", "准备演示数据和移动端测试环境。", userName, DemoDate(2026, 9, 16), "16:45"),
        WorkLog("l6", "完成周报初稿并发送给项目负责人。", userName, DemoDate(2026, 9, 15), "18:00"),
        WorkLog("l7", "检查 Android Studio 和模拟器配置。", userName, DemoDate(2026, 9, 14), "10:20"),
        WorkLog("l8", "整理甲方反馈并标记待确认项。", userName, DemoDate(2026, 9, 13), "15:40"),
        WorkLog("l9", "补充演示流程和讲解提纲。", userName, DemoDate(2026, 9, 12), "13:10"),
        WorkLog("l10", "完成本轮低保真页面走查。", userName, DemoDate(2026, 9, 11), "17:35")
    )
    val personalImportant = logs.take(5).map { it.content }
}
