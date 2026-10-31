package com.ballnote.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ballnote.app.AppModel
import com.ballnote.app.data.Library
import com.ballnote.app.domain.*

@Composable internal fun Penalties(rules: MatchRules, change: (MatchRules)->Unit) {
    listOf("Wide penalty" to rules.widePenalty,"No-ball penalty" to rules.noBallPenalty).forEachIndexed { index,(label,value) ->
        Text(label)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { (1..2).forEach { n -> FilterChip(selected=value==n,onClick={ change(if(index==0) rules.copy(widePenalty=n) else rules.copy(noBallPenalty=n)) },label={ Text("+$n") }) } }
    }
}
@Composable private fun NumberEntry(label: String, value: Int, change: (Int)->Unit) {
    OutlinedTextField(value=if(value==0) "" else value.toString(),onValueChange={ if(it.length<=2 && it.all(Char::isDigit)) change(it.toIntOrNull() ?: 0) },label={Text(label)},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),modifier=Modifier.fillMaxWidth())
}
@Composable internal fun CompetitionScreen(l: Library, vm: AppModel, busy: Boolean) {
    var id by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(creating || id!=null) {creating=false;id=null}
    val c=l.competitions.find { it.id==id }
    val preferences by vm.preferences.collectAsState()
    if(creating) { CompetitionSetup(busy,preferences.rules, cancel={creating=false},save={vm.createCompetition(it);creating=false}); return }
    if(c==null) {
        Button(onClick={creating=true},modifier=Modifier.fillMaxWidth()) { Text("New competition") }
        if(l.competitions.isEmpty()) Text("A quick series, a league, or a full tournament. All saved on this phone.")
        l.competitions.sortedByDescending { it.created }.forEach { item -> OutlinedCard(onClick={id=item.id},modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(item.name,style=MaterialTheme.typography.titleLarge); Text(item.format.title); Text(Competitions.headline(item,l.games,l.balls)) } } }
        return
    }
    CompetitionDetails(c,l,vm,busy,back={id=null})
}
@Composable private fun CompetitionSetup(busy: Boolean, defaults: MatchRules, cancel: ()->Unit, save:(Competition)->Unit) {
    var name by rememberSaveable { mutableStateOf("") }; var names by rememberSaveable { mutableStateOf("") }
    var format by rememberSaveable { mutableStateOf(Format.BILATERAL) }; var rules by remember { mutableStateOf(defaults) }
    var legs by rememberSaveable { mutableIntStateOf(1) }; var count by rememberSaveable { mutableIntStateOf(3) }
    var best by rememberSaveable { mutableStateOf(false) }; var session by rememberSaveable { mutableStateOf(false) }
    var groups by rememberSaveable { mutableIntStateOf(2) }; var qualifiers by rememberSaveable { mutableIntStateOf(2) }; var ipl by rememberSaveable { mutableStateOf(false) }
    var finalsEnabled by rememberSaveable {mutableStateOf(true)}
    var preview by remember { mutableStateOf<Competition?>(null) }; var problem by remember { mutableStateOf<String?>(null) }
    if(preview!=null) {
        val c=preview!!
        Text("Preview · ${c.name}",style=MaterialTheme.typography.headlineSmall)
        Text("${c.teams.size} teams · ${c.rules.overs} overs · ${c.rules.wickets} wickets")
        Text("Wide +${c.rules.widePenalty} · No-ball +${c.rules.noBallPenalty} · ${extrasLabel(c.rules.extras)}")
        c.fixtures.forEach { Text("${it.label}: ${c.team(it.a)} vs ${if(it.bye) "Bye" else c.team(it.b)}") }
        Text("League: win 2, tie/no result 1, loss 0. Rank by points, wins, NRR, then entry order. Early-closed innings and organiser decisions are excluded from NRR.")
        if(c.format in listOf(Format.TRI,Format.PLAYOFFS,Format.GROUPS)) Text("Qualification: ${if(c.iplPlayoffs) "top four with qualifiers and eliminator" else "top ${c.qualifiers}${if(c.format==Format.GROUPS) " per group" else ""}"}.")
        Button(onClick={save(c)},enabled=!busy) {Text("Create competition")}
        OutlinedButton(onClick={preview=Competitions.create(c.copy(teams=c.teams.shuffled()))},enabled=!busy) {Text("Shuffle teams and fixtures")}
        TextButton(onClick={preview=null}) {Text("Edit setup")}; return
    }
    Text("New competition",style=MaterialTheme.typography.headlineSmall)
    OutlinedTextField(name,{if(it.length<=80) name=it},label={Text("Competition name")},modifier=Modifier.fillMaxWidth())
    Format.entries.forEach { f -> FilterChip(selected=format==f,onClick={format=f},label={Text(f.title)}) }
    OutlinedTextField(names,{names=it},label={Text("Team names · one per line (2–16)")},minLines=3,modifier=Modifier.fillMaxWidth())
    if(format==Format.BILATERAL) {
        Row { Checkbox(session,{session=it}); Text("Open-ended session") }
        if(!session) {NumberEntry("Number of matches",count){count=it}; Row {Checkbox(best,{best=it}); Text("Best-of: stop when a side clinches the series")}}
    } else if(format!=Format.KNOCKOUT) {
        Row { (1..2).forEach { n -> FilterChip(legs==n,{legs=n},label={Text(if(n==1) "Single round" else "Double round")}) } }
    }
    if(format==Format.TRI) Row {Checkbox(finalsEnabled,{finalsEnabled=it});Text("Top two play a final")}
    if(format==Format.GROUPS) NumberEntry("Number of groups (2–4)",groups){groups=it}
    if(format in listOf(Format.TRI,Format.PLAYOFFS,Format.GROUPS)) {
        Text(if(format==Format.GROUPS) "Qualify from each group" else "Qualify for finals")
        Row { listOf(2,4).forEach { n -> FilterChip(qualifiers==n,{qualifiers=n},label={Text("Top $n")}) } }
        if(format==Format.PLAYOFFS) Row {Checkbox(ipl,{ipl=it; if(it) qualifiers=4}); Text("Qualifier / eliminator playoffs")}
    }
    NumberEntry("Overs (1–50)",rules.overs){rules=rules.copy(overs=it)}
    NumberEntry("Wicket limit (1–20)",rules.wickets){rules=rules.copy(wickets=it)}
    ExtrasMode.entries.forEach { m -> FilterChip(rules.extras==m,{rules=rules.copy(extras=m)},label={Text(extrasLabel(m))}) }
    Penalties(rules){rules=it}
    problem?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    Button(onClick={runCatching {
        val c=Competition(name=name.trim(),format=format,teams=names.lines().map {it.trim()}.filter {it.isNotEmpty()}.map {Entrant(name=it)},rules=rules,legs=legs,matchCount=count,bestOf=best && !session && format==Format.BILATERAL,openEnded=session && format==Format.BILATERAL,groups=groups,qualifiers=qualifiers,iplPlayoffs=ipl && format==Format.PLAYOFFS,finalsEnabled=finalsEnabled || format!=Format.TRI)
        require(format!=Format.TRI || qualifiers==2){"Tri-series qualifies the top two."}
        require(format!=Format.GROUPS || c.teams.size/groups>=qualifiers){"Each group needs at least $qualifiers teams."}
        require(format!=Format.PLAYOFFS || c.teams.size>=qualifiers){"Need at least $qualifiers teams."}
        Competitions.create(c)
    }.onSuccess {preview=it;problem=null}.onFailure {problem=it.message}},enabled=!busy) {Text("Preview fixtures")}
    TextButton(onClick=cancel){Text("Cancel")}
}
@Composable private fun CompetitionDetails(c: Competition,l: Library,vm: AppModel,busy:Boolean,back:()->Unit) {
    val context=LocalContext.current
    var start by remember {mutableStateOf<Fixture?>(null)}; var decision by remember {mutableStateOf<Fixture?>(null)}
    var shareError by remember {mutableStateOf<String?>(null)}
    var reason by remember {mutableStateOf("")}; var action by remember {mutableStateOf<String?>(null)}
    TextButton(onClick=back){Text("All competitions")}
    Text(c.name,style=MaterialTheme.typography.headlineMedium)
    Text(c.format.title); Text(Competitions.headline(c,l.games,l.balls),style=MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick={
        val text=competitionText(c,l)
        runCatching { val uri=competitionImage(context,text); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="image/png";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_TEXT,text);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share competition")) }.onFailure {shareError="Could not share: ${it.message}"}
    }){Text("Share table and fixtures")}
    shareError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    if(c.format!=Format.KNOCKOUT) {
        val pools=if(c.format==Format.GROUPS) (0 until c.groups).toList() else listOf<Int?>(null)
        pools.forEach { group ->
            Text(if(group==null) "Standings" else "Group ${group+1}",style=MaterialTheme.typography.titleMedium)
            Text("P · W · L · T · NR · Pts · NRR",style=MaterialTheme.typography.labelMedium)
            Competitions.table(c,l.games,l.balls,group).forEach { r -> Text("${c.team(r.id)}${if(r.id in c.withdrawn) " (withdrawn)" else ""}\n${r.played} · ${r.wins} · ${r.losses} · ${r.ties} · ${r.noResults} · ${r.points} · ${"%.3f".format(java.util.Locale.ENGLISH,r.nrr)}") }
        }
        Text("Rank: points, wins, NRR, entry order. Decisions and early-closed innings excluded from NRR.",style=MaterialTheme.typography.bodySmall)
    }
    c.fixtures.forEach { f ->
        val (a,b)=Competitions.sides(c,f,l.games,l.balls); val game=l.games.find {it.id==f.matchId}; val score=game?.let {l.score(it)}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(f.label,style=MaterialTheme.typography.labelLarge)
            Text("${c.team(a)} vs ${if(f.bye) "Bye" else c.team(b)}",style=MaterialTheme.typography.titleMedium)
            Text(if(f.decision!=null) "${c.team(f.decision)} advanced / awarded · ${f.reason}" else if(f.bye) "${c.team(a)} advances with a bye" else score?.result ?: "Upcoming")
            if(game!=null) TextButton(onClick={vm.open(game)},enabled=!busy){Text(if(score!!.finished) "View match" else "Resume scoring")}
            else if(a!=null && b!=null && f.decision==null) Button(onClick={start=f},enabled=!busy && a !in c.withdrawn && b !in c.withdrawn){Text("Start match")}
            if(!f.bye && a!=null && b!=null && (game==null || score?.finished==true)) {
                TextButton(onClick={decision=f;reason=""},enabled=!busy){Text("Record organiser decision")}
                if(game!=null || f.decision!=null) TextButton(onClick={action="Replay:${f.id}"},enabled=!busy){Text("Replay fixture")}
            }
        } }
    }
    if(c.format==Format.BILATERAL && c.openEnded && !c.closed) Button(onClick={vm.nextSeries(c.id)},enabled=!busy && c.fixtures.all {Competitions.finished(c,it,l.games,l.balls)}){Text("Next match")}
    if(c.format==Format.BILATERAL && c.openEnded && !c.closed) OutlinedButton(onClick={action="Close"},enabled=!busy){Text("End session")}
    if(c.finalsEnabled && c.format in listOf(Format.TRI,Format.PLAYOFFS,Format.GROUPS) && c.fixtures.none {it.elimination}) Button(onClick={vm.advanceCompetition(c.id)},enabled=!busy && c.fixtures.all {Competitions.finished(c,it,l.games,l.balls)}){Text("Generate finals")}
    if(c.fixtures.any {it.elimination}) OutlinedButton(onClick={action="Reset"},enabled=!busy){Text("Reset knockout rounds")}
    Text("Team eligibility",style=MaterialTheme.typography.titleMedium)
    c.teams.forEach { team -> TextButton(onClick={action="Withdraw:${team.id}"},enabled=!busy){Text("${team.name} · ${if(team.id in c.withdrawn) "Reinstate" else "Withdraw"}")} }
    TextButton(onClick={action="Delete"},enabled=!busy){Text("Delete competition",color=MaterialTheme.colorScheme.error)}
    start?.let { f -> val sides=Competitions.sides(c,f,l.games,l.balls)
        AlertDialog(onDismissRequest={start=null},title={Text("Who bats first?")},text={Text("${c.team(sides.first)} vs ${c.team(sides.second)}")},confirmButton={TextButton(onClick={vm.startFixture(c.id,f.id,false);start=null}){Text(c.team(sides.first))}},dismissButton={TextButton(onClick={vm.startFixture(c.id,f.id,true);start=null}){Text(c.team(sides.second))}})
    }
    decision?.let { f -> val sides=Competitions.sides(c,f,l.games,l.balls)
        AlertDialog(onDismissRequest={decision=null},title={Text("Record decision")},text={Column {Text("For tie-breaks, walkovers and withdrawals. This does not change the recorded match score.");OutlinedTextField(reason,{reason=it},label={Text("Reason")})}},confirmButton={TextButton(onClick={vm.decideFixture(c.id,f.id,sides.first!!,reason);decision=null},enabled=reason.isNotBlank()){Text(c.team(sides.first))}},dismissButton={TextButton(onClick={vm.decideFixture(c.id,f.id,sides.second!!,reason);decision=null},enabled=reason.isNotBlank()){Text(c.team(sides.second))}})
    }
    action?.let { value -> AlertDialog(onDismissRequest={action=null},title={Text("Confirm change")},text={Text(when {value=="Reset" -> "Reset knockout fixtures so earlier results can be corrected. Recorded matches stay in match history. Finals will need to be generated and played again."; value=="Close" -> "End this session at the current series score. Finish the current match first."; value=="Delete" -> "Delete this competition. Recorded matches stay in match history."; value.startsWith("Replay") -> "Detach the previous match and reopen this fixture. The previous match stays in history."; else -> "Change team eligibility. Resolve remaining fixtures using organiser decisions or reinstate the team before scoring."})},confirmButton={TextButton(onClick={when {value=="Reset"->vm.resetRounds(c.id);value=="Close"->vm.closeSession(c.id);value=="Delete"->{vm.deleteCompetition(c.id);back()};value.startsWith("Replay:")->vm.replayFixture(c.id,value.substringAfter(':'));else->vm.withdraw(c.id,value.substringAfter(':'))};action=null}){Text("Confirm")}},dismissButton={TextButton(onClick={action=null}){Text("Cancel")}}) }
}
internal fun competitionText(c: Competition,l: Library): String = buildString {
    appendLine(c.name);appendLine(c.format.title);appendLine(Competitions.headline(c,l.games,l.balls));appendLine()
    if(c.format!=Format.KNOCKOUT) {
        val pools=if(c.format==Format.GROUPS) (0 until c.groups).toList() else listOf<Int?>(null)
        pools.forEach { group -> appendLine(if(group==null) "Standings: P W L T NR Pts NRR" else "Group ${group+1}: P W L T NR Pts NRR");Competitions.table(c,l.games,l.balls,group).forEach {r->appendLine("${c.team(r.id)}: ${r.played} ${r.wins} ${r.losses} ${r.ties} ${r.noResults} ${r.points} ${"%.3f".format(java.util.Locale.ENGLISH,r.nrr)}")} }
    }
    c.fixtures.forEach {f->val (a,b)=Competitions.sides(c,f,l.games,l.balls);appendLine("${f.label}: ${c.team(a)} v ${if(f.bye) "Bye" else c.team(b)}");appendLine(f.decision?.let {"${c.team(it)}: ${f.reason}"} ?: l.games.find {it.id==f.matchId}?.let {l.score(it).result} ?: "Upcoming")}
    appendLine("Ballnote · Tap. Score. Play.")
}
