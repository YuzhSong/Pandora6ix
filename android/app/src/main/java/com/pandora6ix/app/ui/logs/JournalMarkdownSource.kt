package com.pandora6ix.app.ui.logs

private val journalListMarker = Regex("^ {0,3}(?:[-+*]|[0-9]{1,9}[.)])[ \\t]+")
private val journalFence = Regex("^([ \\t]*)(`{3,}|~{3,})(.*)$")

/** End a list at an unindented note, while keeping the saved/editor source untouched. */
internal fun journalMarkdownSource(source: String): String {
    val result = mutableListOf<String>()
    var inList = false
    var fenceChar: Char? = null
    var fenceLength = 0
    for (line in source.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
        val marker = journalListMarker.find(line)
        // A list item can open a fenced code block on its first line: "- ```".
        val fenceLine = if (marker != null) line.substring(marker.range.last + 1) else line
        val fence = journalFence.matchEntire(fenceLine)
        if (fenceChar != null) {
            // A closing fence cannot have a list marker or non-whitespace suffix.
            if (marker == null && fence != null && fence.groupValues[2].first() == fenceChar &&
                fence.groupValues[2].length >= fenceLength && fence.groupValues[3].isBlank()) {
                fenceChar = null
            }
            result.add(line)
            continue
        }
        if (inList && line.isNotBlank() && !line.first().isWhitespace() && marker == null) {
            if (result.lastOrNull()?.isNotBlank() == true) result.add("")
            inList = false
        }
        if (marker != null) inList = true
        if (fence != null && (fence.groupValues[1].length <= 3 || inList)) {
            val delimiter = fence.groupValues[2]
            // Backtick fence info strings cannot themselves contain backticks.
            if (delimiter.first() != '`' || '`' !in fence.groupValues[3]) {
                fenceChar = delimiter.first()
                fenceLength = delimiter.length
            }
        }
        result.add(line)
    }
    return result.joinToString("\n")
}
