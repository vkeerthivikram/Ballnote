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

data class DeletedMatches(val id: String = newId(), val backup: Backup)

class AppModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val settings = Settings(app)
    val library = MutableStateFlow<Library?>(null)
    val preferences = settings.values.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
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
        val removed = repo.deleteMatches(ids)
        if (removed.matches.isNotEmpty()) {
            deleted.value = DeletedMatches(backup = removed)
            if (selected.value in ids) selected.value = null
        }
    }
    fun undoDeletion(id: String) = command {
        deleted.value?.takeIf { it.id == id }?.let { repo.undoDeletion(it.backup); deleted.value = null }
    }
    fun dismissDeletion(id: String) { if (deleted.value?.id == id) deleted.value = null }
    fun restore(b: Backup) = command { repo.restore(b); selected.value = null; deleted.value = null }
}
