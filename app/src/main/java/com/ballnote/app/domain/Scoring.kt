package com.ballnote.app.domain

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
@Serializable enum class ExtrasMode { REBOWL, COUNT, FINAL_OVER_REBOWL }
@Serializable enum class Kind { NORMAL, WIDE, NO_BALL }
@Serializable data class MatchRules(val overs: Int = 6, val wickets: Int = 5, val extras: ExtrasMode = ExtrasMode.REBOWL) {
    fun validate() { require(overs in 1..50 && wickets in 1..20) { "Use 1–50 overs and 1–20 wickets." } }
}
// Retained for v1 database and backup compatibility. New matches use independent side IDs.
@Serializable data class Team(val id: String = newId(), val name: String, val archived: Boolean = false)
@Serializable data class Game(val id: String = newId(), val teamA: String, val teamB: String, val nameA: String, val nameB: String, val rules: MatchRules = MatchRules(), val created: Long = System.currentTimeMillis(), val opened: Long = created, val firstClosed: Boolean = false, val chaseStarted: Boolean = false, val abandoned: Boolean = false)
@Serializable data class Delivery(val id: String = newId(), val matchId: String, val innings: Int, val sequence: Int, val kind: Kind = Kind.NORMAL, val runs: Int = 0, val wicket: Boolean = false) {
    val total: Int get() = runs + if (kind == Kind.NORMAL) 0 else 1
    val label: String get() = (when (kind) { Kind.NORMAL -> if (wicket && runs == 0) "W" else "$runs"; Kind.WIDE -> "Wd${if (runs > 0) "+$runs" else ""}"; Kind.NO_BALL -> "Nb${if (runs > 0) "+$runs" else ""}" }) + if (wicket && (kind != Kind.NORMAL || runs > 0)) "+W" else ""
    fun validate() { require(innings in 1..2 && sequence >= 0 && runs in 0..99) { "Invalid delivery." } }
}
data class ScoredBall(val delivery: Delivery, val over: Int, val counted: Boolean)
data class InningsScore(val runs: Int, val wickets: Int, val balls: Int, val ended: Boolean, val early: Boolean, val timeline: List<ScoredBall>, val excluded: List<Delivery>) {
    val overs: String get() = "${balls / 6}.${balls % 6}"
}
enum class Status { FIRST, INTERVAL, CHASE, COMPLETE, ABANDONED }
data class Score(val first: InningsScore, val second: InningsScore, val status: Status, val target: Int, val winner: String?, val result: String) {
    val finished get() = status == Status.COMPLETE || status == Status.ABANDONED
    val excluded get() = first.excluded + second.excluded
}
data class Correction(val game: Game, val deliveries: List<Delivery>, val before: Score, val after: Score)
object Scoring {
    fun innings(deliveries: List<Delivery>, rules: MatchRules, target: Int? = null, closed: Boolean = false): InningsScore {
        rules.validate()
        var runs = 0; var wickets = 0; var balls = 0
        val timeline = mutableListOf<ScoredBall>(); val excluded = mutableListOf<Delivery>()
        fun ended() = balls >= rules.overs * 6 || wickets >= rules.wickets || (target != null && runs >= target)
        for (d in deliveries.sortedBy { it.sequence }) {
            d.validate()
            if (ended()) { excluded += d; continue }
            val counted = d.kind == Kind.NORMAL || rules.extras == ExtrasMode.COUNT || (rules.extras == ExtrasMode.FINAL_OVER_REBOWL && balls < (rules.overs - 1) * 6)
            timeline += ScoredBall(d, balls / 6, counted)
            runs += d.total; if (d.wicket) wickets++; if (counted) balls++
        }
        return InningsScore(runs, wickets, balls, ended() || closed, closed && !ended(), timeline, excluded)
    }
    fun score(g: Game, ds: List<Delivery>): Score {
        val first = innings(ds.filter { it.innings == 1 }, g.rules, closed = g.firstClosed || g.chaseStarted)
        val target = first.runs + 1
        val second = innings(ds.filter { it.innings == 2 }, g.rules, target)
        val status = when { g.abandoned -> Status.ABANDONED; g.chaseStarted && second.ended -> Status.COMPLETE; g.chaseStarted -> Status.CHASE; first.ended -> Status.INTERVAL; else -> Status.FIRST }
        val winner = if (status != Status.COMPLETE) null else when { second.runs >= target -> g.teamB; second.runs < first.runs -> g.teamA; else -> null }
        val result = when (status) {
            Status.ABANDONED -> "No result"
            Status.COMPLETE -> when (winner) { g.teamB -> "${g.nameB} won by ${g.rules.wickets - second.wickets} wicket${if (g.rules.wickets - second.wickets == 1) "" else "s"}"; g.teamA -> "${g.nameA} won by ${first.runs - second.runs} run${if (first.runs - second.runs == 1) "" else "s"}"; else -> "Match tied" }
            Status.INTERVAL -> "Target $target · ready for the chase"
            Status.CHASE -> "${target - second.runs} needed from ${g.rules.overs * 6 - second.balls} balls"
            Status.FIRST -> "First innings"
        }
        return Score(first, second, status, target, winner, result)
    }
    fun preview(g: Game, ds: List<Delivery>, id: String, replacement: Delivery?): Correction {
        require(ds.any { it.id == id }) { "Delivery no longer exists." }
        val candidate = ds.mapNotNull { if (it.id == id) replacement?.copy(id = it.id, matchId = it.matchId, innings = it.innings, sequence = it.sequence) else it }
        val after = score(g, candidate)
        val excluded = after.excluded.map { it.id }.toSet()
        return Correction(g, candidate.filterNot { it.id in excluded }, score(g, ds), after)
    }
}
