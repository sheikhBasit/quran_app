package com.quranapp.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quranapp.viewmodel.SettingsViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranapp.ui.screens.prayer.PrayerTimesScreen
import com.quranapp.ui.screens.qibla.QiblaScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    checked = themeMode == "dark",
                    onCheckedChange = { isChecked ->
                        viewModel.setThemeMode(if (isChecked) "dark" else "light")
                    }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("Quran Settings") }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.MenuBook,
                    title = "Arabic Script",
                    subtitle = "Uthmani (Hafs)"
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Translate,
                    title = "Show Translation",
                    subtitle = "Display english translation in reader",
                    checked = showTranslation,
                    onCheckedChange = { viewModel.setShowTranslation(it) }
                )
            }
            
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Default.TextFields, contentDescription = null) },
                    headlineContent = { Text("Arabic Font Size") },
                    supportingContent = {
                        Slider(
                            value = arabicFontSize,
                            onValueChange = { viewModel.setArabicFontSize(it) },
                            valueRange = 20f..48f,
                            steps = 14
                        )
                    },
                    trailingContent = { Text("${arabicFontSize.toInt()}sp") }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("Notifications") }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Prayer Times",
                    subtitle = "Get notified for daily prayers",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("Tools") }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.AccessTime,
                    title = "Prayer Times",
                    subtitle = "View today's prayer schedule",
                    onClick = { navigator.push(PrayerTimesScreen) }
                )
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Explore,
                    title = "Qibla Finder",
                    subtitle = "Find direction of Kaaba",
                    onClick = { navigator.push(QiblaScreen) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("About") }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.1 (Phase 11)"
                )
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Favorite,
                    title = "Support Us",
                    subtitle = "Spread the word & contribute"
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}

// Moved to separate files: PrayerTimesScreen.kt and QiblaScreen.kt
