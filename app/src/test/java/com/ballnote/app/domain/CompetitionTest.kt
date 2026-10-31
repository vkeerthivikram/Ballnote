package com.ballnote.app.domain

import com.ballnote.app.data.Backup
import com.ballnote.app.data.json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class CompetitionTest {
    private fun competition(format: Format, count: Int=4, legs: Int=1) = Competitions.create(Competition(name="Weekend",format=format,teams=(1..count).map {Entrant(id="t$it",name="Team $it")},legs=legs))
    private fun result(c: Competition,f: Fixture,winnerFirst: Boolean=true): Pair<Game,List<Delivery>> {
        val g=Game(id="match-${f.id}",teamA=f.a!!,teamB=f.b!!,nameA=c.team(f.a),nameB=c.team(f.b),rules=c.rules,firstClosed=true,chaseStarted=true)
        val ds=listOf(Delivery(matchId=g.id,innings=1,sequence=0,runs=1)) + if(winnerFirst) (0 until c.rules.wickets).map {Delivery(matchId=g.id,innings=2,sequence=it,wicket=true)} else listOf(Delivery(matchId=g.id,innings=2,sequence=0,runs=2))
        return g to ds
    }
    @Test fun penaltiesAffectTotalsTargetAndRecapAcrossCorrections() {
        val rules=MatchRules(1,2,ExtrasMode.COUNT,2,2)
        val g=Game(teamA="a",teamB="b",nameA="A",nameB="B",rules=rules)
        val ds=listOf(Delivery(matchId=g.id,innings=1,sequence=0,kind=Kind.WIDE,runs=1),Delivery(matchId=g.id,innings=1,sequence=1,kind=Kind.NO_BALL,runs=4),Delivery(matchId=g.id,innings=1,sequence=2),Delivery(matchId=g.id,innings=1,sequence=3,runs=6))
        val s=Scoring.score(g,ds)
        assertEquals(15,s.first.runs);assertEquals(16,s.target);assertEquals(5,s.first.extrasTotal);assertEquals(1,s.first.dots);assertEquals(1,s.first.fours);assertEquals(1,s.first.sixes)
        val corrected=Scoring.preview(g,ds,ds[0].id,ds[0].copy(runs=0))
        assertEquals(14,corrected.after.first.runs);assertEquals(14,corrected.after.first.oversBreakdown.single().runs)
    }
    @Test fun roundRobinGeneratesEachPairOnceOrTwiceAndGroupsStaySeparate() {
        val c=competition(Format.LEAGUE,5)
        assertEquals(10,c.fixtures.size);assertEquals(10,c.fixtures.map {setOf(it.a,it.b)}.distinct().size)
        assertEquals(20,competition(Format.LEAGUE,5,2).fixtures.size)
        val grouped=competition(Format.GROUPS,8)
        assertEquals(12,grouped.fixtures.size)
        grouped.fixtures.forEach {assertEquals(grouped.teams.indexOfFirst {t->t.id==it.a}%2,grouped.teams.indexOfFirst {t->t.id==it.b}%2)}
    }
    @Test fun everyKnockoutSizeHasNoEmptyPairAndAdvancesAllTeams() {
        for(n in 2..16) {
            val c=competition(Format.KNOCKOUT,n)
            val initial=c.fixtures.filter {it.sourceA==null}
            assertEquals(n,initial.flatMap {listOfNotNull(it.a,it.b)}.distinct().size)
            assertTrue(initial.all {it.a!=null});assertEquals(1,c.fixtures.count {it.label=="Final"})
            c.validate()
        }
    }
    @Test fun leagueResultsAndDecisionsProducePointsWithoutFakeNrr() {
        val base=competition(Format.LEAGUE,3); val f=base.fixtures.first();val (g,ds)=result(base,f)
        val c=base.copy(fixtures=base.fixtures.map {if(it.id==f.id) it.copy(matchId=g.id) else it})
        val table=Competitions.table(c,listOf(g),ds)
        assertEquals(f.a,table.first().id);assertEquals(2,table.first().points);assertEquals(1,table.first().played)
        // Early closed first innings is explicitly excluded from NRR.
        assertEquals(0.0,table.first().nrr,0.00001)
        val decision=c.copy(fixtures=c.fixtures.map {if(it.id==f.id) it.copy(decision=f.b,reason="Walkover") else it})
        assertEquals(f.b,Competitions.table(decision,listOf(g),ds).first().id)
    }
    @Test fun qualifiersAndCorrectionsCannotSilentlyChangeAStartedRound() {
        val base=competition(Format.PLAYOFFS,4)
        val results=base.fixtures.map {result(base,it)};val games=results.map {it.first};val balls=results.flatMap {it.second}
        val c=base.copy(fixtures=base.fixtures.mapIndexed {i,f->f.copy(matchId=games[i].id)})
        val advanced=Competitions.advance(c,games,balls)
        assertEquals(2,advanced.lockedSeeds.size)
        assertThrows(IllegalArgumentException::class.java) {Competitions.checkChanges(advanced,games,balls,emptyList(),emptyList())}
        val bracket=competition(Format.KNOCKOUT,4)
        val decided=bracket.copy(fixtures=bracket.fixtures.mapIndexed {i,f->if(i<2) f.copy(decision=f.a,reason="Tie-break") else f.copy(matchId="final")})
        val sides=Competitions.sides(decided,decided.fixtures.last(),emptyList(),emptyList())
        assertEquals("t1",sides.first);assertEquals("t3",sides.second)
    }
    @Test fun seriesBestOfAndSessionHaveIndependentOptions() {
        val base=competition(Format.BILATERAL,2)
        assertEquals(3,base.fixtures.size)
        val c=Competitions.create(base.copy(openEnded=true))
        assertEquals(1,c.fixtures.size)
        assertThrows(IllegalArgumentException::class.java) {Competitions.create(base.copy(bestOf=true,matchCount=4))}
    }
    @Test fun allOutNrrUsesFullQuotaAndTiesAndNoResultsEarnOnePoint() {
        val base=competition(Format.LEAGUE,2).copy(rules=MatchRules(2,1));val f=base.fixtures.single()
        val g=Game(teamA=f.a!!,teamB=f.b!!,nameA="A",nameB="B",rules=base.rules,chaseStarted=true)
        val ds=listOf(Delivery(matchId=g.id,innings=1,sequence=0,runs=6),Delivery(matchId=g.id,innings=1,sequence=1,wicket=true),Delivery(matchId=g.id,innings=2,sequence=0,wicket=true))
        val c=base.copy(fixtures=listOf(f.copy(matchId=g.id)))
        assertEquals(3.0,Competitions.table(c,listOf(g),ds).first().nrr,0.00001)
        val tied=ds.dropLast(1)+Delivery(matchId=g.id,innings=2,sequence=0,runs=6)+Delivery(matchId=g.id,innings=2,sequence=1,wicket=true)
        assertTrue(Competitions.table(c,listOf(g),tied).all {it.points==1 && it.ties==1})
        assertTrue(Competitions.table(c,listOf(g.copy(abandoned=true)),ds).all {it.points==1 && it.noResults==1})
    }
    @Test fun aChangedSemifinalWinnerCannotChangeAStartedFinal() {
        val base=competition(Format.KNOCKOUT,4);val f=base.fixtures.first();val (g,ds)=result(base,f)
        val c=base.copy(fixtures=base.fixtures.mapIndexed {i,x->when(i){0->x.copy(matchId=g.id);1->x.copy(decision=x.a,reason="Walkover");else->x.copy(matchId="started-final")}})
        val changed=ds.filter {it.innings==1}+Delivery(matchId=g.id,innings=2,sequence=0,runs=2)
        assertThrows(IllegalArgumentException::class.java) {Competitions.checkChanges(c,listOf(g),ds,listOf(g),changed)}
    }
    @Test fun backupV1DefaultsAndV2CompetitionRoundTrip() {
        val old=json.decodeFromString<Backup>("""{"formatVersion":1,"teams":[],"matches":[],"deliveries":[]}""")
        old.validate();assertTrue(old.competitions.isEmpty())
        val c=competition(Format.LEAGUE)
        val b=Backup(teams=emptyList(),matches=emptyList(),deliveries=emptyList(),competitions=listOf(c))
        val copy=json.decodeFromString<Backup>(json.encodeToString(b));copy.validate();assertEquals(c,copy.competitions.single())
    }
}
