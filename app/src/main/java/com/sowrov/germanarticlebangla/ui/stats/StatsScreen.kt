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
    val rate = if (stats.cardsPlayed > 0) Math.round(stats.correct * 100f / stats.cardsPlayed) else 0
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

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(AppIcons.Flame, c.primary, size = 48, round = true)
                Column(Modifier.weight(1f)) {
                    Text("$streak day streak", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Answer $DAILY_GOAL cards a day to keep it going.", color = c.mutedForeground, fontSize = 12.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.secondary)) {
                Box(Modifier.fillMaxWidth(goalFraction).height(8.dp).clip(CircleShape).background(c.primary))
            }
            Text(
                "Today: ${minOf(today, DAILY_GOAL)} / $DAILY_GOAL cards",
                color = c.mutedForeground, fontSize = 12.sp,
            )
        }

        StatRow(AppIcons.Layers, "Cards played", stats.cardsPlayed.toString())
        StatRow(AppIcons.Target, "Success rate", "$rate%")
        StatRow(AppIcons.Flame, "Current run", stats.currentRun.toString())
        StatRow(AppIcons.Trophy, "Best run", stats.bestRun.toString())
        StatRow(AppIcons.Clock, "Time practiced", formatTime(stats.timeSpentMs))
        StatRow(AppIcons.Favorite, "Favorites", progress.favorites.size.toString())

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
private fun StatRow(icon: ImageVector, label: String, value: String) {
    val c = WortTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, c.primary, size = 40)
        Text(label, color = c.mutedForeground, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
