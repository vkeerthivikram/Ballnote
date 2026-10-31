@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ballnote.app.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballnote.app.AppModel
import com.ballnote.app.data.*
import com.ballnote.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Light = lightColorScheme(primary = Color(0xFF4C609B), onPrimary = Color.White, primaryContainer = Color(0xFFDCE2FF), onPrimaryContainer = Color(0xFF10244F), secondaryContainer = Color(0xFFE1E3F3), background = Color(0xFFFAF8FF), surface = Color(0xFFFAF8FF))
private val Dark = darkColorScheme(primary = Color(0xFFB4C5FF), onPrimary = Color(0xFF192E60), primaryContainer = Color(0xFF304576), onPrimaryContainer = Color(0xFFDCE2FF), secondaryContainer = Color(0xFF2B2D3B), background = Color(0xFF11131B), surface = Color(0xFF11131B))
fun extrasLabel(mode: ExtrasMode) = when (mode) { ExtrasMode.REBOWL -> "Bowl again"; ExtrasMode.COUNT -> "Count the ball"; ExtrasMode.FINAL_OVER_REBOWL -> "Bowl again in final over" }
internal fun date(time: Long) = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(time))

@Composable fun Ballnote(vm: AppModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val dark = prefs.theme == "Dark" || (prefs.theme == "System" && isSystemInDarkTheme())
    val context = LocalContext.current
    val colors = if (prefs.dynamic && Build.VERSION.SDK_INT >= 31) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) } else if (dark) Dark else Light
    val view = LocalView.current
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors) { Surface(Modifier.fillMaxSize()) { App(vm, prefs) } }
}

@Composable private fun App(vm: AppModel, prefs: Preferences) {
    val library by vm.library.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val overCue by vm.overCue.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf("Matches") }
    var gameId by rememberSaveable { mutableStateOf<String?>(null) }
    var rematchId by rememberSaveable { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Delivery?>(null) }
    var correction by remember { mutableStateOf<Correction?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<Backup?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var fileMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val l = library
    val g = l?.games?.find { it.id == gameId }
    val ds = if (g != null) l.deliveries(g.id) else emptyList()
    val s = if (g != null) l.score(g) else null
    LaunchedEffect(selected) { selected?.let { gameId = it; route = "Live" } }
    LaunchedEffect(route) {
        if (route !in listOf("Matches", "Competitions", "Settings", "New match", "Live", "Match details")) route = "Matches"
        if (route != "Matches") { choosing = false; chosen = emptyList() }
    }
    LaunchedEffect(l?.games) {
        chosen = chosen.filter { id -> l?.games?.any { it.id == id } == true }
    }
    LaunchedEffect(deleted?.id) {
        deleted?.let { event ->
            val count = event.backup.matches.size
            val response = snackbar.showSnackbar(if (count == 1) "Match deleted" else "$count matches deleted", "Undo", withDismissAction = true, duration = SnackbarDuration.Long)
            if (response == SnackbarResult.ActionPerformed) vm.undoDeletion(event.id) else vm.dismissDeletion(event.id)
        }
    }
    LaunchedEffect(overCue) { overCue?.let { if(prefs.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress); snackbar.showSnackbar(it); vm.overCue.value=null } }
    LaunchedEffect(error) { if (error != null && snackbar.showSnackbar(error!!, "Retry", duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) vm.retry() }
    LaunchedEffect(fileMessage) { fileMessage?.let { snackbar.showSnackbar(it); fileMessage = null } }
    val view = LocalView.current
    DisposableEffect(route, prefs.awake) { view.keepScreenOn = route == "Live" && prefs.awake; onDispose { view.keepScreenOn = false } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null && l != null) scope.launch {
        runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(json.encodeToString(Backup(teams = l.teams, matches = l.games, deliveries = l.balls, competitions = l.competitions)).toByteArray()) } ?: error("Could not open backup destination.") } }.onSuccess { fileMessage = "Backup saved" }.onFailure { fileMessage = "Backup failed: ${it.message}" }
    } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
        runCatching { withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)?.use { input -> val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var count = input.read(buffer); while (count >= 0) { require(output.size() + count <= 25 * 1024 * 1024) { "Backup is larger than 25 MB." }; output.write(buffer, 0, count); count = input.read(buffer) }; output.toString("UTF-8") } ?: error("Could not read this file.")
            json.decodeFromString<Backup>(text).also { it.validate() }
        } }.onSuccess { pendingBackup = it }.onFailure { fileMessage = "Cannot restore: ${it.message}" }
    } }
    fun back() { route = "Matches"; vm.selected.value = null }
    BackHandler(route !in listOf("Matches", "Competitions", "Settings")) { back() }
    BackHandler(route == "Matches" && choosing) { choosing = false; chosen = emptyList() }
    val root = route in listOf("Matches", "Competitions", "Settings")
    val pageScroll = remember(route, gameId) { ScrollState(0) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(if (route == "Live") "Ballnote" else if (route == "Matches" && choosing) "${chosen.size} selected" else route, fontWeight = FontWeight.Bold) }, navigationIcon = { if (!root) IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to matches") } }, actions = {
            if (route == "Live" && g != null) IconButton(onClick = { route = "Match details" }) { Icon(Icons.AutoMirrored.Outlined.List, "Match details and corrections") }
            if (route == "Matches" && l?.games?.isNotEmpty() == true) {
                TextButton(onClick = { choosing = !choosing; chosen = emptyList() }, enabled = !busy) { Text(if (choosing) "Cancel" else "Select") }
                if (choosing) IconButton(onClick = { vm.deleteMatches(chosen.toSet()); choosing = false; chosen = emptyList() }, enabled = chosen.isNotEmpty() && !busy) { Icon(Icons.Outlined.Delete, "Delete selected matches") }
            }
        }) },
        bottomBar = { if (root) NavigationBar { listOf("Matches", "Competitions", "Settings").forEach { label -> NavigationBarItem(selected = route == label, onClick = { route = label }, icon = { Icon(if (label == "Settings") Icons.Outlined.Settings else Icons.Outlined.DateRange, label) }, label = { Text(label) }) } } }
    ) { padding ->
        if (l == null) { Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { if (error == null) CircularProgressIndicator() else Text("Could not open saved data. Use Retry below.") }; return@Scaffold }
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(pageScroll).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(if (route == "Live") 8.dp else 16.dp)) {
            when(route) {
                "Competitions" -> CompetitionScreen(l, vm, busy)
                "Matches" -> {
                    if (choosing) TextButton(onClick = { chosen = if (chosen.size == l.games.size) emptyList() else l.games.map { it.id } }) { Text(if (chosen.size == l.games.size) "Deselect all" else "Select all") }
                    else Button(onClick = { rematchId = null; route = "New match" }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("New match") }
                    if (l.games.isEmpty()) Empty("Your next game starts here.", "Choose two teams, set your turf rules, and tap each ball. Everything stays on this phone.")
                    l.games.sortedByDescending { it.created }.forEach { game -> key(game.id) {
                        MatchCard(game, l.score(game), selected = if (choosing) game.id in chosen else null,
                            open = { if (choosing) chosen = if (game.id in chosen) chosen - game.id else chosen + game.id
                                else { gameId = game.id; if (l.score(game).finished) route = "Match details" else vm.open(game) } },
                            delete = { vm.deleteMatches(setOf(game.id)) })
                    } }
                }
                "New match" -> Setup(rematchId?.let { id -> l.games.find { it.id == id } }, prefs.rules, busy) { a, b, rules -> vm.create(a, b, rules) }
                "Live" -> if (g != null && s != null) {
                    LiveScore(g, s, busy, error != null, record = { kind, runs, wicket -> if (prefs.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); vm.record(g.id, kind, runs, wicket) }, more = { editor = Delivery(matchId = g.id, innings = if (g.chaseStarted) 2 else 1, sequence = -1) }, undo = { vm.undo(g.id) }, chase = { vm.chase(g.id) }, details = { route = "Match details" }, undoEnabled = ds.isNotEmpty() || g.firstClosed || g.chaseStarted)
                }
                "Match details" -> if (g != null && s != null) {
                    Text("${g.nameA} vs ${g.nameB}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${date(g.created)} · ${g.rules.overs} overs · ${g.rules.wickets} wickets")
                    Text("Wide +${g.rules.widePenalty} · No-ball +${g.rules.noBallPenalty} · ${extrasLabel(g.rules.extras)}", style = MaterialTheme.typography.bodyMedium)
                    Text(s.result, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!s.finished) Button(onClick = { vm.open(g) }) { Text("Resume") }
                        OutlinedButton(onClick = { rematchId = g.id; route = "New match" }) { Text("Rematch") }
                        if (s.finished) OutlinedButton(onClick = { scope.launch { runCatching { withContext(Dispatchers.IO) { resultFile(context, g, s) } }.onSuccess { uri -> val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, summary(g, s)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(intent, "Share result")) }.onFailure { fileMessage = "Could not share: ${it.message}" } } }) { Text("Share") }
                    }
                    listOf(1 to s.first, 2 to s.second).filter { it.first == 1 || g.chaseStarted }.forEach { (number, inning) ->
                        HorizontalDivider(); Text("${if (number == 1) g.nameA else g.nameB}  ${inning.runs}/${inning.wickets}", style = MaterialTheme.typography.titleLarge)
                        Text("${inning.overs} overs${if (inning.early) " · ended early" else ""}")
                        Text(inning.recap)
                        if (inning.timeline.isNotEmpty()) Text("Tap a delivery to edit it.", style = MaterialTheme.typography.bodySmall)
                        inning.oversBreakdown.forEach { over -> OverHistory(over) { if (!busy) editor = it } }
                        if (inning.timeline.isEmpty()) Text("No deliveries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!s.finished) {
                        if (!g.chaseStarted && s.status == Status.FIRST) OutlinedButton(onClick = { confirm = "End first innings" }) { Text("End first innings early") }
                        TextButton(onClick = { confirm = "Abandon match" }) { Text("Abandon match", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { vm.deleteMatches(setOf(g.id)); back() }, enabled = !busy) { Text("Delete match", color = MaterialTheme.colorScheme.error) }
                }
                "Settings" -> {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("System", "Light", "Dark").forEach { choice -> FilterChip(selected = prefs.theme == choice, onClick = { vm.preferences(prefs.copy(theme = choice)) }, label = { Text(choice) }) } }
                    if (Build.VERSION.SDK_INT >= 31) Toggle("Use device colours", "Match your wallpaper palette", prefs.dynamic) { vm.preferences(prefs.copy(dynamic = it)) }
                    Toggle("Haptic feedback", "A brief response when you score", prefs.haptics) { vm.preferences(prefs.copy(haptics = it)) }
                    Toggle("Keep screen on", "While the scoring screen is open", prefs.awake) { vm.preferences(prefs.copy(awake = it)) }
                    HorizontalDivider(); Text("Your data", style = MaterialTheme.typography.titleLarge)
                    Text("Saved on this phone. Export a backup before changing phones or uninstalling Ballnote.")
                    OutlinedButton(onClick = { export.launch("ballnote-backup-${System.currentTimeMillis()}.json") }, enabled = !busy) { Text("Export backup") }
                    OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !busy) { Text("Restore backup") }
                    TextButton(onClick = { confirm = "Delete all matches" }, enabled = l.games.isNotEmpty() && !busy) { Text("Delete all matches", color = MaterialTheme.colorScheme.error) }
                    val versionName = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
                    Text("Ballnote $versionName · Tap. Score. Play.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    editor?.let { d -> DeliverySheet(d, g?.rules ?: prefs.rules, busy, dismiss = { editor = null }, save = { updated ->
        if (d.sequence < 0) vm.record(d.matchId, updated.kind, updated.runs, updated.wicket)
        else if (g != null) correction = Scoring.preview(g, ds, d.id, updated)
        editor = null
    }, delete = if (d.sequence < 0) null else ({ if (g != null) correction = Scoring.preview(g, ds, d.id, null); editor = null })) }
    correction?.let { c -> AlertDialog(onDismissRequest = { correction = null }, title = { Text("Confirm correction") }, text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("First innings: ${c.before.first.runs}/${c.before.first.wickets} → ${c.after.first.runs}/${c.after.first.wickets}\nChase: ${c.before.second.runs}/${c.before.second.wickets} → ${c.after.second.runs}/${c.after.second.wickets}\nTarget: ${c.before.target} → ${c.after.target}")
        Text(c.after.result)
        if (c.after.first.early && c.game.chaseStarted) Text("The first innings stays closed at its existing boundary and will be marked as ended early.")
        if (c.after.excluded.isNotEmpty()) Text("${c.after.excluded.size} later deliveries will be removed: ${c.after.excluded.joinToString { it.label }}", color = MaterialTheme.colorScheme.error)
        if (c.before.finished && !c.after.finished) Text("This match will reopen.")
    } }, confirmButton = { TextButton(onClick = { vm.correction(c); correction = null }, enabled = !busy) { Text("Apply correction") } }, dismissButton = { TextButton(onClick = { correction = null }) { Text("Cancel") } }) }
    confirm?.let { title -> AlertDialog(onDismissRequest = { confirm = null }, title = { Text(title + "?") }, text = { Text(when (title) {
        "Delete all matches" -> "Delete all ${l?.games?.size ?: 0} matches and their deliveries, including unfinished matches? Export a backup first if you want a permanent copy."
        "Abandon match" -> "Keep all recorded deliveries and mark this match as No result."
        else -> "Close the first innings at the current score and set the chase target."
    }) }, confirmButton = { TextButton(onClick = {
        if (title == "Delete all matches") { vm.deleteMatches(l?.games?.map { it.id }?.toSet() ?: emptySet()); back() }
        else g?.let { if (title == "Abandon match") vm.abandon(it.id) else vm.endFirst(it.id) }
        confirm = null
    }, enabled = !busy) { Text(if (title == "Delete all matches") "Delete all" else "Confirm") } }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }) }
    pendingBackup?.let { b -> AlertDialog(onDismissRequest = { pendingBackup = null }, title = { Text("Replace saved data?") }, text = { Text("Restore ${b.matches.size} matches and ${b.deliveries.size} deliveries. This replaces all current matches. Export a backup first if you want to keep them.") }, confirmButton = { TextButton(onClick = { vm.restore(b); pendingBackup = null; route = "Matches" }, enabled = !busy) { Text("Replace and restore") } }, dismissButton = { TextButton(onClick = { pendingBackup = null }) { Text("Cancel") } }) }
}

@Composable private fun Empty(title: String, body: String) { Column(Modifier.padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.headlineMedium); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun Toggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall) }; Switch(checked = checked, onCheckedChange = change, modifier = Modifier.semantics { contentDescription = title }) } }

@Composable private fun Setup(previous: Game?, defaults: MatchRules, busy: Boolean, create: (String, String, MatchRules) -> Unit) {
    var a by rememberSaveable { mutableStateOf(previous?.nameA ?: "") }
    var b by rememberSaveable { mutableStateOf(previous?.nameB ?: "") }
    var overs by rememberSaveable { mutableStateOf((previous?.rules ?: defaults).overs.toString()) }; var wickets by rememberSaveable { mutableStateOf((previous?.rules ?: defaults).wickets.toString()) }
    var penalties by remember { mutableStateOf(previous?.rules ?: defaults) }
    var mode by rememberSaveable { mutableStateOf((previous?.rules ?: defaults).extras.name) }
    Text("Set your rules. Then just play.", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(a, { if (it.length <= 40) a = it }, label = { Text("Batting first") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b, { if (it.length <= 40) b = it }, label = { Text("Batting second") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = { val old = a; a = b; b = old }) { Text("Swap batting order") }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(overs, { if (it.length <= 2) overs = it.filter(Char::isDigit) }, label = { Text("Overs (1–50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)); OutlinedTextField(wickets, { if (it.length <= 2) wickets = it.filter(Char::isDigit) }, label = { Text("Wickets (1–20)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)) }
    Text("Wides and no-balls", style = MaterialTheme.typography.titleMedium)
    ExtrasMode.entries.forEach { m -> Row(Modifier.fillMaxWidth().clickable { mode = m.name }, verticalAlignment = Alignment.CenterVertically) { RadioButton(mode == m.name, { mode = m.name }); Text(extrasLabel(m)) } }
    Penalties(penalties) { penalties=it }
    val valid = a.isNotBlank() && b.isNotBlank() && !a.trim().equals(b.trim(), true) && (overs.toIntOrNull() ?: 0) in 1..50 && (wickets.toIntOrNull() ?: 0) in 1..20
    Button(enabled = valid && !busy, onClick = { create(a.trim(), b.trim(), MatchRules(overs.toInt(), wickets.toInt(), ExtrasMode.valueOf(mode), penalties.widePenalty, penalties.noBallPenalty)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Start match") }
}

@Composable private fun LiveScore(g: Game, s: Score, busy: Boolean, failed: Boolean, record: (Kind, Int, Boolean) -> Unit, more: () -> Unit, undo: () -> Unit, chase: () -> Unit, details: () -> Unit, undoEnabled: Boolean) {
    val current = if (g.chaseStarted) s.second else s.first
    var large by rememberSaveable { mutableStateOf(false) }
    if(large) LargeScoreboard(g,s) { large=false }
    val playing = s.status == Status.FIRST || s.status == Status.CHASE
    Surface(onClick={large=true}, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (g.chaseStarted) g.nameB else g.nameA, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (g.chaseStarted) "2nd innings" else "1st innings", style = MaterialTheme.typography.bodySmall)
            if (LocalDensity.current.fontScale > 1.2f || current.runs >= 1000) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("${current.runs}", fontSize = (56f * minOf(1f, 3f / current.runs.toString().length)).sp, fontWeight = FontWeight.Bold); Text("/${current.wickets}", fontSize = 36.sp) }
                Text("${current.overs} of ${g.rules.overs} overs", style = MaterialTheme.typography.titleMedium)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("${current.runs}", fontSize = 72.sp, fontWeight = FontWeight.Bold); Text("/${current.wickets}", fontSize = 36.sp); Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Text(current.overs, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("of ${g.rules.overs} overs", style = MaterialTheme.typography.bodySmall) } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .2f))
            Text(if (g.chaseStarted || s.status == Status.INTERVAL || s.finished) s.result else "${g.rules.overs * 6 - current.balls} balls left · ${g.rules.wickets - current.wickets} wickets in hand", style = MaterialTheme.typography.titleMedium)
            if (g.chaseStarted) Text("Target ${s.target}", style = MaterialTheme.typography.bodySmall)
        }
    }
    val lastOver = current.timeline.lastOrNull()?.over ?: 0
    val balls = current.timeline.filter { it.over == lastOver }
    val over = current.oversBreakdown.lastOrNull()
    Text(if (over == null) "This over · 0 runs" else overLabel(over), style = MaterialTheme.typography.labelLarge)
    val scroll = rememberScrollState()
    LaunchedEffect(lastOver, balls.size) { scroll.scrollTo(scroll.maxValue) }
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        balls.forEach { b -> key(b.delivery.id) { DeliveryChip(b) } }
        repeat((6 - balls.count { it.counted }).coerceAtLeast(0)) { Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Spacer(Modifier.size(40.dp)) } }
    }
    if (playing) {
        listOf(listOf(0, 1, 2), listOf(3, 4, 6)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { n ->
                    Button(onClick = { record(Kind.NORMAL, n, false) }, enabled = !busy, shape = RoundedCornerShape(18.dp), colors = if (n >= 4) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(), modifier = Modifier.weight(1f).heightIn(min = 80.dp).semantics { contentDescription = if (n == 0) "Dot ball" else "$n runs" }) { Text("$n", fontSize = 32.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { record(Kind.NORMAL, 0, true) }, enabled = !busy, contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Wicket", color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { record(Kind.WIDE, 0, false) }, enabled = !busy, contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Wide +${g.rules.widePenalty}") }
            OutlinedButton(onClick = { record(Kind.NO_BALL, 0, false) }, enabled = !busy, contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("No-ball +${g.rules.noBallPenalty}") }
        }
    }
    if (s.status == Status.INTERVAL) Button(onClick = chase, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Start chase · target ${s.target}") }
    if (s.finished) Button(onClick = details, modifier = Modifier.fillMaxWidth()) { Text("Match result and sharing") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (playing) OutlinedButton(onClick = more, enabled = !busy) { Text("More") }
        TextButton(onClick = undo, enabled = undoEnabled && !busy && !s.finished) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Text(" Undo last ball") }
        Spacer(Modifier.weight(1f)); Text(if (busy) "Saving…" else if (failed) "Not saved" else "Saved", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun DeliverySheet(d: Delivery, rules: MatchRules, busy: Boolean, dismiss: () -> Unit, save: (Delivery) -> Unit, delete: (() -> Unit)?) {
    var kind by remember(d.id) { mutableStateOf(d.kind) }; var runs by remember(d.id) { mutableStateOf(d.runs.toString()) }; var wicket by remember(d.id) { mutableStateOf(d.wicket) }
    ModalBottomSheet(onDismissRequest = dismiss) { Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (delete == null) "More ways to score" else "Edit delivery", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Kind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(when(k) { Kind.NORMAL -> "Normal"; Kind.WIDE -> "Wide"; Kind.NO_BALL -> "No-ball" }) }) } }
        OutlinedTextField(runs, { if (it.length <= 2) runs = it.filter(Char::isDigit) }, label = { Text(if (kind == Kind.NORMAL) "Runs (0–99)" else "Additional runs (0–99)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Toggle("Wicket", "Record the outcome agreed on the turf", wicket) { wicket = it }
        Text("${(runs.toIntOrNull() ?: 0) + when(kind) { Kind.NORMAL -> 0; Kind.WIDE -> rules.widePenalty; Kind.NO_BALL -> rules.noBallPenalty }} runs${if (wicket) " + wicket" else ""} · one recorded delivery", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { save(d.copy(kind = kind, runs = runs.toInt(), wicket = wicket)) }, enabled = !busy && runs.toIntOrNull() in 0..99, modifier = Modifier.fillMaxWidth()) { Text(if (delete == null) "Record delivery" else "Preview correction") }
        delete?.let { TextButton(onClick = it, enabled = !busy) { Text("Delete delivery…", color = MaterialTheme.colorScheme.error) } }
        Spacer(Modifier.height(16.dp))
    } }
}

private fun summary(g: Game, s: Score) = "${g.nameA} ${s.first.runs}/${s.first.wickets} (${s.first.overs})\n${g.nameB} ${s.second.runs}/${s.second.wickets} (${s.second.overs})\n${s.result}\n${date(g.created)} · Ballnote"
