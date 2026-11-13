package com.pandora6ix.app.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.TaskDto
import com.pandora6ix.app.ui.theme.PandoraTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskSummaryLayoutDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortLandscapeKeepsTitleAtTopAndGivesTableMostOfTheHeight() {
        compose.setContent {
            PandoraTheme {
                Box(Modifier.fillMaxWidth().height(280.dp).testTag("viewport")) {
                    TaskSummaryLayout(true, "mine", {}, {}, {}, true, {}, true) {
                        Box(it.fillMaxWidth().testTag("table"))
                    }
                }
            }
        }
        compose.onNodeWithTag("table").assertHeightIsAtLeast(220.dp)
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("任务汇总").fetchSemanticsNode().boundsInRoot
        assertTrue(title.top - viewport.top < viewport.height * .15f)
        compose.onNodeWithText("我创建的").assertDoesNotExist()
        compose.onNodeWithContentDescription("返回").assertIsDisplayed()
        compose.onNodeWithContentDescription("恢复竖屏").assertIsDisplayed()
        compose.onNodeWithContentDescription("刷新").assertIsDisplayed()
    }

    @Test fun compactScopeMenuSwitchesTheVisibleCollection() {
        compose.setContent {
            PandoraTheme {
                var selected by remember { mutableStateOf("mine") }
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    TaskSummaryLayout(true, selected, { selected = it }, {}, {}, true, {}, true) {
                        Box(it) { Text("scope:$selected") }
                    }
                }
            }
        }
        compose.onNodeWithText("我的任务").performClick()
        compose.onNodeWithText("下属任务").performClick()
        compose.onNodeWithText("scope:managed").assertIsDisplayed()
        compose.onNodeWithText("下属任务").assertIsDisplayed()
    }

    @Test fun shortLandscapeTableScrollsRowsWhileHeaderStaysVisible() {
        val pager = SummaryPager<TaskDto>({ it.id }) {
            SummaryPage((0..30).map { TaskDto(id = "$it", title = "任务 $it", reviewStatus = "APPROVED") }, null)
        }
        runBlocking { pager.load() }
        compose.setContent {
            PandoraTheme {
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    TaskSummaryLayout(true, "mine", {}, {}, {}, true, {}, true) {
                        TaskSummaryTable(pager, { it }, {}, {}, it, showScrollHint = false)
                    }
                }
            }
        }
        compose.onNodeWithTag("summary-task-rows").performScrollToNode(hasText("任务 20"))
        compose.onNodeWithText("任务 20").assertIsDisplayed()
        compose.onNodeWithText("任务名称").assertIsDisplayed()
        compose.onNodeWithText("左右滑动查看更多列，或点右上角横屏查看").assertDoesNotExist()
    }
}
