package com.ballnote.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ballnote.app.domain.*

internal fun statusLabel(score: Score): String = when (score.status) {
    Status.FIRST, Status.CHASE -> "In progress"
    Status.INTERVAL -> "Innings break"
    Status.COMPLETE -> if (score.winner == null) "Tied" else "Completed"
    Status.ABANDONED -> "No result"
}

private data class MatchColors(val container: Color, val content: Color, val accent: Color)

@Composable private fun matchColors(score: Score): MatchColors {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    return when {
        !score.finished -> if (dark) MatchColors(Color(0xFF463500), Color(0xFFFFDF9A), Color(0xFFFFCD66))
            else MatchColors(Color(0xFFFFF1D6), Color(0xFF573B00), Color(0xFF765400))
        score.status == Status.ABANDONED -> MatchColors(colors.surfaceVariant, colors.onSurfaceVariant, colors.onSurfaceVariant)
        score.winner == null -> MatchColors(colors.tertiaryContainer, colors.onTertiaryContainer, colors.tertiary)
        else -> MatchColors(colors.primaryContainer, colors.onPrimaryContainer, colors.primary)
    }
}

@Composable internal fun MatchCard(g: Game, s: Score, selected: Boolean?, open: () -> Unit, delete: () -> Unit) {
    val colors = matchColors(s)
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = open, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.container, contentColor = colors.content),
        border = BorderStroke(if (selected == true) 2.dp else 1.dp, colors.accent.copy(alpha = if (selected == true) 1f else 0.25f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            if (selected != null) Checkbox(checked = selected, onCheckedChange = { open() },
                modifier = Modifier.semantics { contentDescription = "Select ${g.nameA} vs ${g.nameB}" })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(statusLabel(s), style = MaterialTheme.typography.labelLarge, color = colors.accent)
                Text("${g.nameA} vs ${g.nameB}", style = MaterialTheme.typography.titleLarge)
                Text("${date(g.created)} · ${g.rules.overs} overs", style = MaterialTheme.typography.bodySmall)
                Text("${s.first.runs}/${s.first.wickets} (${s.first.overs})" + if (g.chaseStarted) "  ·  ${s.second.runs}/${s.second.wickets} (${s.second.overs})" else "",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(s.result)
            }
            if (selected == null) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Match options for ${g.nameA} vs ${g.nameB}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete match") }, onClick = { menu = false; delete() })
                }
            }
        }
    }
}

@Composable internal fun DeliveryChip(ball: ScoredBall, edit: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val d = ball.delivery
    val (background, foreground) = when {
        d.wicket -> colors.errorContainer to colors.onErrorContainer
        d.runs == 6 -> if (dark) Color(0xFF7EDBCB) to Color(0xFF00382F) else Color(0xFF006B5F) to Color.White
        d.runs == 4 -> if (dark) Color(0xFFB4C5FF) to Color(0xFF10244F) else Color(0xFF3459A6) to Color.White
        d.kind != Kind.NORMAL -> colors.surface to colors.onSurface
        else -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    val type = when (d.kind) { Kind.NORMAL -> ""; Kind.WIDE -> "wide, "; Kind.NO_BALL -> "no-ball, " }
    val description = "${if (edit != null) "Edit delivery, " else ""}over ${ball.over + 1}, $type${d.total} run${if (d.total == 1) "" else "s"}${if (d.wicket) ", wicket" else ""}, ${if (ball.counted) "counts as a ball" else "bowl again"}"
    val modifier = Modifier.defaultMinSize(if (edit == null) 40.dp else 48.dp, if (edit == null) 40.dp else 48.dp)
        .semantics(mergeDescendants = true) { contentDescription = description }
    val border = if (d.kind != Kind.NORMAL) BorderStroke(1.dp, foreground.copy(alpha = 0.6f)) else null
    val content: @Composable () -> Unit = {
        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(d.label, fontWeight = FontWeight.SemiBold) }
    }
    if (edit == null) Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = background, contentColor = foreground, border = border, content = content)
    else Surface(onClick = edit, modifier = modifier, shape = RoundedCornerShape(24.dp), color = background, contentColor = foreground, border = border, content = content)
}

internal fun overLabel(over: OverScore): String = "Over ${over.number} · ${over.runs} runs${if (over.wickets > 0) " · ${over.wickets} wicket${if (over.wickets == 1) "" else "s"}" else ""}"

@Composable internal fun OverHistory(over: OverScore, edit: (Delivery) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(12.dp)) {
            Text(overLabel(over), Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.titleSmall)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            over.deliveries.forEach { ball -> key(ball.delivery.id) { DeliveryChip(ball) { edit(ball.delivery) } } }
        }
        Text("${over.balls}/6 counted balls${if (over.balls < 6) " · partial over" else ""}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
