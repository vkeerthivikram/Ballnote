package com.ballnote.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ballnote.app.data.Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun currentTimer()=runBlocking {val repo=Repository(rule.activity);try {repo.snapshot().games.firstOrNull {it.nameA=="Clock XI"}?.timer} finally {repo.close()}}
    @Test fun matchTimerControlsAndLiveSnapshotSharing() {
        rule.waitUntil(15000) {rule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithContentDescription("Back to matches").fetchSemanticsNodes().isNotEmpty()}
        if(rule.onAllNodesWithContentDescription("Back to matches").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithContentDescription("Back to matches").performClick()
        rule.onAllNodesWithText("Matches").onLast().performClick()
        rule.onNodeWithText("New match").performClick()
        rule.onNodeWithText("Batting first").performTextInput("Clock XI")
        rule.onNodeWithText("Batting second").performTextInput("Snapshot XI")
        rule.onNodeWithText("Start match").performScrollTo().performClick()
        rule.waitUntil(15000) {rule.onAllNodesWithContentDescription("Match timer").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithContentDescription("Match timer").performClick()
        rule.onNodeWithText("Start timer").performClick()
        rule.waitUntil(10000) {currentTimer()!=null}
        rule.onNodeWithContentDescription("Match timer").performClick()
        rule.onNodeWithText("Pause").performClick()
        rule.waitUntil(10000) {currentTimer()?.pausedRemaining!=null}
        val paused=currentTimer()!!.pausedRemaining!!
        rule.onNodeWithText("+10 min").performClick()
        rule.waitUntil(10000) {currentTimer()?.pausedRemaining==paused+600_000}
        rule.onNodeWithText("Resume").performClick()
        rule.waitUntil(10000) {currentTimer()?.pausedRemaining==null}
        rule.onNodeWithText("Close").performClick()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val monitor=instrumentation.addMonitor(IntentFilter(Intent.ACTION_CHOOSER),Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true)
        try {
            rule.onNodeWithContentDescription("Share live score").performClick()
            rule.waitUntil(10000) {monitor.hits>0}
        } finally {instrumentation.removeMonitor(monitor)}
        rule.onNodeWithContentDescription("Match timer").performClick()
        rule.onNodeWithText("Stop timer").performClick()
        rule.waitUntil(10000) {currentTimer()==null}
    }
}
