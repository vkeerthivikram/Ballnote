package com.ballnote.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompetitionFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun saved() {rule.waitForIdle();rule.waitUntil(15000){rule.onAllNodesWithText("Saved").fetchSemanticsNodes().isNotEmpty()}}
    @Test fun createSeriesScoreWithDoublePenaltyAndShowLargeScoreboard() {
        rule.waitUntil(15000) { rule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithContentDescription("Back to matches").fetchSemanticsNodes().isNotEmpty() }
        if(rule.onAllNodesWithContentDescription("Back to matches").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithContentDescription("Back to matches").performClick()
        rule.onNodeWithText("Competitions").performClick()
        rule.onNodeWithText("New competition").performScrollTo().performClick()
        rule.onNodeWithText("Competition name").performTextInput("Device series")
        rule.onNodeWithText("Team names · one per line (2–16)").performTextInput("Red\nBlue")
        rule.onNodeWithText("Overs (1–50)").performScrollTo().performTextReplacement("1")
        rule.onNodeWithText("Wicket limit (1–20)").performScrollTo().performTextReplacement("1")
        rule.onAllNodesWithText("+2").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("Preview fixtures").performScrollTo().performClick()
        rule.onNodeWithText("Create competition").performScrollTo().performClick()
        rule.waitUntil(15000){rule.onAllNodesWithText("Device series").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Device series").performScrollTo().performClick()
        rule.onNodeWithText("Set tournament timer").performScrollTo().performClick()
        rule.onNodeWithText("Start timer").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText("tournament timer",substring=true).fetchSemanticsNodes().isNotEmpty()}
        rule.onAllNodesWithText("Start match").onFirst().performScrollTo().performClick()
        rule.onNodeWithText("Red").performClick()
        saved()
        rule.onNodeWithText("Wide +2").performScrollTo().performClick();saved()
        rule.onNodeWithText("Over 1 · 2 runs").assertExists()
        rule.onNodeWithText("1st innings").performScrollTo().performClick()
        rule.onNodeWithText("Tournament ·",substring=true).assertExists()
        rule.onNodeWithText("Back to scoring").assertExists().performClick()
        rule.onNodeWithText("Wicket").performScrollTo().performClick();saved()
        rule.onNodeWithText("Start chase · target 3").performScrollTo().performClick();saved()
        rule.onNodeWithContentDescription("4 runs").performScrollTo().performClick();saved()
        rule.onNodeWithText("Match result and sharing").performScrollTo().performClick()
        rule.onNodeWithText("0 dots · 2 extras · 0 fours · 0 sixes",substring=true).assertExists()
        rule.onNodeWithContentDescription("Back to matches").performClick()
        rule.onNodeWithText("Competitions").performClick()
        rule.onNodeWithText("Device series").performScrollTo().performClick()
        rule.onNodeWithText("Red 0–1 Blue").assertExists()
    }
}
