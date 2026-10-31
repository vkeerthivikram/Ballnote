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
import android.graphics.Bitmap
import java.io.File

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    @Test fun generateShareableResultWithoutNetwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val g = Game(teamA = "a", teamB = "b", nameA = "Weekend XI", nameB = "Turf Titans", abandoned = true)
        val chase = g.copy(abandoned = false, chaseStarted = true, firstClosed = true, rules = MatchRules(1, 5))
        val first = Delivery(matchId = chase.id, innings = 1, sequence = 0, runs = 4)
        val win = listOf(first, Delivery(matchId = chase.id, innings = 2, sequence = 0, runs = 6))
        val tie = listOf(first) + (0..5).map { Delivery(matchId = chase.id, innings = 2, sequence = it, runs = if (it < 4) 1 else 0) }
        val longNames = chase.copy(nameA = "Weekend Cricket Club Morning Team Alpha", nameB = "Turf Cricket Club Evening Team Champions")
        listOf(Triple("no-result", g, emptyList()), Triple("completed", chase, win), Triple("tied", chase, tie), Triple("long-names", longNames, win)).forEach { (label, match, deliveries) ->
            val uri = resultFile(context, match, Scoring.score(match, deliveries))
            val image = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            assertEquals(1080, image.width); assertEquals(1080, image.height)
            File(context.filesDir, "result-$label.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
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

    @Test fun deleteAndUndoPreserveSharedLegacySidesAndOtherMatches() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${newId()}.db"
        var repo = Repository(context, name)
        try {
            val first = Game(teamA = "a", teamB = "b", nameA = "First", nameB = "Second")
            val other = Game(teamA = "a", teamB = "c", nameA = "First", nameB = "Third")
            val original = Backup(teams = listOf(Team("a", "First"), Team("b", "Second"), Team("c", "Third"), Team("old", "Archived", true)),
                matches = listOf(first, other), deliveries = listOf(Delivery(matchId = first.id, innings = 1, sequence = 0, runs = 4), Delivery(matchId = other.id, innings = 1, sequence = 0, runs = 1)))
            repo.restore(original)
            val deleted = repo.deleteMatches(setOf(first.id))
            repo.close(); repo = Repository(context, name)
            val kept = repo.snapshot()
            assertEquals(listOf(other), kept.games)
            assertEquals(setOf("a", "c", "old"), kept.teams.map { it.id }.toSet())
            assertEquals(listOf(other.id), kept.balls.map { it.matchId })
            Backup(teams = kept.teams, matches = kept.games, deliveries = kept.balls).validate()
            repo.undoDeletion(deleted)
            val restored = repo.snapshot()
            assertEquals(original.matches.toSet(), restored.games.toSet())
            assertEquals(original.deliveries.toSet(), restored.balls.toSet())
            assertEquals(original.teams.toSet(), restored.teams.toSet())
        } finally { repo.close(); context.deleteDatabase(name) }
    }

    @Test fun deleteAllAndUndoDoNotOverwriteNewOrChangedMatches() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${newId()}.db"
        val repo = Repository(context, name)
        try {
            fun game() = Game(teamA = newId(), teamB = newId(), nameA = "Team A", nameB = "Team B")
            val first = game(); val second = game()
            repo.create(first); repo.create(second)
            val ball = Delivery(matchId = first.id, innings = 1, sequence = 0, runs = 4)
            repo.save(first, listOf(ball))
            val deleted = repo.deleteMatches(setOf(first.id, second.id))
            assertEquals(Library(), repo.snapshot())
            val newMatch = game(); repo.create(newMatch)
            repo.undoDeletion(deleted)
            assertEquals(3, repo.snapshot().games.size)
            repo.save(first, listOf(ball.copy(runs = 6)))
            repo.undoDeletion(deleted)
            val saved = repo.snapshot()
            assertEquals(6, saved.score(saved.games.first { it.id == first.id }).first.runs)
            assertEquals(3, saved.games.size)
            Backup(teams = saved.teams, matches = saved.games, deliveries = saved.balls).validate()
        } finally { repo.close(); context.deleteDatabase(name) }
    }
}
