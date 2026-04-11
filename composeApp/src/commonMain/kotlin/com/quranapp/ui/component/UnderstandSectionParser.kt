package com.quranapp.ui.component

data class ParsedSection(val header: String, val content: String)

/**
 * Pure parsing logic extracted from [UnderstandSheet] so it can be unit-tested
 * without Compose or Android dependencies.
 *
 * The LLM response uses bare marker words (CONTEXT, WORD HIGHLIGHTS, …) — it
 * does NOT include the leading `## ` in the search keys, matching the original
 * [parseUnderstandResponse] implementation exactly.
 */
fun parseUnderstandSections(raw: String): List<ParsedSection> {
    val labels = listOf("CONTEXT", "WORD HIGHLIGHTS", "SCHOLAR VIEW", "PRACTICAL LESSON")
    val fullHeaders = labels.map { "## $it" }
    val sections = mutableListOf<ParsedSection>()
    for (i in fullHeaders.indices) {
        val start = raw.indexOf(fullHeaders[i])
        if (start == -1) continue
        val contentStart = start + fullHeaders[i].length
        val end = if (i + 1 < fullHeaders.size) {
            val nextIdx = raw.indexOf(fullHeaders[i + 1], contentStart)
            if (nextIdx == -1) raw.length else nextIdx
        } else raw.length
        val content = raw.substring(contentStart, end).trim()
        if (content.isNotEmpty()) {
            sections.add(ParsedSection(labels[i], content))
        }
    }
    return sections
}
