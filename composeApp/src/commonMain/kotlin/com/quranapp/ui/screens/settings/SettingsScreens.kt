package com.quranapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { SettingsSectionHeader("Appearance") }
            item {
                var darkMode by remember { mutableStateOf(false) }
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
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
                var showTranslation by remember { mutableStateOf(true) }
                SettingsToggleItem(
                    icon = Icons.Default.Translate,
                    title = "Show Translation",
                    subtitle = "Display english translation in reader",
                    checked = showTranslation,
                    onCheckedChange = { showTranslation = it }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("Notifications") }
            item {
                var prayerNotifs by remember { mutableStateOf(true) }
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Prayer Times",
                    subtitle = "Get notified for daily prayers",
                    checked = prayerNotifs,
                    onCheckedChange = { prayerNotifs = it }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsSectionHeader("About") }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0 (Alpha)"
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
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}

@Composable
fun PrayerTimesScreen() { Text("Prayer Times") }

@Composable
fun QiblaScreen() { Text("Qibla") }
