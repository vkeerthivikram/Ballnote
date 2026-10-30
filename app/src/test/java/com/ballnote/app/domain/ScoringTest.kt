package com.ballnote.app.domain

import com.ballnote.app.data.Backup
import com.ballnote.app.data.json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ScoringTest {
    private fun game(rules: MatchRules = MatchRules()) = Game(teamA = "a", teamB = "b", nameA = "First", nameB = "Second", rules = rules)
    private fun ball(g: Game, n: Int, runs: Int = 0, kind: Kind = Kind.NORMAL, wicket: Boolean = false, innings: Int = 1) = Delivery(matchId = g.id, innings = innings, sequence = n, runs = runs, kind = kind, wicket = wicket)
    @Test fun extrasModes() {
        ExtrasMode.entries.forEach { mode -> val g = game(MatchRules(extras = mode)); val s = Scoring.score(g, listOf(ball(g, 0, kind = Kind.WIDE), ball(g, 1, 4, Kind.NO_BALL))); assertEquals(6, s.first.runs); assertEquals(if (mode == ExtrasMode.REBOWL) 0 else 2, s.first.balls) }
    }
    @Test fun finalOverStartsAtCorrectBoundary() {
        val g = game(MatchRules(2, 5, ExtrasMode.FINAL_OVER_REBOWL))
        val ds = (0..4).map { ball(g, it) } + ball(g, 5, kind = Kind.WIDE) + ball(g, 6, kind = Kind.NO_BALL) + ball(g, 7, 1)
        val s = Scoring.score(g, ds).first
        assertEquals(7, s.balls); assertEquals(3, s.runs); assertFalse(s.timeline[6].counted); assertEquals("1.1", s.overs)
    }
    @Test fun oneOverExtrasAlwaysRebowledInFinalMode() { val g = game(MatchRules(1, 5, ExtrasMode.FINAL_OVER_REBOWL)); assertEquals(0, Scoring.score(g, (0..30).map { ball(g, it, kind = Kind.WIDE) }).first.balls) }
    @Test fun runoutAndUnusualTotals() { val g = game(); val s = Scoring.score(g, listOf(ball(g, 0, 1, wicket = true), ball(g, 1, 7))); assertEquals(8, s.first.runs); assertEquals(1, s.first.wickets); assertEquals(2, s.first.balls) }
    @Test fun limitsAndInterval() { val g = game(MatchRules(1, 1)); val s = Scoring.score(g, listOf(ball(g, 0, 6), ball(g, 1, wicket = true), ball(g, 2, 4))); assertEquals(Status.INTERVAL, s.status); assertEquals(7, s.target); assertEquals(1, s.excluded.size) }
    @Test fun targetWinThroughExtra() {
        val g = game().copy(chaseStarted = true, firstClosed = true)
        val s = Scoring.score(g, listOf(ball(g, 0, 0), ball(g, 0, kind = Kind.WIDE, innings = 2)))
        assertEquals(Status.COMPLETE, s.status); assertEquals("b", s.winner); assertEquals(0, s.second.balls)
    }
    @Test fun lastBallWinAndTieAndLoss() {
        val g = game(MatchRules(1, 5)).copy(chaseStarted = true, firstClosed = true)
        val first = listOf(ball(g, 0, 5))
        fun score(last: Int) = Scoring.score(g, first + (0..4).map { ball(g, it, 0, innings = 2) } + ball(g, 5, last, innings = 2))
        assertEquals("b", score(6).winner); assertEquals("Match tied", score(5).result); assertEquals("a", score(4).winner)
    }
    @Test fun earlyClosureAndAbandonment() { val g = game().copy(firstClosed = true); assertTrue(Scoring.score(g, emptyList()).first.early); assertEquals(Status.INTERVAL, Scoring.score(g, emptyList()).status); assertEquals(Status.ABANDONED, Scoring.score(g.copy(abandoned = true), emptyList()).status) }
    @Test fun correctionTruncatesOnlyAfterPreview() {
        val g = game().copy(chaseStarted = true, firstClosed = true)
        val first = ball(g, 0, 5); val second = ball(g, 0, 1, innings = 2); val third = ball(g, 1, 5, innings = 2)
        val original = listOf(first, second, third)
        val p = Scoring.preview(g, original, second.id, second.copy(runs = 6))
        assertEquals(3, original.size); assertEquals(1, p.after.excluded.size); assertEquals(2, p.deliveries.size); assertEquals(6, p.after.second.runs)
    }
    @Test fun correctionCanReopenCompletedMatchAndRecordsFollow() {
        val g = game().copy(chaseStarted = true, firstClosed = true)
        val d = ball(g, 0, 6, innings = 2); val ds = listOf(ball(g, 0, 5), d)
        val before = Scoring.score(g, ds); assertEquals(1, Scoring.record("b", listOf(g to before)).won)
        val p = Scoring.preview(g, ds, d.id, d.copy(runs = 1))
        assertEquals(Status.CHASE, p.after.status); assertEquals(0, Scoring.record("b", listOf(g to p.after)).played)
    }
    @Test fun changedTargetAndKeptFirstBoundary() {
        val g = game(MatchRules(1, 1)).copy(chaseStarted = true, firstClosed = true)
        val d = ball(g, 0, 2, wicket = true); val ds = listOf(d, ball(g, 0, 1, innings = 2))
        val p = Scoring.preview(g, ds, d.id, d.copy(runs = 4, wicket = false))
        assertEquals(5, p.after.target); assertTrue(p.after.first.early)
    }
    @Test fun backupRoundTripAndValidation() {
        val g = game(); val b = Backup(teams = listOf(Team("a", "First"), Team("b", "Second")), matches = listOf(g), deliveries = listOf(ball(g, 0, 4)))
        val decoded = json.decodeFromString<Backup>(json.encodeToString(b)); decoded.validate(); assertEquals(b, decoded)
        assertThrows(IllegalArgumentException::class.java) { b.copy(formatVersion = 9).validate() }
        assertThrows(IllegalArgumentException::class.java) { b.copy(teams = emptyList()).validate() }
        assertThrows(IllegalArgumentException::class.java) { b.copy(deliveries = b.deliveries + b.deliveries).validate() }
    }
    @Test fun renameDoesNotAlterHistoricNameOrRecord() { val g = game().copy(abandoned = true); val t = Team("a", "New name"); assertEquals("First", g.nameA); assertEquals(1, Scoring.record(t.id, listOf(g to Scoring.score(g, emptyList()))).noResult) }
    @Test fun invalidRulesAndRunsAreRejected() { assertThrows(IllegalArgumentException::class.java) { MatchRules(0).validate() }; val g = game(); assertThrows(IllegalArgumentException::class.java) { Scoring.score(g, listOf(ball(g, 0, -1))) } }
}
