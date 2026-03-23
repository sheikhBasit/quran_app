package com.quranapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.domain.repository.SettingsRepository
import com.quranapp.ui.navigation.AppNavigation
import com.quranapp.ui.theme.QuranAppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsRepository: SettingsRepository by inject()

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "system")
            
            QuranAppTheme(themeMode = themeMode) {
                AppNavigation()
            }
        }
    }
}
