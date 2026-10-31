package com.ballnote.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.ballnote.app.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import com.ballnote.app.ui.resultFile
import android.graphics.BitmapFactory

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    @Test fun generateShareableResultWithoutNetwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val g = Game(teamA = "a", teamB = "b", nameA = "Weekend XI", nameB = "Turf Titans", abandoned = true)
        val uri = resultFile(context, g, Scoring.score(g, emptyList()))
        val image = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        assertEquals(1080, image.width); assertEquals(1080, image.height); image.recycle()
    }
    @Test fun persistentRoundTripAndAtomicRestore() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${newId()}.db"
        var repo = Repository(context, name)
        try {
            val a = Team(name = "First"); val b = Team(name = "Second")
            val g = Game(teamA = a.id, teamB = b.id, nameA = a.name, nameB = b.name)
            val d = Delivery(matchId = g.id, innings = 1, sequence = 0, runs = 4)
            repo.create(g); repo.save(g, listOf(d))
            repo.close(); repo = Repository(context, name)
            val saved = repo.snapshot(); assertEquals(4, saved.score(saved.games.single()).first.runs)
            val backup = Backup(teams = saved.teams, matches = saved.games, deliveries = saved.balls)
            try { repo.restore(backup.copy(teams = emptyList())); fail("Invalid restore accepted") } catch (_: IllegalArgumentException) { }
            assertEquals(saved, repo.snapshot())
            repo.restore(Backup(teams = emptyList(), matches = emptyList(), deliveries = emptyList()))
            assertTrue(repo.snapshot().games.isEmpty())
            repo.restore(backup)
            assertEquals(saved, repo.snapshot())
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun repeatedNamesBelongToIndependentMatches() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${newId()}.db"
        val repo = Repository(context, name)
        try {
            val morning = Game(teamA = newId(), teamB = newId(), nameA = "Team A", nameB = "Team B")
            val evening = Game(teamA = newId(), teamB = newId(), nameA = "Team A", nameB = "Team B")
            repo.create(morning); repo.create(evening)
            val ball = Delivery(matchId = morning.id, innings = 1, sequence = 0, runs = 4)
            repo.save(morning, listOf(ball))
            repo.create(morning)
            repo.close()
            val reopened = Repository(context, name)
            try {
                val saved = reopened.snapshot()
                assertEquals(2, saved.games.size)
                assertEquals(4, saved.games.flatMap { listOf(it.teamA, it.teamB) }.distinct().size)
                assertEquals(4, saved.score(saved.games.first { it.id == morning.id }).first.runs)
                assertEquals(0, saved.score(saved.games.first { it.id == evening.id }).first.runs)
                Backup(teams = saved.teams, matches = saved.games, deliveries = saved.balls).validate()
            } finally { reopened.close() }
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun restoreLegacyTeamsWithoutChangingMatchNames() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${newId()}.db"
        val repo = Repository(context, name)
        try {
            val morning = Game(teamA = "a", teamB = "b", nameA = "Morning A", nameB = "Morning B")
            val evening = morning.copy(id = newId(), nameA = "Evening A", nameB = "Evening B")
            val backup = Backup(teams = listOf(Team("a", "Renamed A", archived = true), Team("b", "Renamed B")), matches = listOf(morning, evening), deliveries = emptyList())
            repo.restore(backup)
            val saved = repo.snapshot()
            assertEquals(setOf("Morning A", "Evening A"), saved.games.map { it.nameA }.toSet())
            assertEquals(setOf("Morning B", "Evening B"), saved.games.map { it.nameB }.toSet())
            assertEquals(backup.teams.toSet(), saved.teams.toSet())
        } finally { repo.close(); context.deleteDatabase(name) }
    }
}
