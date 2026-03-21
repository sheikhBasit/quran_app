package com.quranapp.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quranapp.domain.model.Ayah

@Composable
fun AnnotationMenu(
    ayah: Ayah,
    onBookmark: () -> Unit,
    onHighlight: (String) -> Unit, // Color hex
    onNote: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Ayah ${ayah.surahNumber}:${ayah.ayahNumber}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AnnotationItem(Icons.Default.Bookmark, "Bookmark", onBookmark)
            AnnotationItem(Icons.Default.FormatPaint, "Highlight") { 
                // Color selection could be a sub-menu, for now just a default
                onHighlight("#FFD700") 
            }
            AnnotationItem(Icons.Default.ContentPaste, "Note", onNote)
            AnnotationItem(Icons.Default.Share, "Share", onShare)
        }
    }
}

@Composable
private fun AnnotationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
