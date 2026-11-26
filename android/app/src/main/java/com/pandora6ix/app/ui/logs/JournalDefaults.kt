package com.pandora6ix.app.ui.logs

import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.JournalDraftEntryDto
import com.pandora6ix.app.data.schedule
import java.time.LocalDate

/** Only initialize on entering a date; an intentionally emptied draft is still authoritative. */
internal fun initialJournalEntries(
    dateKey: String,
    dailyItems: List<DailyItem>,
    draftEntries: List<JournalDraftEntryDto>? = null,
    existingEntries: List<JournalDraftEntryDto>? = null
): List<JournalDraftEntryDto> {
    if (draftEntries != null) return draftEntries
    if (existingEntries != null) return existingEntries
    // 跨日事项在其覆盖期内的每一天都参与预填；日期解析失败时回退为按开始日期匹配
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull()
    val bullets = dailyItems.filter { item ->
        val schedule = item.schedule
        if (schedule != null && date != null) schedule.contains(date) else item.date == dateKey
    }.map { item ->
        "- " + item.content.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\n  ")
    }
    val content = if (bullets.isEmpty()) "" else bullets.joinToString("\n", postfix = "\n\n")
    return listOf(JournalDraftEntryDto(null, content))
}
