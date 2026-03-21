package com.quranapp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.domain.model.Ayah
import com.quranapp.domain.model.TafsirEntry

@Composable
fun TafsirBottomSheet(
    ayah: Ayah,
    tafsiers: List<TafsirEntry>,
    onDismiss: () -> Unit,
) {
    var selectedTafsirIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 500.dp)
    ) {
        Text(
            text = "Tafsir Ayah ${ayah.surahNumber}:${ayah.ayahNumber}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTafsirIndex,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tafsiers.forEachIndexed { index, entry ->
                Tab(
                    selected = selectedTafsirIndex == index,
                    onClick = { selectedTafsirIndex = index },
                    text = { Text(entry.bookName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tafsiers.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = tafsiers[selectedTafsirIndex].content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Text("Loading Tafsir...")
        }
    }
}
