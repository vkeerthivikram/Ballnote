package com.ballnote.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import com.ballnote.app.data.Preferences
import com.ballnote.app.data.Settings
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScoringFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun saved() { rule.waitForIdle(); rule.waitUntil(10000) { rule.onAllNodesWithText("Saved").fetchSemanticsNodes().isNotEmpty() } }
    private fun screenshot(name: String) {
        rule.onNodeWithText("Weekend XI").performScrollTo()
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(rule.activity.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun playMatchAndCorrectWinningDelivery() {
        rule.onNodeWithText("New match").performClick()
        rule.onNodeWithText("Batting first").performTextInput("Weekend XI")
        rule.onNodeWithText("Batting second").performTextInput("Turf Titans")
        rule.onNodeWithText("Overs (1–50)").performTextReplacement("1")
        rule.onNodeWithText("Start match").performScrollTo().performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("4 runs").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("4 runs").performScrollTo().performClick()
        saved()
        screenshot("scoring-light")
        runBlocking { Settings(rule.activity).save(Preferences(theme = "Dark")) }
        rule.waitForIdle()
        screenshot("scoring-dark")
        runBlocking { Settings(rule.activity).save(Preferences(theme = "Light")) }
        rule.onNodeWithText("Wide +1").performScrollTo().performClick()
        saved()
        rule.onNodeWithText(" Undo last ball").performScrollTo().performClick()
        saved()
        repeat(5) { rule.onNodeWithContentDescription("Dot ball").performScrollTo().performClick(); saved() }
        rule.onNodeWithText("Start chase · target 5").performScrollTo().performClick()
        saved()
        rule.onNodeWithContentDescription("6 runs").performScrollTo().performClick()
        saved()
        rule.onNodeWithText("Match result and sharing").performScrollTo().performClick()
        rule.onNodeWithText("Turf Titans won by 5 wickets").assertExists()
        rule.onAllNodesWithText("Edit").onLast().performScrollTo().performClick()
        rule.onNodeWithText("Runs (0–99)").performTextReplacement("1")
        rule.onNodeWithText("Preview correction").performScrollTo().performClick()
        rule.onNodeWithText("Apply correction").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("4 needed from 5 balls").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Rematch").performScrollTo().performClick()
        rule.onNodeWithText("Batting first").assertExists()
    }
}
