package com.pandora6ix.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.DailyItemsStore
import com.pandora6ix.app.ui.theme.PandoraTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DailyItemsDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val items = mutableStateListOf<DailyItem>()

    @Test fun addValidatesTimeAndDetailsCanEditContentAndAllDayMode() {
        showPanel()
        compose.onNodeWithContentDescription("添加事项").performClick()
        compose.onNodeWithText("事项内容").performTextInput("准备材料")
        selectEndHour("09")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("结束时间必须晚于开始时间").assertIsDisplayed()
        compose.runOnIdle { assertTrue(items.isEmpty()) }

        selectEndHour("10")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("准备材料").performClick()
        compose.onNodeWithText("事项详情").assertIsDisplayed()
        val id = compose.runOnIdle { items.single().id }
        compose.onNodeWithText("编辑").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("整理材料")
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("整理材料").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()

        compose.runOnIdle {
            val edited = items.single()
            assertEquals(id, edited.id)
            assertEquals("整理材料", edited.content)
            assertNull(edited.startMinute)
            assertNull(edited.endMinute)
        }
    }

    @Test fun longPressOnContinuationMovesTheWholeRange() {
        val monday = YearMonth.now().atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        val tuesday = monday.plusDays(1)
        val thursday = monday.plusDays(3)
        val original = DailyItem("span", monday.toString(), "跨日事项",
            endDate = tuesday.toString(), startMinute = 1320, endMinute = 60)
        items.add(original)
        showPanel()

        val bar = compose.onNodeWithText("跨日事项").fetchSemanticsNode().boundsInRoot
        val grab = Offset(bar.left + bar.width * 0.75f, bar.center.y)
        val drop = dayNode(thursday).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(grab)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            for (step in 1..10) {
                moveTo(grab + (drop - grab) * (step / 10f), delayMillis = 30)
            }
            up()
        }

        compose.runOnIdle {
            assertEquals(original.copy(date = thursday.minusDays(1).toString(), endDate = thursday.toString()), items.single())
        }
        dayNode(thursday).performClick()
        compose.onNodeWithText("跨日事项").assertIsDisplayed()
        compose.onNodeWithText("完成").performClick()
        dayNode(monday).performClick()
        compose.onNodeWithText("当日暂无事项").assertIsDisplayed()
    }

    @Test fun savedRangeSurvivesReloadAndIsIsolatedByOwner() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = "daily-items-test-${UUID.randomUUID()}"
        val item = DailyItem("one", "2026-11-30", "跨日", done = true,
            endDate = "2026-12-01", startMinute = 1320, endMinute = 60)
        try {
            DailyItemsStore(context, owner).save(listOf(item))
            assertEquals(listOf(item), DailyItemsStore(context, owner).load())
            assertTrue(DailyItemsStore(context, "$owner-other").load().isEmpty())
        } finally {
            context.deleteSharedPreferences("daily_items_$owner")
        }
    }

    private fun selectEndHour(hour: String) {
        compose.onNode(hasText("结束 ", substring = true) and hasClickAction()).performClick()
        compose.onNodeWithText(hour).performClick()
        compose.onNodeWithText("确定").performClick()
    }

    private fun dayNode(date: LocalDate) = compose.onNode(
        SemanticsMatcher("Calendar day $date") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label ==
                "查看${date.monthValue}月${date.dayOfMonth}日事项"
        }
    )

    private fun showPanel() {
        compose.setContent {
            PandoraTheme {
                DailyItemsPanel(
                    modifier = Modifier.fillMaxSize(),
                    items = items,
                    onSave = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index < 0) items.add(item) else items[index] = item
                    },
                    onToggle = {},
                    onDelete = {}
                )
            }
        }
    }
}
