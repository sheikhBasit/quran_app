package com.quranapp.ui.screens.hadith

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

object HadithCollectionsScreen : Screen {
    @Composable override fun Content() { Text("Hadith Collections — Phase 5") }
}

data class HadithBooksScreen(val collection: String) : Screen {
    @Composable override fun Content() { Text("Books: $collection — Phase 5") }
}

data class HadithListScreen(val collection: String, val bookNumber: Int) : Screen {
    @Composable override fun Content() { Text("Hadith List — Phase 5") }
}
