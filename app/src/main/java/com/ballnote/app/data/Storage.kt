package com.ballnote.app.data

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ballnote.app.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }
@Entity(tableName = "teams") data class TeamRow(@PrimaryKey val id: String, val payload: String)
@Entity(tableName = "matches") data class MatchRow(@PrimaryKey val id: String, val payload: String)
@Entity(tableName = "innings", primaryKeys = ["matchId", "number"], foreignKeys = [ForeignKey(entity = MatchRow::class, parentColumns = ["id"], childColumns = ["matchId"], onDelete = ForeignKey.CASCADE)])
data class InningsRow(val matchId: String, val number: Int, val closed: Boolean)
@Entity(tableName = "deliveries", indices = [Index(value = ["matchId", "innings", "sequence"], unique = true)], foreignKeys = [ForeignKey(entity = MatchRow::class, parentColumns = ["id"], childColumns = ["matchId"], onDelete = ForeignKey.CASCADE)])
data class BallRow(@PrimaryKey val id: String, val matchId: String, val innings: Int, val sequence: Int, val payload: String)
@Entity(tableName = "competitions") data class CompetitionRow(@PrimaryKey val id: String, val payload: String)
@Dao interface BallnoteDao {
    @Query("SELECT * FROM competitions") suspend fun readCompetitions(): List<CompetitionRow>
    @Upsert suspend fun competition(row: CompetitionRow)
    @Query("DELETE FROM competitions") suspend fun clearCompetitions()
    @Query("DELETE FROM competitions WHERE id = :id") suspend fun deleteCompetition(id: String)
    @Query("SELECT * FROM teams") suspend fun readTeams(): List<TeamRow>
    @Query("SELECT * FROM matches") suspend fun readGames(): List<MatchRow>
    @Query("SELECT * FROM deliveries ORDER BY innings, sequence") suspend fun readBalls(): List<BallRow>
    @Query("SELECT * FROM teams") fun teams(): Flow<List<TeamRow>>
    @Query("SELECT * FROM matches") fun games(): Flow<List<MatchRow>>
    @Query("SELECT * FROM deliveries ORDER BY innings, sequence") fun balls(): Flow<List<BallRow>>
    @Upsert suspend fun team(row: TeamRow)
    @Upsert suspend fun game(row: MatchRow)
    @Upsert suspend fun innings(rows: List<InningsRow>)
    @Upsert suspend fun ball(row: BallRow)
    @Query("DELETE FROM deliveries WHERE matchId = :id") suspend fun clearBalls(id: String)
    @Query("DELETE FROM matches") suspend fun clearGames()
    @Query("DELETE FROM matches WHERE id IN (:ids)") suspend fun deleteGames(ids: List<String>)
    @Query("DELETE FROM teams") suspend fun clearTeams()
    @Query("DELETE FROM teams WHERE id IN (:ids)") suspend fun deleteTeams(ids: List<String>)
}
@Database(entities = [TeamRow::class, MatchRow::class, InningsRow::class, BallRow::class, CompetitionRow::class], version = 2, exportSchema = true)
abstract class BallnoteDb : RoomDatabase() { abstract fun dao(): BallnoteDao }
data class Library(val teams: List<Team> = emptyList(), val games: List<Game> = emptyList(), val balls: List<Delivery> = emptyList(), val competitions: List<Competition> = emptyList()) {
    fun deliveries(id: String) = balls.filter { it.matchId == id }
    fun score(g: Game) = Scoring.score(g, deliveries(g.id))
}
@Serializable data class Backup(val formatVersion: Int = 3, val teams: List<Team>, val matches: List<Game>, val deliveries: List<Delivery>, val competitions: List<Competition> = emptyList()) {
    fun validate() {
        require(formatVersion in 1..3) { "This backup version is not supported." }
        require(teams.map { it.id }.distinct().size == teams.size && matches.map { it.id }.distinct().size == matches.size && deliveries.map { it.id }.distinct().size == deliveries.size) { "Duplicate IDs in backup." }
        require(teams.all { it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 40 }) { "Invalid team in backup." }
        val teamIds = teams.map { it.id }.toSet(); val matchIds = matches.map { it.id }.toSet()
        matches.forEach { g ->
            g.rules.validate()
            g.timer?.validate()
            require(g.id.isNotBlank() && g.teamA in teamIds && g.teamB in teamIds && g.teamA != g.teamB && g.nameA.isNotBlank() && g.nameB.isNotBlank()) { "Invalid match in backup." }
            val ds = deliveries.filter { it.matchId == g.id }
            require(g.chaseStarted || ds.none { it.innings == 2 }) { "Chase deliveries without an innings." }
            require(ds.map { it.innings to it.sequence }.distinct().size == ds.size) { "Duplicate delivery positions." }
            require(Scoring.score(g, ds).excluded.isEmpty()) { "Backup contains deliveries after an innings ended." }
        }
        require(competitions.map { it.id }.distinct().size == competitions.size) { "Duplicate competition IDs." }
        val linked = competitions.flatMap { it.fixtures.mapNotNull { f -> f.matchId } }
        require(linked.distinct().size == linked.size) { "A match belongs to multiple fixtures." }
        competitions.forEach { c ->
            c.validate()
            c.fixtures.forEach { f ->
                require(f.matchId == null || f.matchId in matchIds) { "Missing fixture match." }
                val sides = Competitions.sides(c,f,matches,deliveries)
                f.matchId?.let { id -> val g=matches.first { it.id==id }; require(setOf(g.teamA,g.teamB)==setOf(sides.first,sides.second) && g.rules==c.rules) { "Fixture does not match its teams or rules." } }
                require(f.decision == null || f.decision in listOfNotNull(sides.first,sides.second)) { "Invalid fixture decision." }
            }
            Competitions.checkChanges(c,matches,deliveries,matches,deliveries)
        }
        deliveries.forEach { it.validate(); require(it.id.isNotBlank() && it.matchId in matchIds) { "Orphan delivery in backup." } }
    }
}
class Repository(context: Context, databaseName: String = "ballnote.db") {
    private val db = Room.databaseBuilder(context, BallnoteDb::class.java, databaseName).addMigrations(object : androidx.room.migration.Migration(1,2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS competitions (id TEXT NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(id))") }
    }).build()
    fun close() = db.close()
    private val dao = db.dao()
    suspend fun snapshot(): Library = db.withTransaction { Library(dao.readTeams().map { json.decodeFromString<Team>(it.payload) }, dao.readGames().map { json.decodeFromString<Game>(it.payload) }, dao.readBalls().map { json.decodeFromString<Delivery>(it.payload) }, dao.readCompetitions().map { json.decodeFromString<Competition>(it.payload) }) }
    val library = db.invalidationTracker.createFlow("teams", "matches", "deliveries", "competitions").map { snapshot() }
    private suspend fun team(t: Team) { require(t.name.isNotBlank() && t.name.length <= 40); dao.team(TeamRow(t.id, json.encodeToString(t))) }
    suspend fun create(g: Game) = db.withTransaction {
        g.rules.validate()
        require(g.teamA.isNotBlank() && g.teamB.isNotBlank() && g.teamA != g.teamB) { "Match sides must have different IDs." }
        require(g.nameA.isNotBlank() && g.nameB.isNotBlank() && g.nameA.length <= 40 && g.nameB.length <= 40 && !g.nameA.equals(g.nameB, true)) { "Enter two different team names, up to 40 characters each." }
        // Retrying a committed creation must not replace deliveries recorded since then.
        if (dao.readGames().any { it.id == g.id }) return@withTransaction
        team(Team(g.teamA, g.nameA)); team(Team(g.teamB, g.nameB))
        save(g, emptyList())
    }
    suspend fun save(g: Game, balls: List<Delivery>) = db.withTransaction {
        val before = snapshot()
        before.competitions.forEach { c -> Competitions.checkChanges(c,before.games,before.balls,before.games.filterNot { it.id==g.id }+g,before.balls.filterNot { it.matchId==g.id }+balls) }
        dao.game(MatchRow(g.id, json.encodeToString(g)))
        dao.innings(listOf(InningsRow(g.id, 1, g.firstClosed || g.chaseStarted)) + if (g.chaseStarted) listOf(InningsRow(g.id, 2, Scoring.score(g, balls).second.ended)) else emptyList())
        dao.clearBalls(g.id)
        balls.forEach { d -> dao.ball(BallRow(d.id, d.matchId, d.innings, d.sequence, json.encodeToString(d))) }
    }
    suspend fun competition(c: Competition) { c.validate(); dao.competition(CompetitionRow(c.id,json.encodeToString(c))) }
    suspend fun fixture(c: Competition, g: Game) = db.withTransaction { create(g); competition(c) }
    suspend fun changeTimer(scope: TimerScope, id: String, change: (SessionTimer?) -> SessionTimer?) = db.withTransaction {
        val l=snapshot()
        if(scope==TimerScope.MATCH) {
            val g=l.games.first {it.id==id}; val timer=change(g.timer);timer?.validate()
            if(timer!=g.timer) save(g.copy(timer=timer),l.deliveries(id))
        } else {
            val c=l.competitions.first {it.id==id};val timer=change(c.timer);timer?.validate()
            if(timer!=c.timer) competition(c.copy(timer=timer))
        }
    }
    suspend fun claimTimerAlert(ref: TimerRef, now: Long): TimerAlert? = db.withTransaction {
        var claimed: TimerAlert? = null
        val current=snapshot().sessionTimers().find {it.key==ref.key}
        if(current?.timer?.id==ref.timer.id) changeTimer(ref.scope,ref.ownerId) { timer ->
            val alert=timer?.alert(now)
            claimed=alert
            when(alert) {TimerAlert.EXPIRED->timer!!.copy(expiryDelivered=true,warningDelivered=true);TimerAlert.TEN_MINUTES->timer!!.copy(warningDelivered=true);null->timer}
        }
        claimed
    }
    suspend fun removeCompetition(id: String) { dao.deleteCompetition(id) }
    suspend fun restore(b: Backup) {
        b.validate()
        db.withTransaction { dao.clearCompetitions(); dao.clearGames(); dao.clearTeams(); b.teams.forEach { team(it) }; b.matches.forEach { save(it, b.deliveries.filter { d -> d.matchId == it.id }) }; b.competitions.forEach { competition(it) } }
    }
    suspend fun deleteMatches(ids: Set<String>): Backup = db.withTransaction {
        val current = snapshot()
        current.competitions.forEach { c -> Competitions.checkChanges(c,current.games,current.balls,current.games.filterNot { it.id in ids },current.balls.filterNot { it.matchId in ids }) }
        val affected = current.competitions.filter { c -> c.fixtures.any { it.matchId in ids } }
        val removed = current.games.filter { it.id in ids }
        val remaining = current.games.filterNot { it.id in ids }
        val usedSides = remaining.flatMap { listOf(it.teamA, it.teamB) }.toSet()
        val removedSides = removed.flatMap { listOf(it.teamA, it.teamB) }.toSet()
        val removedTeams = current.teams.filter { it.id !in usedSides && (remaining.isEmpty() || it.id in removedSides) }
        val backup = Backup(teams = current.teams.filter { it.id in removedSides || it in removedTeams }, matches = removed, deliveries = current.balls.filter { it.matchId in ids })
        if (removed.isNotEmpty()) {
            affected.forEach { c -> competition(c.copy(fixtures=c.fixtures.map { if(it.matchId in ids) it.copy(matchId=null,decision=null,reason="") else it })) }
            removed.map { it.id }.chunked(400).forEach { dao.deleteGames(it) }
            removedTeams.map { it.id }.chunked(400).forEach { dao.deleteTeams(it) }
        }
        backup
    }
    suspend fun undoDeletion(backup: Backup) {
        backup.validate()
        db.withTransaction {
            val current = snapshot()
            val currentIds = current.games.map { it.id }.toSet()
            // Do not overwrite matches that were restored or changed since deletion.
            val missing = backup.matches.filterNot { it.id in currentIds }
            val neededSides = missing.flatMap { listOf(it.teamA, it.teamB) }.toSet()
            val currentSides = current.teams.map { it.id }.toSet()
            backup.teams.filter { it.id !in currentSides && (it.id in neededSides || backup.matches.all { g -> g.id !in currentIds }) }.forEach { team(it) }
            missing.forEach { save(it, backup.deliveries.filter { d -> d.matchId == it.id }) }
        }
    }
}
val Context.preferences by preferencesDataStore("settings")
data class Preferences(val theme: String = "System", val dynamic: Boolean = false, val haptics: Boolean = true, val awake: Boolean = true, val rules: MatchRules = MatchRules())
class Settings(private val context: Context) {
    val values = context.preferences.data.map { p -> Preferences(p[stringPreferencesKey("theme")] ?: "System", p[booleanPreferencesKey("dynamic")] ?: false, p[booleanPreferencesKey("haptics")] ?: true, p[booleanPreferencesKey("awake")] ?: true, MatchRules(p[intPreferencesKey("overs")] ?: 6, p[intPreferencesKey("wickets")] ?: 5, ExtrasMode.valueOf(p[stringPreferencesKey("extras")] ?: "REBOWL"), p[intPreferencesKey("widePenalty")] ?: 1, p[intPreferencesKey("noBallPenalty")] ?: 1)) }
    suspend fun save(p: Preferences) { context.preferences.edit { it[stringPreferencesKey("theme")] = p.theme; it[booleanPreferencesKey("dynamic")] = p.dynamic; it[booleanPreferencesKey("haptics")] = p.haptics; it[booleanPreferencesKey("awake")] = p.awake; it[intPreferencesKey("overs")] = p.rules.overs; it[intPreferencesKey("wickets")] = p.rules.wickets; it[stringPreferencesKey("extras")] = p.rules.extras.name; it[intPreferencesKey("widePenalty")] = p.rules.widePenalty; it[intPreferencesKey("noBallPenalty")] = p.rules.noBallPenalty } }
}

fun Library.sessionTimers(): List<TimerRef> = games.mapNotNull {g->g.timer?.let {TimerRef(TimerScope.MATCH,g.id,g.nameA+" vs "+g.nameB,it)}} + competitions.mapNotNull {c->c.timer?.let {TimerRef(TimerScope.COMPETITION,c.id,c.name,it)}}
