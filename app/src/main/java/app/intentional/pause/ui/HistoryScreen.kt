package app.intentional.pause.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.intentional.pause.core.UsageMath
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun HistoryScreen(days: Map<LocalDate, Long>) {
    var period by rememberSaveable { mutableIntStateOf(7) }
    val visible = days.entries.toList().takeLast(period).associate { it.key to it.value }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("The bigger picture", "Time, with perspective.", "Your combined daily usage. A record to reflect on, never a score to chase.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(7, 30).forEach { count -> FilterChip(selected = period == count, onClick = { period = count }, label = { Text("Last $count days") }) }
            }
        }
        item {
            Panel {
                Text(UsageMath.short(visible.values.sum()), style = MaterialTheme.typography.headlineLarge)
                Text("Total across $period days · ${UsageMath.short(visible.values.sum() / period)} per day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                UsageChart(visible)
            }
        }
        if (visible.values.all { it == 0L }) item {
            Panel { Text("A fresh start", style = MaterialTheme.typography.titleLarge); Text("Your history will appear as you use monitored apps after choosing an intention.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { Text("Day by day", style = MaterialTheme.typography.titleLarge) }
        items(visible.entries.toList().reversed(), key = { it.key.toString() }) { (date, total) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("EEEE")), style = MaterialTheme.typography.titleSmall)
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM yyyy")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(UsageMath.clock(total), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        }
    }
}

@Composable fun UsageChart(days: Map<LocalDate, Long>, compact: Boolean = false) {
    val entries = days.entries.toList()
    if (entries.isEmpty()) return
    var selected by remember(days.size) { mutableIntStateOf(entries.lastIndex) }
    val max = maxOf(entries.maxOf { it.value }, 60_000L)
    val primary = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.primaryContainer
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = entries.joinToString(". ") { "${it.key}: ${UsageMath.clock(it.value)}" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (entries.all { it.value == 0L }) "No usage recorded yet" else "Tap a day to explore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(UsageMath.short(max), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(if (compact) 110.dp else 160.dp).semantics { contentDescription = description }
            .pointerInput(entries) { detectTapGestures { selected = ((it.x / size.width) * entries.size).toInt().coerceIn(entries.indices) } }) {
            val step = size.width / entries.size
            val gap = if (entries.size > 7) 3.dp.toPx() else 14.dp.toPx()
            repeat(3) { n -> val y = size.height * n / 2; drawLine(grid.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
            entries.forEachIndexed { i, entry ->
                val height = maxOf(3.dp.toPx(), size.height * entry.value.toFloat() / max)
                drawRoundRect(if (i == selected) primary else faint, Offset(i * step + gap / 2, size.height - height), Size((step - gap).coerceAtLeast(2f), height), CornerRadius(5.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (entries.size <= 7) entries.forEach { Text(it.key.format(DateTimeFormatter.ofPattern("EEEEE")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else listOf(entries.first().key, entries[entries.size / 2].key, entries.last().key).forEach { Text(it.format(DateTimeFormatter.ofPattern("d MMM")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (!compact) entries.getOrNull(selected)?.let { Text("${it.key.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}  ·  ${UsageMath.clock(it.value)}", style = MaterialTheme.typography.labelLarge, color = primary) }
    }
}
