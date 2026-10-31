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
@Dao interface BallnoteDao {
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
@Database(entities = [TeamRow::class, MatchRow::class, InningsRow::class, BallRow::class], version = 1, exportSchema = true)
abstract class BallnoteDb : RoomDatabase() { abstract fun dao(): BallnoteDao }
data class Library(val teams: List<Team> = emptyList(), val games: List<Game> = emptyList(), val balls: List<Delivery> = emptyList()) {
    fun deliveries(id: String) = balls.filter { it.matchId == id }
    fun score(g: Game) = Scoring.score(g, deliveries(g.id))
}
@Serializable data class Backup(val formatVersion: Int = 1, val teams: List<Team>, val matches: List<Game>, val deliveries: List<Delivery>) {
    fun validate() {
        require(formatVersion == 1) { "This backup version is not supported." }
        require(teams.map { it.id }.distinct().size == teams.size && matches.map { it.id }.distinct().size == matches.size && deliveries.map { it.id }.distinct().size == deliveries.size) { "Duplicate IDs in backup." }
        require(teams.all { it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 40 }) { "Invalid team in backup." }
        val teamIds = teams.map { it.id }.toSet(); val matchIds = matches.map { it.id }.toSet()
        matches.forEach { g ->
            g.rules.validate()
            require(g.id.isNotBlank() && g.teamA in teamIds && g.teamB in teamIds && g.teamA != g.teamB && g.nameA.isNotBlank() && g.nameB.isNotBlank()) { "Invalid match in backup." }
            val ds = deliveries.filter { it.matchId == g.id }
            require(g.chaseStarted || ds.none { it.innings == 2 }) { "Chase deliveries without an innings." }
            require(ds.map { it.innings to it.sequence }.distinct().size == ds.size) { "Duplicate delivery positions." }
            require(Scoring.score(g, ds).excluded.isEmpty()) { "Backup contains deliveries after an innings ended." }
        }
        deliveries.forEach { it.validate(); require(it.id.isNotBlank() && it.matchId in matchIds) { "Orphan delivery in backup." } }
    }
}
class Repository(context: Context, databaseName: String = "ballnote.db") {
    private val db = Room.databaseBuilder(context, BallnoteDb::class.java, databaseName).build()
    fun close() = db.close()
    private val dao = db.dao()
    suspend fun snapshot(): Library = db.withTransaction { Library(dao.readTeams().map { json.decodeFromString<Team>(it.payload) }, dao.readGames().map { json.decodeFromString<Game>(it.payload) }, dao.readBalls().map { json.decodeFromString<Delivery>(it.payload) }) }
    val library = db.invalidationTracker.createFlow("teams", "matches", "deliveries").map { snapshot() }
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
        dao.game(MatchRow(g.id, json.encodeToString(g)))
        dao.innings(listOf(InningsRow(g.id, 1, g.firstClosed || g.chaseStarted)) + if (g.chaseStarted) listOf(InningsRow(g.id, 2, Scoring.score(g, balls).second.ended)) else emptyList())
        dao.clearBalls(g.id)
        balls.forEach { d -> dao.ball(BallRow(d.id, d.matchId, d.innings, d.sequence, json.encodeToString(d))) }
    }
    suspend fun restore(b: Backup) {
        b.validate()
        db.withTransaction { dao.clearGames(); dao.clearTeams(); b.teams.forEach { team(it) }; b.matches.forEach { save(it, b.deliveries.filter { d -> d.matchId == it.id }) } }
    }
    suspend fun deleteMatches(ids: Set<String>): Backup = db.withTransaction {
        val current = snapshot()
        val removed = current.games.filter { it.id in ids }
        val remaining = current.games.filterNot { it.id in ids }
        val usedSides = remaining.flatMap { listOf(it.teamA, it.teamB) }.toSet()
        val removedSides = removed.flatMap { listOf(it.teamA, it.teamB) }.toSet()
        val removedTeams = current.teams.filter { it.id !in usedSides && (remaining.isEmpty() || it.id in removedSides) }
        val backup = Backup(teams = current.teams.filter { it.id in removedSides || it in removedTeams }, matches = removed, deliveries = current.balls.filter { it.matchId in ids })
        if (removed.isNotEmpty()) {
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
    val values = context.preferences.data.map { p -> Preferences(p[stringPreferencesKey("theme")] ?: "System", p[booleanPreferencesKey("dynamic")] ?: false, p[booleanPreferencesKey("haptics")] ?: true, p[booleanPreferencesKey("awake")] ?: true, MatchRules(p[intPreferencesKey("overs")] ?: 6, p[intPreferencesKey("wickets")] ?: 5, ExtrasMode.valueOf(p[stringPreferencesKey("extras")] ?: "REBOWL"))) }
    suspend fun save(p: Preferences) { context.preferences.edit { it[stringPreferencesKey("theme")] = p.theme; it[booleanPreferencesKey("dynamic")] = p.dynamic; it[booleanPreferencesKey("haptics")] = p.haptics; it[booleanPreferencesKey("awake")] = p.awake; it[intPreferencesKey("overs")] = p.rules.overs; it[intPreferencesKey("wickets")] = p.rules.wickets; it[stringPreferencesKey("extras")] = p.rules.extras.name } }
}
