package com.quranapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cafe.adriel.voyager.navigator.Navigator
import com.quranapp.ui.screens.quran.QuranHomeScreen
import com.quranapp.ui.theme.QuranAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class QuranUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quranHomeScreen_showsSurahList() {
        composeTestRule.setContent {
            QuranAppTheme {
                Navigator(QuranHomeScreen)
            }
        }

        // Wait for potential async loading
        composeTestRule.waitUntil(5000) {
            composeTestRule
                .onAllNodesWithText("Al-Fatihah")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Al-Fatihah").assertIsDisplayed()
        composeTestRule.onNodeWithText("The Opening").assertIsDisplayed()
    }

    @Test
    fun quranReader_navigationAndTranslationToggle() {
        composeTestRule.setContent {
            QuranAppTheme {
                Navigator(QuranHomeScreen)
            }
        }

        // 1. Wait and click Surah
        composeTestRule.waitUntil(5000) {
            composeTestRule
                .onAllNodesWithText("Al-Fatihah")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Al-Fatihah").performClick()

        // 2. Verify in Reader (Check Top Bar Title)
        composeTestRule.onNodeWithText("The Opening").assertIsDisplayed()

        // 3. Verify Arabic Text Presence (Basmallah for most surahs, but Al-Fatihah starts with it as ayah 1 in some scripts)
        // Since we use Uthmani font, we just check if any text starts with the typical unicode for Bismillah if we can,
        // but easier to check for the Ayah 1 number.
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed()

        // 4. Toggle Translation
        // Find by icon content description
        composeTestRule.onNodeWithContentDescription("Toggle Translation").performClick()
        
        // Assert English translation of first ayah is NOT displayed if toggled off
        // "In the name of Allah..."
        composeTestRule.onNodeWithText("In the name of Allah", substring = true).assertDoesNotExist()

        // Toggle back
        composeTestRule.onNodeWithContentDescription("Toggle Translation").performClick()
        composeTestRule.onNodeWithText("In the name of Allah", substring = true).assertIsDisplayed()
    }
}
