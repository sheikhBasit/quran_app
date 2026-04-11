package com.quranapp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.domain.model.DueFlashcard
import com.quranapp.domain.model.ReviewRating

@Composable
fun FlashcardSession(
    cards: List<DueFlashcard>,
    currentIndex: Int,
    showAnswer: Boolean,
    sessionComplete: Boolean,
    sessionCorrect: Int,
    sessionTotal: Int,
    onReveal: () -> Unit,
    onRating: (ReviewRating) -> Unit,
    onDismiss: () -> Unit,
) {
    if (sessionComplete) {
        FlashcardSummary(
            correct = sessionCorrect,
            total = sessionTotal,
            onDismiss = onDismiss,
        )
        return
    }

    val card = cards.getOrNull(currentIndex) ?: run {
        // No cards due
        FlashcardSummary(correct = 0, total = 0, onDismiss = onDismiss)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text("Exit") }
            Text(
                text = "${currentIndex + 1} / $sessionTotal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(64.dp))
        }

        LinearProgressIndicator(
            progress = { if (sessionTotal > 0) currentIndex.toFloat() / sessionTotal else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Flash card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = card.arabicWord,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = card.transliteration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                AnimatedVisibility(visible = showAnswer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = card.englishMeaning,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Surah ${card.surahNumber}:${card.ayahNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        if (!showAnswer) {
            Button(
                onClick = onReveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Reveal Answer", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onRating(ReviewRating.AGAIN) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Again", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Again")
                }
                OutlinedButton(
                    onClick = { onRating(ReviewRating.HARD) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF57F17)
                    ),
                ) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Hard", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hard")
                }
                Button(
                    onClick = { onRating(ReviewRating.KNOW_IT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Know it", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Got it")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FlashcardSummary(
    correct: Int,
    total: Int,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (total == 0) "No words due for review!" else "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        if (total > 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "$correct / $total correct",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            val pct = correct * 100 / total
            Text(
                text = "$pct% accuracy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}
