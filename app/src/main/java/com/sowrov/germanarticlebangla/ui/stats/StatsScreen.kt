package com.sowrov.germanarticlebangla.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sowrov.germanarticlebangla.data.DAILY_GOAL
import com.sowrov.germanarticlebangla.data.WeekStats
import com.sowrov.germanarticlebangla.data.germanScore
import com.sowrov.germanarticlebangla.data.weekKey
import com.sowrov.germanarticlebangla.data.getActiveStreak
import com.sowrov.germanarticlebangla.data.getTodayUsage
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.AppViewModel
import com.sowrov.germanarticlebangla.ui.IconTile
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
fun StatsScreen(vm: AppViewModel) {
    val c = WortTheme.colors
    val progress by vm.progress.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    val stats = progress.stats
    val thisWeek = progress.weeks[weekKey()] ?: WeekStats()
    val lastWeek = progress.weeks[weekKey(1)]
    val weekBefore = progress.weeks[weekKey(2)]
    val score = germanScore(thisWeek)
    val lastScore = if (lastWeek != null) germanScore(lastWeek) else null
    val delta = if (lastWeek != null && weekBefore != null) germanScore(lastWeek) - germanScore(weekBefore) else null
    val rate = if (thisWeek.cards > 0) Math.round(thisWeek.correct * 100f / thisWeek.cards) else 0
    val streak = getActiveStreak(stats)
    val today = getTodayUsage(progress).cardsPracticed
    val goalFraction = (today.toFloat() / DAILY_GOAL).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text("Statistics", color = c.foreground, fontFamily = SerifFont, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Your progress on this device.", color = c.mutedForeground, fontSize = 14.sp)
        }

        // German Score: this week's score out of 100, with last week's report underneath.
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(AppIcons.Trophy, c.primary, size = 48, round = true)
                Column(Modifier.weight(1f)) {
                    Text("German Score", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$score", color = c.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text(" / 100", color = c.mutedForeground, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Last week report", color = c.mutedForeground, fontSize = 11.sp)
                if (lastScore != null) {
                    Text("$lastScore", color = c.foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (delta != null) {
                        val arrow = when {
                            delta > 0 -> "↑ $delta"
                            delta < 0 -> "↓ ${-delta}"
                            else -> "– 0"
                        }
                        val tint = when {
                            delta > 0 -> c.success
                            delta < 0 -> c.destructive
                            else -> c.mutedForeground
                        }
                        Text(arrow, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("—", color = c.mutedForeground, fontSize = 12.sp)
                }
            }
        }

        Text("This week", color = c.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(AppIcons.Layers, "Cards played", thisWeek.cards.toString(), Modifier.weight(1f))
            StatTile(AppIcons.Target, "Success rate", "$rate%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(AppIcons.Check, "Current run", stats.currentRun.toString(), Modifier.weight(1f))
            StatTile(AppIcons.Trophy, "Best run", thisWeek.bestRun.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(AppIcons.Clock, "Time practiced", formatTime(thisWeek.timeMs), Modifier.weight(1f))
            StatTile(AppIcons.Favorite, "Favorites", progress.favorites.size.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(AppIcons.Mic, "Perfect Pronunciation", thisWeek.pronPerfect.toString(), Modifier.weight(1f))
            StatTile(AppIcons.Flame, "Day streak", streak.toString(), Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, c.border),
        ) { Text("Reset statistics", color = c.foreground) }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.card,
            title = { Text("Reset statistics?", color = c.foreground) },
            text = { Text("Your cards played, runs and streak will go back to zero. Favorites are kept.", color = c.mutedForeground) },
            confirmButton = {
                TextButton(onClick = { vm.resetStats(); confirmReset = false }) { Text("Reset", color = c.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = c.foreground) }
            },
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val c = WortTheme.colors
    Row(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(c.card).padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, c.primary, size = 44)
        Column {
            Text(label, color = c.mutedForeground, fontSize = 13.sp, maxLines = 2)
            Text(
                value, color = c.foreground, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                fontSize = if (value.length > 5) 20.sp else 28.sp,
            )
        }
    }
}
