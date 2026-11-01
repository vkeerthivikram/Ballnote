package com.ballnote.app.domain

import com.ballnote.app.data.Backup
import com.ballnote.app.data.json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class SessionTimerTest {
    private val now=1_800_000_000_000L
    @Test fun deadlineSurvivesElapsedTimeAndSerialization() {
        val timer=SessionTimer.start(60,now)
        val copy=json.decodeFromString<SessionTimer>(json.encodeToString(timer))
        assertEquals(45*60_000L,copy.remaining(now+15*60_000L))
        assertEquals("45:00",timerClock(copy.remaining(now+15*60_000L)))
        assertEquals("1:00:00",timerClock(timer.remaining(now)))
        assertEquals("00:01",timerClock(1));assertEquals("00:00",timerClock(-1))
    }
    @Test fun pauseResumeAndExtensionKeepTheCorrectRemainingTime() {
        val timer=SessionTimer.start(60,now).pause(now+15*60_000L)
        assertEquals(45*60_000L,timer.remaining(now+60*60_000L))
        assertNull(timer.alert(now+60*60_000L))
        val extended=timer.extend(now+60*60_000L)
        assertEquals(55*60_000L,extended.pausedRemaining)
        val resumed=extended.resume(now+120*60_000L)
        assertEquals(55*60_000L,resumed.remaining(now+120*60_000L))
        assertNotEquals(timer.id,resumed.id)
    }
    @Test fun warningsAndExpiryAreDistinctAndCanBeDisabled() {
        val timer=SessionTimer.start(60,now)
        assertNull(timer.alert(now+49*60_000L))
        assertEquals(TimerAlert.TEN_MINUTES,timer.alert(now+50*60_000L))
        assertNull(timer.copy(warningDelivered=true).alert(now+51*60_000L))
        assertEquals(TimerAlert.EXPIRED,timer.alert(now+61*60_000L))
        assertNull(timer.copy(expiryDelivered=true).alert(now+61*60_000L))
        assertNull(SessionTimer.start(60,now,false).alert(now+55*60_000L))
        assertNull(SessionTimer.start(5,now).alert(now))
    }
    @Test fun extendingAnExpiredTimerRestartsItWithoutAnImmediateWarning() {
        val timer=SessionTimer.start(1,now).copy(expiryDelivered=true)
        val extended=timer.extend(now+5*60_000L)
        assertEquals(10*60_000L,extended.remaining(now+5*60_000L))
        assertNull(extended.alert(now+5*60_000L))
        assertEquals(TimerAlert.EXPIRED,extended.alert(now+15*60_000L))
    }
    @Test fun invalidDurationsAndSavedTimersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {SessionTimer.start(0,now)}
        assertThrows(IllegalArgumentException::class.java) {SessionTimer.start(721,now)}
        assertThrows(IllegalArgumentException::class.java) {SessionTimer(endsAt=-1).validate()}
        assertThrows(IllegalArgumentException::class.java) {SessionTimer(endsAt=now,pausedRemaining=-1).validate()}
        assertThrows(IllegalArgumentException::class.java) {SessionTimer.start(720,now).extend(now)}
    }
    @Test fun oldBackupsDefaultToNoTimerAndNewBackupsKeepBothScopes() {
        val g=Game(teamA="a",teamB="b",nameA="A",nameB="B",timer=SessionTimer.start(60,now))
        val c=Competitions.create(Competition(name="Cup",format=Format.BILATERAL,teams=listOf(Entrant(id="a",name="A"),Entrant(id="b",name="B")),timer=SessionTimer.start(120,now)))
        val b=Backup(teams=listOf(Team("a","A"),Team("b","B")),matches=listOf(g),deliveries=emptyList(),competitions=listOf(c))
        val copy=json.decodeFromString<Backup>(json.encodeToString(b));copy.validate()
        assertEquals(g.timer,copy.matches.single().timer);assertEquals(c.timer,copy.competitions.single().timer)
        val oldText=json.encodeToString(b.copy(formatVersion=2,matches=listOf(g.copy(timer=null)),competitions=listOf(c.copy(timer=null)))).replace(",\"timer\":null","")
        val old=json.decodeFromString<Backup>(oldText);old.validate();assertNull(old.matches.single().timer)
        assertThrows(IllegalArgumentException::class.java) {b.copy(matches=listOf(g.copy(timer=SessionTimer(endsAt=-1)))).validate()}
    }
    @Test fun liveSnapshotIsFrozenAndIncludesChaseMathWithoutDeclaringAWinner() {
        val g=Game(teamA="a",teamB="b",nameA="A",nameB="B",rules=MatchRules(2,2),chaseStarted=true)
        val balls=listOf(Delivery(matchId=g.id,innings=1,sequence=0,runs=10),Delivery(matchId=g.id,innings=2,sequence=0,runs=4))
        val snapshot=LiveSnapshot(g,Scoring.score(g,balls),now)
        assertTrue(snapshot.text.contains("7 needed from 11 balls"));assertTrue(snapshot.text.contains("Target 11"));assertTrue(snapshot.label.contains("LIVE"))
        Scoring.score(g,balls+Delivery(matchId=g.id,innings=2,sequence=1,runs=6))
        assertEquals(4,snapshot.score.second.runs)
        val first=LiveSnapshot(g.copy(chaseStarted=false),Scoring.score(g.copy(chaseStarted=false),balls.take(1)),now)
        assertTrue(first.text.contains("yet to bat"));assertTrue(first.headline.contains("11 balls left"))
    }
}
