package com.ballnote.app.data
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ballnote.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompetitionStorageTest {
    @Test fun migrateV1AndRoundTripLinkedCompetition() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val name="migration-"+newId()+".db"; val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val legacy=Game(teamA="old-a",teamB="old-b",nameA="Old A",nameB="Old B")
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            old.execSQL("CREATE TABLE IF NOT EXISTS `teams` (`id` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))")
            old.execSQL("CREATE TABLE IF NOT EXISTS `matches` (`id` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))")
            old.execSQL("CREATE TABLE IF NOT EXISTS `innings` (`matchId` TEXT NOT NULL, `number` INTEGER NOT NULL, `closed` INTEGER NOT NULL, PRIMARY KEY(`matchId`, `number`), FOREIGN KEY(`matchId`) REFERENCES `matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            old.execSQL("CREATE TABLE IF NOT EXISTS `deliveries` (`id` TEXT NOT NULL, `matchId` TEXT NOT NULL, `innings` INTEGER NOT NULL, `sequence` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`matchId`) REFERENCES `matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            old.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_deliveries_matchId_innings_sequence` ON `deliveries` (`matchId`, `innings`, `sequence`)")
            old.execSQL("INSERT INTO matches (id,payload) VALUES (?,?)",arrayOf(legacy.id,json.encodeToString(legacy)))
            old.execSQL("INSERT INTO teams (id,payload) VALUES (?,?)",arrayOf("old-a",json.encodeToString(Team("old-a","Old A"))))
            old.execSQL("INSERT INTO teams (id,payload) VALUES (?,?)",arrayOf("old-b",json.encodeToString(Team("old-b","Old B"))))
            old.version=1
        }
        var repo=Repository(context,name)
        try {
            assertEquals(legacy,repo.snapshot().games.single())
            val c=Competitions.create(Competition(name="Series",format=Format.BILATERAL,teams=listOf(Entrant(name="A"),Entrant(name="B")),rules=MatchRules(widePenalty=2)))
            repo.competition(c)
            val f=c.fixtures.first(); val g=Game(teamA=f.a!!,teamB=f.b!!,nameA="A",nameB="B",rules=c.rules)
            repo.fixture(c.copy(fixtures=c.fixtures.map {if(it.id==f.id) it.copy(matchId=g.id) else it}),g)
            repo.save(g,listOf(Delivery(matchId=g.id,innings=1,sequence=0,kind=Kind.WIDE)))
            repo.close();repo=Repository(context,name)
            val l=repo.snapshot();assertEquals(2,l.score(l.games.first {it.id==g.id}).first.runs)
            val backup=Backup(teams=l.teams,matches=l.games,deliveries=l.balls,competitions=l.competitions)
            backup.validate();repo.restore(json.decodeFromString<Backup>(json.encodeToString(backup)))
            assertEquals(l,repo.snapshot())
        } finally {repo.close();context.deleteDatabase(name)}
    }
}
