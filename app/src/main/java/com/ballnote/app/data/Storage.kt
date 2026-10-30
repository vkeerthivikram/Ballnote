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
    @Query("DELETE FROM teams") suspend fun clearTeams()
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
    suspend fun team(t: Team) { require(t.name.isNotBlank() && t.name.length <= 40); dao.team(TeamRow(t.id, json.encodeToString(t))) }
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
}
val Context.preferences by preferencesDataStore("settings")
data class Preferences(val theme: String = "System", val dynamic: Boolean = false, val haptics: Boolean = true, val awake: Boolean = true, val rules: MatchRules = MatchRules())
class Settings(private val context: Context) {
    val values = context.preferences.data.map { p -> Preferences(p[stringPreferencesKey("theme")] ?: "System", p[booleanPreferencesKey("dynamic")] ?: false, p[booleanPreferencesKey("haptics")] ?: true, p[booleanPreferencesKey("awake")] ?: true, MatchRules(p[intPreferencesKey("overs")] ?: 6, p[intPreferencesKey("wickets")] ?: 5, ExtrasMode.valueOf(p[stringPreferencesKey("extras")] ?: "REBOWL"))) }
    suspend fun save(p: Preferences) { context.preferences.edit { it[stringPreferencesKey("theme")] = p.theme; it[booleanPreferencesKey("dynamic")] = p.dynamic; it[booleanPreferencesKey("haptics")] = p.haptics; it[booleanPreferencesKey("awake")] = p.awake; it[intPreferencesKey("overs")] = p.rules.overs; it[intPreferencesKey("wickets")] = p.rules.wickets; it[stringPreferencesKey("extras")] = p.rules.extras.name } }
}
