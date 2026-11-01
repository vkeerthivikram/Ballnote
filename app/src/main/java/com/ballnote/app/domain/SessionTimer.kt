package com.ballnote.app.domain

import kotlinx.serialization.Serializable

/** A saved booking deadline; scoring and match completion never alter it. */
@Serializable data class SessionTimer(
    val id: String = newId(),
    val endsAt: Long,
    val pausedRemaining: Long? = null,
    val warnAtTenMinutes: Boolean = true,
    val warningDelivered: Boolean = false,
    val expiryDelivered: Boolean = false
) {
    fun validate() {
        require(id.isNotBlank() && endsAt in 1..253402300799999L) { "Invalid session timer deadline." }
        require(pausedRemaining == null || pausedRemaining in 1..MAX_DURATION) { "Invalid paused timer." }
    }
    fun remaining(now: Long): Long = pausedRemaining ?: (endsAt - now).coerceAtLeast(0)
    fun alert(now: Long): TimerAlert? = when {
        pausedRemaining != null -> null
        remaining(now) == 0L && !expiryDelivered -> TimerAlert.EXPIRED
        warnAtTenMinutes && !warningDelivered && remaining(now) in 1..TEN_MINUTES -> TimerAlert.TEN_MINUTES
        else -> null
    }
    fun pause(now: Long): SessionTimer {
        require(remaining(now) > 0) { "This timer has ended." }
        return copy(id=newId(), pausedRemaining=remaining(now))
    }
    fun resume(now: Long): SessionTimer = pausedRemaining?.let { copy(id=newId(),endsAt=now+it,pausedRemaining=null) } ?: this
    fun extend(now: Long, minutes: Int = 10): SessionTimer {
        require(minutes in 1..720)
        val duration=remaining(now)+minutes*60_000L
        require(duration<=MAX_DURATION) { "A timer can have up to 12 hours remaining." }
        return copy(id=newId(),endsAt=now+duration,pausedRemaining=if(pausedRemaining!=null) duration else null,warningDelivered=duration<=TEN_MINUTES,expiryDelivered=false)
    }
    companion object {
        const val TEN_MINUTES = 600_000L
        const val MAX_DURATION = 43_200_000L
        fun start(minutes: Int, now: Long, warn: Boolean = true): SessionTimer {
            require(minutes in 1..720) { "Choose 1–720 minutes." }
            return SessionTimer(endsAt=now+minutes*60_000L,warnAtTenMinutes=warn,warningDelivered=minutes<=10).also {it.validate()}
        }
    }
}
enum class TimerAlert { TEN_MINUTES, EXPIRED }
enum class TimerScope { MATCH, COMPETITION }
data class TimerRef(val scope: TimerScope,val ownerId: String,val label: String,val timer: SessionTimer) {
    val key get() = "${scope.name}:$ownerId"
    fun message(alert: TimerAlert) = "$label · ${if(alert==TimerAlert.EXPIRED) "Turf session ended" else "10 minutes of turf time left"}"
}
fun timerClock(remaining: Long): String {
    val seconds=(remaining.coerceAtLeast(0)+999)/1000
    return if(seconds>=3600) "%d:%02d:%02d".format(java.util.Locale.ENGLISH,seconds/3600,seconds/60%60,seconds%60)
    else "%02d:%02d".format(java.util.Locale.ENGLISH,seconds/60,seconds%60)
}

data class LiveSnapshot(val game: Game,val score: Score,val capturedAt: Long) {
    val headline get() = if(score.status==Status.FIRST) "${game.rules.overs*6-score.first.balls} balls left · ${game.rules.wickets-score.first.wickets} wickets in hand" else score.result
    val label get() = when(score.status) {Status.FIRST->"LIVE · FIRST INNINGS";Status.CHASE->"LIVE · CHASE";Status.INTERVAL->"INNINGS BREAK";Status.COMPLETE->"FINAL RESULT";Status.ABANDONED->"NO RESULT"}
    val text get() = buildString {
        appendLine(label)
        appendLine("${game.nameA} ${score.first.runs}/${score.first.wickets} (${score.first.overs})")
        appendLine(if(game.chaseStarted) "${game.nameB} ${score.second.runs}/${score.second.wickets} (${score.second.overs})" else "${game.nameB} · yet to bat")
        if(game.chaseStarted || score.status==Status.INTERVAL) appendLine("Target ${score.target}")
        append(headline)
    }
}
