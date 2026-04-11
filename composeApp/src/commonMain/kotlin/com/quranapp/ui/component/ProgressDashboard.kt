package com.quranapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.LearningProgress

private const val TOTAL_QURAN_WORDS = 14000
private const val TOTAL_AYAHS = 6236

@Composable
fun ProgressDashboard(
    progress: LearningProgress,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Learning Progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Streak",
                value = "${progress.streakDays}",
                unit = "days",
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6F00),
                        modifier = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Due Today",
                value = "${progress.dueReviewCount}",
                unit = "words",
                modifier = Modifier.weight(1f),
            )
        }

        // Word mastery progress bar
        ProgressRow(
            label = "Words Learned",
            current = progress.wordBankCount,
            total = TOTAL_QURAN_WORDS,
            color = MaterialTheme.colorScheme.primary,
        )

        // Ayahs studied progress bar
        ProgressRow(
            label = "Ayahs Studied",
            current = progress.studiedAyahCount,
            total = TOTAL_AYAHS,
            color = Color(0xFF2E7D32),
        )

        // Review button
        if (progress.dueReviewCount > 0) {
            Button(
                onClick = onStartReview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Review ${progress.dueReviewCount} Due Words")
            }
        }

        // Surah heatmap
        Text(
            "Surah Study Map",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Color shows how deeply each surah has been studied.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        SurahHeatmap(studiedBySurah = progress.studiedBySurah)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon?.invoke()
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun ProgressRow(
    label: String,
    current: Int,
    total: Int,
    color: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$current / $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) current.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

@Composable
private fun SurahHeatmap(studiedBySurah: Map<Int, Int>) {
    val baseColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val maxStudied = studiedBySurah.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items((1..114).toList()) { surahNum ->
            val studied = studiedBySurah[surahNum] ?: 0
            val intensity = if (studied == 0) 0f
                           else (studied.toFloat() / maxStudied).coerceIn(0.15f, 1f)
            val color = if (studied == 0) emptyColor
                       else lerp(emptyColor, baseColor, intensity)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                if (surahNum <= 9) {
                    Text(
                        text = surahNum.toString(),
                        fontSize = 6.sp,
                        color = if (studied > 0) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
