package com.quranapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    TabNavigator(QuranTab) { tabNavigator ->
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    listOf(QuranTab, HadithTab, SearchTab, ChatbotTab, SettingsTab)
                        .forEach { tab ->
                            val icon = when(tab) {
                                is QuranTab -> tab.icon
                                is HadithTab -> tab.icon
                                is SearchTab -> tab.icon
                                is ChatbotTab -> tab.icon
                                is SettingsTab -> tab.icon
                                else -> Icons.Default.Block // Should not happen
                            }
                            NavigationBarItem(
                                selected = tabNavigator.current == tab,
                                onClick = { tabNavigator.current = tab },
                                icon = { Icon(icon, contentDescription = tab.options.title) },
                                label = { Text(tab.options.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                CurrentTab()
            }
        }
    }
}

// Tab definitions — one per bottom nav item
object QuranTab : Tab {
    val icon: ImageVector = Icons.Default.MenuBook
    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "Quran")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(QuranHomeScreen)
    }
}

object HadithTab : Tab {
    val icon: ImageVector = Icons.Default.AutoStories
    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "Hadith")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(HadithCollectionsScreen)
    }
}

object SearchTab : Tab {
    val icon: ImageVector = Icons.Default.Search
    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "Search")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(SearchScreen)
    }
}

object ChatbotTab : Tab {
    val icon: ImageVector = Icons.Default.Chat
    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "Chat")

    @Composable
    override fun Content() {
        cafe.adriel.voyager.navigator.Navigator(ChatbotScreen)
    }
}

object SettingsTab : Tab {
    val icon: ImageVector = Icons.Default.Settings
    override val options: TabOptions
        @Composable get() = TabOptions(index = 4u, title = "Settings")

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
