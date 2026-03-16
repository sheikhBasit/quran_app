package com.quranapp.ui.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranapp.ui.screens.quran.QuranHomeScreen
import com.quranapp.ui.screens.hadith.HadithCollectionsScreen
import com.quranapp.ui.screens.search.SearchScreen
import com.quranapp.ui.screens.chatbot.ChatbotScreen
import com.quranapp.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation() {
    TabNavigator(QuranTab) {
        // Bottom navigation bar + CurrentTab
        // Scaffold with BottomBar defined here
        CurrentTab()
    }
}

// Tab definitions — one per bottom nav item
object QuranTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "Quran")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(QuranHomeScreen)
    }
}

object HadithTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "Hadith")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(HadithCollectionsScreen)
    }
}

object SearchTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "Search")

    @Composable
    override fun Content() {
        SearchScreen()
    }
}

object ChatbotTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "Chat")

    @Composable
    override fun Content() {
        ChatbotScreen()
    }
}

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 4u, title = "Settings")

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
