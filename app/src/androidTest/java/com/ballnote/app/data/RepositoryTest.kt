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
            repo.team(a); repo.team(b); repo.save(g, listOf(d))
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
}
