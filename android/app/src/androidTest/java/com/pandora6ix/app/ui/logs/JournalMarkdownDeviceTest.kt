package com.pandora6ix.app.ui.logs

import android.graphics.Color
import android.text.Spanned
import android.text.style.URLSpan
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class JournalMarkdownDeviceTest {
    private fun render(source: String): Spanned = journalMarkdownRenderer(
        InstrumentationRegistry.getInstrumentation().targetContext, Color.BLUE, Color.WHITE
    ).toMarkdown(journalMarkdownSource(source))

    @Test fun unindentedNoteHasNoListIndentationSpan() {
        for (source in listOf("- 待办\n补充说明", "- [ ] 待办\n补充说明", "1. 待办\n补充说明")) {
            val rendered = render(source)
            val start = rendered.toString().indexOf("补充说明")
            assertTrue(start >= 0)
            assertTrue(rendered.getSpans(start, rendered.length, android.text.style.LeadingMarginSpan::class.java).isEmpty())
        }
    }

    @Test fun ordinaryTextKeepsLineBreaksAndEscapedMarkers() {
        assertEquals("完成设计\n继续联调", render("完成设计\n继续联调").toString())
        assertEquals("*普通文本*", render("\\*普通文本\\*").toString())
    }

    @Test fun listAndParagraphBoundariesDoNotAddBlankLines() {
        for (source in listOf("- 待办\n补充说明", "- [ ] 待办\n补充说明", "1. 待办\n补充说明", "- 待办\n\n补充说明")) {
            assertEquals("待办\n补充说明", render(source).toString())
        }
        assertEquals("父项\n子项\n补充说明", render("- 父项\n  - 子项\n补充说明").toString())
        assertEquals("段落一\n段落二", render("段落一\n\n段落二").toString())
        assertEquals("first\n\nlast", render("```\nfirst\n\nlast\n```").toString())
    }

    @Test fun prefilledTodosBecomeBulletsWithoutChangingSource() {
        val source = "- 完成设计\n- 完成联调\n\n"
        val rendered = render(source)
        assertEquals("完成设计\n完成联调", rendered.toString())
        assertEquals(2, rendered.spanNames().count { it == "BulletListItemSpan" })
        assertEquals("- 完成设计\n- 完成联调\n\n", source)
    }

    @Test fun mixedMarkdownProducesFormattingAndReadOnlyTaskStates() {
        val rendered = render("# 今日工作\n\n**完成**与*计划*及~~取消~~\n\n> 引用\n\n`code`\n\n```\nval done = true\n```\n\n[链接](https://example.com)\n\n- [x] 完成事项\n- [ ] 待办事项")
        val names = rendered.spanNames()
        listOf("HeadingSpan", "StrongEmphasisSpan", "EmphasisSpan", "StrikethroughSpan", "BlockQuoteSpan", "CodeSpan", "CodeBlockSpan").forEach {
            assertTrue("Missing $it in $names", it in names)
        }
        assertEquals(2, names.count { it == "TaskListSpan" })
        assertEquals("https://example.com", rendered.getSpans(0, rendered.length, URLSpan::class.java).single().url)
        assertFalse(rendered.toString().contains("[x]"))
        assertFalse(rendered.toString().contains("[ ]"))
    }

    private fun Spanned.spanNames() = getSpans(0, length, Any::class.java).map { it.javaClass.simpleName }
}
