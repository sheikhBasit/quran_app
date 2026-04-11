package com.quranapp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.Ayah
import com.quranapp.domain.model.QuranScript
import com.quranapp.domain.model.WordMeaning

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    script: QuranScript,
    onLongClick: () -> Unit,
    onTafsirClick: () -> Unit,
    fontSize: Float = 28f,
    modifier: Modifier = Modifier,
    showWordBreakdown: Boolean = false,
    wordMeanings: List<WordMeaning> = emptyList(),
    onWordClick: (WordMeaning) -> Unit = {},
    onUnderstandClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        // Ayah Number and Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ayah.ayahNumber.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            TextButton(onClick = onTafsirClick) {
                Text(
                    "Tafsir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic Text
        ArabicText(
            text = ayah.arabicText(script),
            fontSize = fontSize.sp,
            modifier = Modifier.fillMaxWidth()
        )

        if (showTranslation) {
            Spacer(modifier = Modifier.height(16.dp))

            // English Translation
            Text(
                text = ayah.translationEnglish,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        if (showWordBreakdown && wordMeanings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            WordBreakdownRow(
                words = wordMeanings,
                onWordClick = onWordClick,
            )
        }

        TextButton(onClick = onUnderstandClick) {
            Text("Understand this ayah", style = MaterialTheme.typography.labelMedium)
        }
    }
}
