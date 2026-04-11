package com.quranapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class UnderstandSection(
    val header: String,
    val icon: ImageVector,
    val color: Color,
    val content: String,
)

/** Parses the 4-section streaming response into structured cards. */
private fun parseUnderstandResponse(raw: String): List<UnderstandSection> {
    val headers = listOf("CONTEXT", "WORD HIGHLIGHTS", "SCHOLAR VIEW", "PRACTICAL LESSON")
    val icons = listOf(
        Icons.Default.History,
        Icons.Default.Spellcheck,
        Icons.Default.MenuBook,
        Icons.Default.Lightbulb,
    )
    val colors = listOf(
        Color(0xFF1565C0), // blue  — Context
        Color(0xFF2E7D32), // green — Word Highlights
        Color(0xFF6A1B9A), // purple — Scholar View
        Color(0xFFE65100), // orange — Practical Lesson
    )
    val sections = mutableListOf<UnderstandSection>()
    for (i in headers.indices) {
        val start = raw.indexOf(headers[i])
        if (start == -1) continue
        val contentStart = start + headers[i].length
        val end = if (i + 1 < headers.size) {
            val nextIdx = raw.indexOf(headers[i + 1])
            if (nextIdx == -1) raw.length else nextIdx
        } else raw.length
        val content = raw.substring(contentStart, end).trim()
        if (content.isNotEmpty()) {
            sections.add(
                UnderstandSection(
                    header = headers[i].lowercase().replaceFirstChar { it.uppercase() },
                    icon = icons[i],
                    color = colors[i],
                    content = content,
                )
            )
        }
    }
    return sections
}

@Composable
fun UnderstandSheet(
    streamText: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    MaterialTheme.shapes.small,
                )
        )

        Text(
            text = "Understanding This Ayah",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        when {
            error != null -> {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                )
            }
            isLoading && streamText.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Loading explanation...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            streamText.isNotEmpty() -> {
                val sections = parseUnderstandResponse(streamText)
                if (sections.isEmpty()) {
                    // Still streaming first section — show raw text
                    Text(
                        text = streamText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                    )
                } else {
                    sections.forEach { section ->
                        UnderstandSectionCard(section = section)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (isLoading) {
                        Text(
                            text = "...",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun UnderstandSectionCard(section: UnderstandSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = section.color.copy(alpha = 0.08f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.color,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = section.header,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = section.color,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }
    }
}
