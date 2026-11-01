package com.ballnote.app.data

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ballnote.app.domain.*
import com.ballnote.app.ui.resultFile
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerStorageTest {
    @Test fun timersPersistIndependentlyAndConcurrentAlertDeliveryIsClaimedOnce() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val name="timer-"+newId()+".db";var repo=Repository(context,name)
        try {
            val now=System.currentTimeMillis()
            val c=Competitions.create(Competition(name="Booked turf",format=Format.BILATERAL,teams=listOf(Entrant(name="A"),Entrant(name="B")),timer=SessionTimer.start(120,now)))
            repo.competition(c);val f=c.fixtures.first()
            val g=Game(teamA=f.a!!,teamB=f.b!!,nameA="A",nameB="B",rules=c.rules,timer=SessionTimer.start(60,now))
            repo.fixture(c.copy(fixtures=c.fixtures.map {if(it.id==f.id) it.copy(matchId=g.id) else it}),g)
            repo.changeTimer(TimerScope.MATCH,g.id) {it!!.pause(now+60_000)}
            repo.close();repo=Repository(context,name)
            var l=repo.snapshot()
            assertEquals(59*60_000L,l.games.single().timer!!.remaining(now+15*60_000))
            assertNull(l.competitions.single().timer!!.pausedRemaining)
            val backup=Backup(teams=l.teams,matches=l.games,deliveries=l.balls,competitions=l.competitions)
            backup.validate();repo.restore(json.decodeFromString<Backup>(json.encodeToString(backup)))
            assertEquals(backup.competitions,repo.snapshot().competitions)
            val stale=l.sessionTimers().first {it.scope==TimerScope.MATCH}
            repo.changeTimer(TimerScope.MATCH,g.id) {SessionTimer(endsAt=now-1)}
            assertNull(repo.claimTimerAlert(stale,now))
            l=repo.snapshot();val ref=l.sessionTimers().first {it.scope==TimerScope.MATCH}
            val claimed=coroutineScope {listOf(async(Dispatchers.IO){repo.claimTimerAlert(ref,now)},async(Dispatchers.IO){repo.claimTimerAlert(ref,now)}).awaitAll()}
            assertEquals(1,claimed.count {it==TimerAlert.EXPIRED})
            repo.changeTimer(TimerScope.MATCH,g.id) {null}
            assertNull(repo.snapshot().games.single().timer)
            assertNotNull(repo.snapshot().competitions.single().timer)
        } finally {repo.close();context.deleteDatabase(name)}
    }
    @Test fun liveSnapshotFilesAreDistinctAndFreezeTheImageAtCapture() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val g=Game(teamA="a",teamB="b",nameA="A long turf side name",nameB="Another turf side",rules=MatchRules(2,5))
        val balls=listOf(Delivery(matchId=g.id,innings=1,sequence=0,runs=4))
        val first=resultFile(context,g,Scoring.score(g,balls),System.currentTimeMillis())
        val firstBytes=context.contentResolver.openInputStream(first)!!.use {it.readBytes()}
        val next=resultFile(context,g,Scoring.score(g,balls+Delivery(matchId=g.id,innings=1,sequence=1,runs=6)),System.currentTimeMillis()+1000)
        assertNotEquals(first,next)
        assertArrayEquals(firstBytes,context.contentResolver.openInputStream(first)!!.use {it.readBytes()})
        val bitmap=BitmapFactory.decodeByteArray(firstBytes,0,firstBytes.size)
        assertEquals(1080,bitmap.width);assertEquals(1080,bitmap.height);bitmap.recycle()
        assertFalse(firstBytes.contentEquals(context.contentResolver.openInputStream(next)!!.use {it.readBytes()}))
    }
}
