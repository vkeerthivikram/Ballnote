package com.ballnote.app.domain

import kotlinx.serialization.Serializable

@Serializable enum class Format(val title: String) {
    BILATERAL("Bilateral / session"), TRI("Tri-series"), LEAGUE("League"), KNOCKOUT("Knockout"), PLAYOFFS("League + playoffs"), GROUPS("Groups + knockout")
}
@Serializable data class Entrant(val id: String = newId(), val name: String)
@Serializable data class Fixture(
    val id: String = newId(), val label: String, val a: String? = null, val b: String? = null,
    val sourceA: String? = null, val sourceB: String? = null, val loserA: Boolean = false, val loserB: Boolean = false,
    val group: Int = 0, val elimination: Boolean = false, val matchId: String? = null,
    val decision: String? = null, val reason: String = "", val bye: Boolean = false
)
@Serializable data class Competition(
    val id: String = newId(), val name: String, val format: Format, val teams: List<Entrant>,
    val rules: MatchRules = MatchRules(), val legs: Int = 1, val matchCount: Int = 3,
    val bestOf: Boolean = false, val openEnded: Boolean = false, val groups: Int = 2, val qualifiers: Int = 2,
    val iplPlayoffs: Boolean = false, val fixtures: List<Fixture> = emptyList(), val lockedSeeds: List<String> = emptyList(),
    val finalsEnabled: Boolean = true, val closed: Boolean = false, val withdrawn: Set<String> = emptySet(), val created: Long = System.currentTimeMillis()
) {
    fun validate() {
        rules.validate()
        require(name.isNotBlank() && name.length <= 80 && teams.size in 2..16) { "Use a name and 2–16 teams." }
        require(teams.map { it.id }.distinct().size == teams.size && teams.map { it.name.trim().lowercase() }.distinct().size == teams.size && teams.all { it.id.isNotBlank() && it.name.isNotBlank() && it.name.length <= 40 }) { "Use distinct team names." }
        require((format != Format.BILATERAL || teams.size == 2) && (format != Format.TRI || teams.size == 3)) { "Bilateral needs two teams; tri-series needs three." }
        require(legs in 1..2 && matchCount in 1..99 && qualifiers in listOf(2,4) && groups in 2..4) { "Invalid competition options." }
        require(!bestOf || matchCount % 2 == 1) { "Best-of series needs an odd match count." }
        require(format != Format.GROUPS || teams.size >= groups * qualifiers) { "Each group needs at least two teams." }
        require(format != Format.PLAYOFFS || teams.size >= if(iplPlayoffs) 4 else qualifiers) { "Not enough teams for playoffs." }
        require(format != Format.TRI || qualifiers == 2) { "Tri-series qualifies two teams." }
        require(fixtures.map { it.id }.distinct().size == fixtures.size && fixtures.mapNotNull { it.matchId }.distinct().size == fixtures.count { it.matchId != null }) { "Duplicate fixture or match." }
        val ids = teams.map { it.id }.toSet(); val seen = mutableSetOf<String>()
        fixtures.forEach { f ->
            require(f.id.isNotBlank() && listOfNotNull(f.a,f.b,f.decision).all { it in ids } && (f.a == null || f.a != f.b)) { "Invalid fixture team." }
            require(listOfNotNull(f.sourceA,f.sourceB).all { it in seen }) { "Invalid fixture dependency." }
            seen += f.id
        }
        require(withdrawn.all { it in ids } && lockedSeeds.all { it in ids }) { "Invalid competition team." }
    }
    fun team(id: String?) = teams.find { it.id == id }?.name ?: "To be decided"
}
data class Standing(val id: String, val played: Int, val wins: Int, val losses: Int, val ties: Int, val noResults: Int, val points: Int, val nrr: Double)

object Competitions {
    fun create(c: Competition): Competition {
        c.validate()
        val ids = c.teams.map { it.id }
        val fixtures = when(c.format) {
            Format.BILATERAL -> (1..if(c.openEnded) 1 else c.matchCount).map { Fixture(label = "Match $it", a=ids[0], b=ids[1]) }
            Format.KNOCKOUT -> bracket(ids)
            else -> {
                val pools = if(c.format == Format.GROUPS) (0 until c.groups).map { group -> ids.filterIndexed { index, _ -> index % c.groups == group } } else listOf(ids)
                pools.flatMapIndexed { group, pool -> (1..c.legs).flatMap { leg ->
                    pool.flatMapIndexed { i, a -> pool.drop(i+1).map { b -> Fixture(label="${if(c.format == Format.GROUPS) "Group ${group+1} · " else ""}Round $leg", a=if(leg==1) a else b, b=if(leg==1) b else a, group=group) } }
                } }
            }
        }
        return c.copy(fixtures=fixtures)
    }
    fun sides(c: Competition, f: Fixture, games: List<Game>, balls: List<Delivery>): Pair<String?,String?> =
        (f.a ?: f.sourceA?.let { outcome(c,it,games,balls,f.loserA) }) to (f.b ?: f.sourceB?.let { outcome(c,it,games,balls,f.loserB) })
    fun outcome(c: Competition, id: String, games: List<Game>, balls: List<Delivery>, loser: Boolean = false): String? {
        val f = c.fixtures.find { it.id == id } ?: return null
        val (a,b) = sides(c,f,games,balls)
        val g = games.find { it.id == f.matchId }
        val score = g?.let { Scoring.score(it,balls.filter { d -> d.matchId == it.id }) }
        val winner = f.decision ?: if(f.bye) a ?: b else score?.winner
        return if(loser) if(winner == null) null else if(winner==a) b else a else winner
    }
    fun finished(c: Competition, f: Fixture, games: List<Game>, balls: List<Delivery>): Boolean = f.bye || f.decision != null || games.find { it.id == f.matchId }?.let { Scoring.score(it, balls.filter { d -> d.matchId == it.id }).finished } == true
    fun table(c: Competition, games: List<Game>, balls: List<Delivery>, group: Int? = null): List<Standing> {
        val pool = if(group == null) c.teams.map { it.id } else c.teams.filterIndexed { i,_ -> i % c.groups == group }.map { it.id }
        return pool.map { id ->
            var played=0; var wins=0; var losses=0; var ties=0; var nr=0; var runsFor=0; var runsAgainst=0; var ballsFor=0; var ballsAgainst=0
            c.fixtures.filter { !it.elimination && (group == null || it.group==group) }.forEach { f ->
                val (a,b)=sides(c,f,games,balls)
                if(id==a || id==b) {
                    val g=games.find { it.id==f.matchId }; val s=g?.let { Scoring.score(it,balls.filter { d -> d.matchId==it.id }) }
                    if(f.decision != null || s?.finished==true) {
                        played++
                        val winner=f.decision ?: s?.winner
                        when { winner==id -> wins++; winner!=null -> losses++; s?.status==Status.ABANDONED -> nr++; else -> ties++ }
                        if(s?.status==Status.COMPLETE && !s.first.early && f.decision==null) {
                            val own=if(g!!.teamA==id) s.first else s.second; val other=if(g.teamA==id) s.second else s.first
                            runsFor+=own.runs; runsAgainst+=other.runs
                            ballsFor+=if(own.wickets>=g.rules.wickets) g.rules.overs*6 else own.balls
                            ballsAgainst+=if(other.wickets>=g.rules.wickets) g.rules.overs*6 else other.balls
                        }
                    }
                }
            }
            Standing(id,played,wins,losses,ties,nr,wins*2+ties+nr,if(ballsFor>0 && ballsAgainst>0) runsFor*6.0/ballsFor-runsAgainst*6.0/ballsAgainst else 0.0)
        }.sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { it.wins }.thenByDescending { it.nrr }.thenBy { pool.indexOf(it.id) })
    }
    fun seeds(c: Competition, games: List<Game>, balls: List<Delivery>): List<String> =
        if(c.format==Format.GROUPS) (0 until c.groups).flatMap { table(c,games,balls,it).filterNot { row -> row.id in c.withdrawn }.take(c.qualifiers).map { it.id } }
        else table(c,games,balls).filterNot { it.id in c.withdrawn }.take(if(c.iplPlayoffs) 4 else c.qualifiers).map { it.id }
    fun advance(c: Competition, games: List<Game>, balls: List<Delivery>): Competition {
        require(c.finalsEnabled && c.format in listOf(Format.TRI,Format.PLAYOFFS,Format.GROUPS) && c.fixtures.none { it.elimination }) { "Playoffs already exist or are not needed." }
        require(c.fixtures.all { finished(c,it,games,balls) }) { "Finish all league fixtures first." }
        val ids=seeds(c,games,balls); require(ids.size>=2) { "Need at least two eligible teams." }
        val next=if(c.iplPlayoffs && ids.size==4) {
            val q=Fixture(label="Qualifier 1",a=ids[0],b=ids[1],elimination=true)
            val e=Fixture(label="Eliminator",a=ids[2],b=ids[3],elimination=true)
            val q2=Fixture(label="Qualifier 2",sourceA=q.id,loserA=true,sourceB=e.id,elimination=true)
            listOf(q,e,q2,Fixture(label="Final",sourceA=q.id,sourceB=q2.id,elimination=true))
        } else bracket(if(c.format==Format.GROUPS && c.groups==2 && ids.size==4) listOf(ids[0],ids[3],ids[1],ids[2]) else ids)
        return c.copy(fixtures=c.fixtures+next,lockedSeeds=ids)
    }
    fun bracket(ids: List<String>): List<Fixture> {
        var size=2; while(size<ids.size) size*=2
        val slots=ids.toMutableList<String?>(); while(slots.size<size) slots.add(null)
        // Spread byes across first-round pairs rather than creating empty fixtures.
        val pairs=mutableListOf<Pair<String?,String?>>()
        val byes=size-ids.size; var index=0
        repeat(byes) { pairs+=slots[index++] to null }
        while(index<ids.size) { pairs+=slots[index] to slots[index+1]; index+=2 }
        val result=mutableListOf<Fixture>()
        var round=pairs.map { (a,b) -> Fixture(label=if(size==2) "Final" else "Round of $size",a=a,b=b,elimination=true,bye=b==null) }
        result+=round
        while(round.size>1) {
            round=round.chunked(2).map { Fixture(label=if(round.size==2) "Final" else "Round of ${round.size}",sourceA=it[0].id,sourceB=it[1].id,elimination=true) }
            result+=round
        }
        return result
    }
    fun checkChanges(c: Competition, beforeGames: List<Game>, beforeBalls: List<Delivery>, afterGames: List<Game>, afterBalls: List<Delivery>) {
        if(c.lockedSeeds.isNotEmpty()) require(seeds(c,afterGames,afterBalls)==c.lockedSeeds && c.fixtures.filterNot { it.elimination }.all { finished(c,it,afterGames,afterBalls) }) { "This changes playoff qualification. Reset playoffs in the competition first; recorded matches will be kept." }
        c.fixtures.filter { (it.matchId != null || it.decision != null) && (it.sourceA != null || it.sourceB != null) }.forEach { f ->
            require(sides(c,f,beforeGames,beforeBalls)==sides(c,f,afterGames,afterBalls)) { "This changes teams in a later fixture. Reset knockout rounds first; recorded matches will be kept." }
        }
    }
    fun headline(c: Competition, games: List<Game>, balls: List<Delivery>): String {
        val final=c.fixtures.lastOrNull { it.label=="Final" }
        if(final!=null) outcome(c,final.id,games,balls)?.let { return "${c.team(it)} won ${c.name}" }
        val rows=table(c,games,balls)
        if(c.format==Format.BILATERAL) {
            val a=rows.find { it.id==c.teams[0].id }!!; val b=rows.find { it.id==c.teams[1].id }!!
            val complete=c.closed || (!c.openEnded && c.fixtures.all { finished(c,it,games,balls) }) || (c.bestOf && maxOf(a.wins,b.wins)>c.matchCount/2)
            return "${c.teams[0].name} ${a.wins}–${b.wins} ${c.teams[1].name}" + if(complete) " · Series complete" else ""
        }
        return if((c.format==Format.LEAGUE || c.format==Format.TRI && !c.finalsEnabled) && c.fixtures.all { finished(c,it,games,balls) }) "${c.team(rows.first().id)} won the league" else "${c.fixtures.count { finished(c,it,games,balls) }}/${c.fixtures.size} fixtures complete"
    }
}
