package com.pandora6ix.app.mock

data class PersonalImportantItem(val sourceLogId: String, val content: String)

fun personalItemCandidates(
    logs: List<WorkLog>,
    items: List<PersonalImportantItem>,
    today: DemoDate,
    user: String = MockData.userName
): List<WorkLog> {
    val selectedIds = items.map { it.sourceLogId }.toSet()
    return logs.filter { it.user == user && it.date >= today.plusDays(-9) && it.date <= today && it.id !in selectedIds }
        .distinctBy { it.id }
        .sortedByDescending { it.date.ordinal() }
}

fun selectPersonalItem(items: MutableList<PersonalImportantItem>, drafts: Map<String, String>, log: WorkLog): Boolean {
    if (items.size >= 10 || items.any { it.sourceLogId == log.id }) return false
    items.add(PersonalImportantItem(log.id, drafts[log.id] ?: log.content))
    return true
}

fun editPersonalItem(items: MutableList<PersonalImportantItem>, drafts: MutableMap<String, String>, index: Int, text: String) {
    val content = text.trim()
    if (content.isEmpty()) return
    val item = items[index].copy(content = content)
    drafts[item.sourceLogId] = content
    items[index] = item
}
