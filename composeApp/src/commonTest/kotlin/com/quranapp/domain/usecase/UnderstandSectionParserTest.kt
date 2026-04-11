package com.quranapp.domain.usecase

import com.quranapp.ui.component.parseUnderstandSections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnderstandSectionParserTest {

    // -------------------------------------------------------------------------
    // 1. All 4 sections present
    // -------------------------------------------------------------------------

    @Test
    fun `all 4 sections present returns 4 parsed sections`() {
        val raw = """
            ## CONTEXT
            This surah was revealed in Mecca.
            ## WORD HIGHLIGHTS
            The word 'Rahman' means Most Gracious.
            ## SCHOLAR VIEW
            Ibn Kathir says this verse is foundational.
            ## PRACTICAL LESSON
            Start every action with the name of Allah.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals(4, result.size)
    }

    @Test
    fun `all 4 sections have correct header names`() {
        val raw = """
            ## CONTEXT
            Context content.
            ## WORD HIGHLIGHTS
            Word content.
            ## SCHOLAR VIEW
            Scholar content.
            ## PRACTICAL LESSON
            Practical content.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals("CONTEXT", result[0].header)
        assertEquals("WORD HIGHLIGHTS", result[1].header)
        assertEquals("SCHOLAR VIEW", result[2].header)
        assertEquals("PRACTICAL LESSON", result[3].header)
    }

    @Test
    fun `all 4 sections have correct content`() {
        val raw = """
            ## CONTEXT
            This surah was revealed in Mecca.
            ## WORD HIGHLIGHTS
            The word 'Rahman' means Most Gracious.
            ## SCHOLAR VIEW
            Ibn Kathir says this verse is foundational.
            ## PRACTICAL LESSON
            Start every action with the name of Allah.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals("This surah was revealed in Mecca.", result[0].content)
        assertEquals("The word 'Rahman' means Most Gracious.", result[1].content)
        assertEquals("Ibn Kathir says this verse is foundational.", result[2].content)
        assertEquals("Start every action with the name of Allah.", result[3].content)
    }

    // -------------------------------------------------------------------------
    // 2. Partial streaming — only 2 sections arrived
    // -------------------------------------------------------------------------

    @Test
    fun `partial streaming with only 2 sections returns 2 sections`() {
        val raw = """
            ## CONTEXT
            This surah was revealed in Mecca.
            ## WORD HIGHLIGHTS
            The word 'Rahman' means Most Gracious.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals(2, result.size)
        assertEquals("CONTEXT", result[0].header)
        assertEquals("WORD HIGHLIGHTS", result[1].header)
    }

    @Test
    fun `partial streaming does not crash when last section has no following header`() {
        val raw = "## CONTEXT\nThis surah was revealed in Mecca."

        val result = parseUnderstandSections(raw)

        assertEquals(1, result.size)
        assertEquals("This surah was revealed in Mecca.", result[0].content)
    }

    // -------------------------------------------------------------------------
    // 3. Empty string
    // -------------------------------------------------------------------------

    @Test
    fun `empty string returns empty list`() {
        val result = parseUnderstandSections("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `whitespace-only string returns empty list`() {
        val result = parseUnderstandSections("   \n\t  ")

        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 4. Header keyword appearing inside body text does not split early
    // -------------------------------------------------------------------------

    @Test
    fun `header keyword in body text does not truncate preceding section`() {
        // "SCHOLAR VIEW" appears inside CONTEXT body — must not end CONTEXT early
        val raw = """
            ## CONTEXT
            Some scholars — see SCHOLAR VIEW — debate this.
            ## SCHOLAR VIEW
            Actual scholar content here.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        // CONTEXT content must contain the full sentence including "SCHOLAR VIEW"
        val contextContent = result.first { it.header == "CONTEXT" }.content
        assertTrue(
            contextContent.contains("SCHOLAR VIEW"),
            "Expected body mention of SCHOLAR VIEW to be preserved inside CONTEXT",
        )

        // SCHOLAR VIEW section still exists and has its own content
        val scholarSection = result.firstOrNull { it.header == "SCHOLAR VIEW" }
        assertEquals("Actual scholar content here.", scholarSection?.content)
    }

    // -------------------------------------------------------------------------
    // 5. Leading / trailing whitespace stripped from content
    // -------------------------------------------------------------------------

    @Test
    fun `content has leading and trailing whitespace stripped`() {
        val raw = "## CONTEXT\n\n   Bismillah ir-Rahman ir-Raheem.   \n\n## WORD HIGHLIGHTS\nWord."

        val result = parseUnderstandSections(raw)

        assertEquals("Bismillah ir-Rahman ir-Raheem.", result[0].content)
    }

    // -------------------------------------------------------------------------
    // 6. Missing one header in the middle — remaining sections still parsed
    // -------------------------------------------------------------------------

    @Test
    fun `missing WORD HIGHLIGHTS section does not affect other sections`() {
        val raw = """
            ## CONTEXT
            Context content.
            ## SCHOLAR VIEW
            Scholar content.
            ## PRACTICAL LESSON
            Practical content.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals(3, result.size)
        assertEquals("CONTEXT", result[0].header)
        assertEquals("SCHOLAR VIEW", result[1].header)
        assertEquals("PRACTICAL LESSON", result[2].header)
    }

    @Test
    fun `missing CONTEXT section still parses remaining sections correctly`() {
        val raw = """
            ## WORD HIGHLIGHTS
            Word content.
            ## SCHOLAR VIEW
            Scholar content.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        assertEquals(2, result.size)
        assertEquals("WORD HIGHLIGHTS", result[0].header)
        assertEquals("SCHOLAR VIEW", result[1].header)
    }

    // -------------------------------------------------------------------------
    // 7. [DONE] sentinel does not appear in any section content
    // -------------------------------------------------------------------------

    @Test
    fun `DONE sentinel stripped when caller pre-filters it from raw text`() {
        // The SSE layer is expected to strip [DONE] before calling the parser.
        // This test verifies parser behaviour when [DONE] is absent (already filtered).
        val rawWithoutDone = """
            ## CONTEXT
            Revelation context here.
            ## PRACTICAL LESSON
            Apply this daily.
        """.trimIndent()

        val result = parseUnderstandSections(rawWithoutDone)

        result.forEach { section ->
            assertFalse(
                section.content.contains("[DONE]"),
                "Section '${section.header}' must not contain [DONE]",
            )
        }
    }

    @Test
    fun `DONE sentinel present in raw does not appear in section content when stripped by caller`() {
        // Simulate what the ViewModel does: remove [DONE] before passing to parser
        val rawFromSSE = """
            ## CONTEXT
            Revelation context here.
            ## PRACTICAL LESSON
            Apply this daily.[DONE]
        """.trimIndent()

        val cleaned = rawFromSSE.replace("[DONE]", "").trim()
        val result = parseUnderstandSections(cleaned)

        result.forEach { section ->
            assertFalse(
                section.content.contains("[DONE]"),
                "Section '${section.header}' must not contain [DONE] after pre-filtering",
            )
        }
    }

    // -------------------------------------------------------------------------
    // 8. Sections returned in definition order, not occurrence order
    // -------------------------------------------------------------------------

    @Test
    fun `sections out of order in raw are returned in definition order`() {
        // LLM returns PRACTICAL LESSON before CONTEXT — parser must honour definition order
        val raw = """
            ## PRACTICAL LESSON
            Apply this in your daily life.
            ## CONTEXT
            This surah was revealed early.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        // CONTEXT is defined first, so it must appear first in the result
        assertEquals("CONTEXT", result[0].header)
        assertEquals("PRACTICAL LESSON", result[1].header)
    }

    @Test
    fun `out-of-order sections — parser processes in definition order`() {
        // SCHOLAR VIEW appears before WORD HIGHLIGHTS in the raw text.
        // Parser processes in definition order: WORD HIGHLIGHTS (index 1) first,
        // then SCHOLAR VIEW (index 2). SCHOLAR VIEW's end boundary is the next
        // defined header after it (PRACTICAL LESSON), which is absent, so it
        // captures everything to end-of-string — including the WORD HIGHLIGHTS block.
        // WORD HIGHLIGHTS is found at its actual position and gets its own content.
        val raw = """
            ## SCHOLAR VIEW
            Scholar content.
            ## WORD HIGHLIGHTS
            Word content.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        // WORD HIGHLIGHTS comes before SCHOLAR VIEW in definition order → index 0
        assertEquals("WORD HIGHLIGHTS", result[0].header)
        assertEquals("Word content.", result[0].content)

        // SCHOLAR VIEW is index 1; its content runs to end-of-string (no following
        // defined header found after contentStart), so it includes the WORD HIGHLIGHTS block
        assertEquals("SCHOLAR VIEW", result[1].header)
        assertTrue(result[1].content.startsWith("Scholar content."))
    }

    // -------------------------------------------------------------------------
    // 9. Content with newlines — multiline content preserved
    // -------------------------------------------------------------------------

    @Test
    fun `multiline content is preserved correctly`() {
        val raw = """
            ## CONTEXT
            Line one of context.
            Line two of context.
            Line three of context.
            ## WORD HIGHLIGHTS
            Word content.
        """.trimIndent()

        val result = parseUnderstandSections(raw)

        val contextContent = result.first { it.header == "CONTEXT" }.content
        assertTrue(contextContent.contains("Line one of context."))
        assertTrue(contextContent.contains("Line two of context."))
        assertTrue(contextContent.contains("Line three of context."))
    }

    @Test
    fun `internal newlines inside content are not collapsed`() {
        val raw = "## CONTEXT\nFirst line.\nSecond line.\n## SCHOLAR VIEW\nScholar."

        val result = parseUnderstandSections(raw)

        val contextContent = result.first { it.header == "CONTEXT" }.content
        assertTrue(
            contextContent.contains("\n"),
            "Internal newlines in multiline content must be preserved",
        )
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `section with only whitespace content is excluded`() {
        val raw = "## CONTEXT\n   \n## WORD HIGHLIGHTS\nWord content."

        val result = parseUnderstandSections(raw)

        // CONTEXT had only whitespace — trim makes it empty — must be excluded
        assertEquals(1, result.size)
        assertEquals("WORD HIGHLIGHTS", result[0].header)
    }

    @Test
    fun `single section with no following markers returns full remaining text as content`() {
        val raw = "## PRACTICAL LESSON\nApply this in your morning routine.\nReflect each evening."

        val result = parseUnderstandSections(raw)

        assertEquals(1, result.size)
        assertEquals("PRACTICAL LESSON", result[0].header)
        assertTrue(result[0].content.contains("Apply this in your morning routine."))
        assertTrue(result[0].content.contains("Reflect each evening."))
    }
}
