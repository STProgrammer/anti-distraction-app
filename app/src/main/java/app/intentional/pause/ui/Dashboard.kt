package app.intentional.pause.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.intentional.pause.core.UsageMath
import app.intentional.pause.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun Dashboard(days: Map<LocalDate, Long>, prefs: Preferences, live: LiveState, functioning: Boolean,
    installed: List<InstalledApp>, note: String?, onSetup: () -> Unit, onApps: () -> Unit, onHistory: () -> Unit, onDismissNote: () -> Unit) {
    val today = days[LocalDate.now()] ?: 0
    val week = days.entries.toList().takeLast(7).associate { it.key to it.value }
    val count = prefs.apps.count { a -> a.enabled && installed.any { it.pkg == a.pkg } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PageTitle(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM")), "Make room\nfor what matters.") }
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Pause, "Intent", Modifier.padding(14.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Surface(shape = RoundedCornerShape(28.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF173F37), Color(0xFF205D50))))) {
                Canvas(Modifier.matchParentSize()) {
                    repeat(3) { i -> drawCircle(Color(0xFF6DC5A0).copy(alpha = .10f), size.width * (.32f + i * .12f), Offset(size.width * .98f, size.height * .90f), style = Stroke(1.5.dp.toPx())) }
                }
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp), tint = Mint)
                        Spacer(Modifier.width(8.dp)); Text("TODAY'S SOCIAL TIME", color = Mint, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        if (live.counting) Text("● LIVE", color = Mint, style = MaterialTheme.typography.labelSmall)
                    }
                    BoxWithConstraints {
                        Text(UsageMath.clock(today), color = Color.White, fontSize = if (maxWidth < 310.dp) 37.sp else 43.sp,
                            fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, letterSpacing = (-1.5).sp)
                    }
                    Text("All your monitored apps, one mindful total.", color = Color(0xFFC1DCD0), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(onClick = onSetup, color = if (functioning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (functioning) Icons.Outlined.VerifiedUser else Icons.Outlined.Info, null, Modifier.size(23.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (functioning) "A little pause. A better choice." else if (!prefs.enabled) "Monitoring is paused" else "Finish setting up Intent", style = MaterialTheme.typography.titleSmall)
                    Text(if (functioning) "Monitoring is ready · $count apps" else live.error ?: "Check permissions and service status", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
        if (note != null) Panel {
            Text("An interruption was detected", style = MaterialTheme.typography.titleSmall)
            Text(note, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismissNote) { Text("Dismiss") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Daily average", UsageMath.short(week.values.sum() / 7), "Over the last 7 days", Modifier.weight(1f))
            MetricCard("Mindful apps", "$count", "Enabled on this phone", Modifier.weight(1f))
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Your week", style = MaterialTheme.typography.titleLarge); Text("Small choices add up.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onHistory) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "View history") }
            }
            UsageChart(week, compact = true)
        }
        Panel {
            SettingRow(Icons.Outlined.Apps, "Choose your distractions", "Make the pause work for your habits.", onApps)
        }
        Text("No limits. No judgement. Just intention.", Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun MetricCard(label: String, value: String, hint: String, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
