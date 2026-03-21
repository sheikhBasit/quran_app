package com.quranapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.quranapp.ui.navigation.AppNavigation
import com.quranapp.ui.theme.QuranAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QuranAppTheme {
                AppNavigation()
            }
        }
    }
}
