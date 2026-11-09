package com.pandora6ix.app.ui.logs

import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.JournalDraftEntryDto

/** Only initialize on entering a date; an intentionally emptied draft is still authoritative. */
internal fun initialJournalEntries(
    dateKey: String,
    dailyItems: List<DailyItem>,
    draftEntries: List<JournalDraftEntryDto>? = null,
    existingEntries: List<JournalDraftEntryDto>? = null
): List<JournalDraftEntryDto> {
    if (draftEntries != null) return draftEntries
    if (existingEntries != null) return existingEntries
    val bullets = dailyItems.filter { it.date == dateKey }.map { item ->
        "- " + item.content.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\n  ")
    }
    val content = if (bullets.isEmpty()) "" else bullets.joinToString("\n", postfix = "\n\n")
    return listOf(JournalDraftEntryDto(null, content))
}
