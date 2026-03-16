package com.quranapp.ui.screens.quran

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

/** Surah list screen — fully implemented in Phase 4 */
object QuranHomeScreen : Screen {
    @Composable
    override fun Content() {
        Text("Quran Home — Phase 4")
    }
}

/** Quran reader (scroll + page modes) — fully implemented in Phase 4 */
data class QuranReaderScreen(val surahNumber: Int) : Screen {
    @Composable
    override fun Content() {
        Text("Quran Reader Surah $surahNumber — Phase 4")
    }
}
