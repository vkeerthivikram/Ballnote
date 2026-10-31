package com.ballnote.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ballnote.app.data.*
import com.ballnote.app.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DeletedMatches(val id: String = newId(), val backup: Backup, val competitions: List<Competition> = emptyList())

class AppModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val settings = Settings(app)
    val library = MutableStateFlow<Library?>(null)
    val preferences = settings.values.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
    val overCue = MutableStateFlow<String?>(null)
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val selected = MutableStateFlow<String?>(null)
    val deleted = MutableStateFlow<DeletedMatches?>(null)
    private val lock = Mutex()
    private var retry: (suspend () -> Unit)? = null
    init { viewModelScope.launch { repo.library.catch { error.value = "Could not open saved data: ${it.message}" }.collect { data -> if (library.value == null) selected.value = data.games.filter { !data.score(it).finished }.maxByOrNull { it.opened }?.id; library.value = data } } }
    fun command(action: suspend () -> Unit) { viewModelScope.launch { lock.withLock { busy.value = true; try { action(); library.value = repo.snapshot(); retry = null; error.value = null } catch (e: Exception) { retry = action; error.value = e.message ?: "Could not save. Please retry." } finally { busy.value = false } } } }
    fun retry() { command(retry ?: { library.value = repo.snapshot() }) }
    private suspend fun current(id: String): Pair<Game, List<Delivery>> { val l = repo.snapshot(); return l.games.first { it.id == id } to l.deliveries(id) }
    fun open(g: Game) { selected.value = g.id; command { val (latest, ds) = current(g.id); repo.save(latest.copy(opened = System.currentTimeMillis()), ds) } }
    fun create(a: String, b: String, rules: MatchRules) {
      val g = Game(teamA = newId(), teamB = newId(), nameA = a.trim(), nameB = b.trim(), rules = rules)
      command {
        settings.save(preferences.value.copy(rules = rules)); repo.create(g); selected.value = g.id
      }
    }
    fun record(id: String, kind: Kind, runs: Int, wicket: Boolean) = command {
        val (g, ds) = current(id); val s = Scoring.score(g, ds)
        require(s.status == Status.FIRST || s.status == Status.CHASE) { "This innings has ended." }
        val inning = if (g.chaseStarted) 2 else 1
        val d = Delivery(matchId = id, innings = inning, sequence = (ds.filter { it.innings == inning }.maxOfOrNull { it.sequence } ?: -1) + 1, kind = kind, runs = runs, wicket = wicket)
        d.validate(); repo.save(g, ds + d)
        val scored=Scoring.score(g,ds+d); val inningScore=if(g.chaseStarted) scored.second else scored.first
        if(inningsOverCompleted(if(g.chaseStarted) s.second else s.first,inningScore)) overCue.value="Over complete · ${inningScore.oversBreakdown.last().runs} runs"
    }
    fun undo(id: String) = command {
        val (g, ds) = current(id)
        val last = ds.filter { it.innings == if (g.chaseStarted) 2 else 1 }.maxByOrNull { it.sequence }
        if (last != null) repo.save(g.copy(firstClosed = if (!g.chaseStarted) false else g.firstClosed), ds.filterNot { it.id == last.id })
        else if (g.chaseStarted) repo.save(g.copy(chaseStarted = false, firstClosed = false), ds)
        else if (g.firstClosed) repo.save(g.copy(firstClosed = false), ds)
    }
    fun chase(id: String) = command { val (g, ds) = current(id); require(Scoring.score(g, ds).status == Status.INTERVAL); repo.save(g.copy(chaseStarted = true, firstClosed = true), ds) }
    fun endFirst(id: String) = command { val (g, ds) = current(id); require(!g.chaseStarted); repo.save(g.copy(firstClosed = true), ds) }
    fun abandon(id: String) = command { val (g, ds) = current(id); repo.save(g.copy(abandoned = true), ds) }
    fun correction(c: Correction) = command {
        val (g, ds) = current(c.game.id)
        require(g == c.game && Scoring.score(g, ds) == c.before) { "Match changed. Preview the correction again." }
        repo.save(g, c.deliveries)
    }
    fun preferences(p: Preferences) = command { settings.save(p) }
    fun deleteMatches(ids: Set<String>) = command {
        val competitions = repo.snapshot().competitions.filter { c -> c.fixtures.any { it.matchId in ids } }
        val removed = repo.deleteMatches(ids)
        if (removed.matches.isNotEmpty()) {
            deleted.value = DeletedMatches(backup = removed, competitions = competitions)
            if (selected.value in ids) selected.value = null
        }
    }
    fun undoDeletion(id: String) = command {
        deleted.value?.takeIf { it.id == id }?.let { repo.undoDeletion(it.backup); val current=repo.snapshot(); it.competitions.forEach { old -> val now=current.competitions.find { c -> c.id==old.id }; if(now!=null) repo.competition(now.copy(fixtures=now.fixtures.map { f -> val prior=old.fixtures.find { x -> x.id==f.id }; if(f.matchId==null && prior?.matchId in it.backup.matches.map { g -> g.id }) prior!! else f })) }; deleted.value = null }
    }
    fun dismissDeletion(id: String) { if (deleted.value?.id == id) deleted.value = null }
    fun createCompetition(c: Competition) = command { repo.competition(Competitions.create(c)) }
    fun startFixture(id: String, fixtureId: String, swap: Boolean) = command {
        val l=repo.snapshot(); val c=l.competitions.first { it.id==id }; val f=c.fixtures.first { it.id==fixtureId }
        f.matchId?.let { selected.value=it; return@command }
        require(!c.closed && !f.bye && f.decision==null) { "This fixture is resolved." }
        if(c.bestOf) require(!Competitions.headline(c,l.games,l.balls).contains("Series complete")) { "This series is complete." }
        val (a,b)=Competitions.sides(c,f,l.games,l.balls)
        require(a!=null && b!=null && a !in c.withdrawn && b !in c.withdrawn) { "Resolve earlier fixtures or withdrawals first." }
        val first=if(swap) b else a; val second=if(swap) a else b
        val g=Game(teamA=first,teamB=second,nameA=c.team(first),nameB=c.team(second),rules=c.rules)
        repo.fixture(c.copy(fixtures=c.fixtures.map { if(it.id==f.id) it.copy(matchId=g.id) else it }),g); selected.value=g.id
    }
    fun advanceCompetition(id: String) = command { val l=repo.snapshot(); val c=l.competitions.first { it.id==id }; repo.competition(Competitions.advance(c,l.games,l.balls)) }
    fun nextSeries(id: String) = command {
        val l=repo.snapshot(); val c=l.competitions.first { it.id==id }
        require(!c.closed && c.format==Format.BILATERAL && c.openEnded && c.fixtures.all { Competitions.finished(c,it,l.games,l.balls) }) { "Finish the current session match first." }
        repo.competition(c.copy(fixtures=c.fixtures+Fixture(label="Match "+(c.fixtures.size+1),a=c.teams[0].id,b=c.teams[1].id)))
    }
    fun decideFixture(id: String, fixtureId: String, winner: String, reason: String) = command {
        val l=repo.snapshot(); val c=l.competitions.first { it.id==id }; val f=c.fixtures.first { it.id==fixtureId }
        val sides=Competitions.sides(c,f,l.games,l.balls)
        require(winner in listOfNotNull(sides.first,sides.second) && reason.isNotBlank()) { "Choose a team and record the reason." }
        require(f.matchId==null || l.games.find { it.id==f.matchId }?.let { l.score(it).finished }==true) { "Finish or abandon the live match first." }
        require(c.fixtures.none { (it.matchId!=null || it.decision!=null) && (it.sourceA==f.id || it.sourceB==f.id) } && (f.elimination || c.lockedSeeds.isEmpty())) { "Reset later rounds before changing this decision." }
        repo.competition(c.copy(fixtures=c.fixtures.map { if(it.id==f.id) it.copy(decision=winner,reason=reason.trim()) else it }))
    }
    fun replayFixture(id: String, fixtureId: String) = command {
        val l=repo.snapshot(); val c=l.competitions.first { it.id==id }; val f=c.fixtures.first { it.id==fixtureId }
        require((f.elimination || c.lockedSeeds.isEmpty()) && c.fixtures.none { (it.matchId!=null || it.decision!=null) && (it.sourceA==f.id || it.sourceB==f.id) }) { "Reset later rounds before replaying." }
        repo.competition(c.copy(fixtures=c.fixtures.map { if(it.id==f.id) it.copy(matchId=null,decision=null,reason="") else it }))
    }
    fun resetRounds(id: String) = command {
        val c=repo.snapshot().competitions.first { it.id==id }
        repo.competition(if(c.format==Format.KNOCKOUT) Competitions.create(c.copy(fixtures=emptyList())) else c.copy(fixtures=c.fixtures.filterNot { it.elimination },lockedSeeds=emptyList()))
    }
    fun withdraw(id: String, team: String) = command {
        val l=repo.snapshot(); val c=l.competitions.first { it.id==id }
        require(c.lockedSeeds.isEmpty()) { "Reset playoffs before changing eligibility." }
        repo.competition(c.copy(withdrawn=if(team in c.withdrawn) c.withdrawn-team else c.withdrawn+team))
    }
    fun closeSession(id: String) = command {
        val l=repo.snapshot(); val c=l.competitions.first {it.id==id}
        require(c.format==Format.BILATERAL && c.openEnded && c.fixtures.all {Competitions.finished(c,it,l.games,l.balls)}) { "Finish the current session match first." }
        repo.competition(c.copy(closed=true))
    }
    fun deleteCompetition(id: String) = command { repo.removeCompetition(id) }
    fun restore(b: Backup) = command { repo.restore(b); selected.value = null; deleted.value = null }
}

internal fun inningsOverCompleted(before: InningsScore, after: InningsScore) = after.balls > before.balls && after.balls % 6 == 0
