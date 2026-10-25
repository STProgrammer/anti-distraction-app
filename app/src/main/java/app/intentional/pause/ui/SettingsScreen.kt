package app.intentional.pause.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.intentional.pause.data.*
import app.intentional.pause.monitor.Access

@Composable fun SettingsScreen(prefs: Preferences, store: Store, usage: Boolean, accessibility: Boolean, live: LiveState, onAnswers: () -> Unit, onSetup: () -> Unit) {
    val context = LocalContext.current
    var offset by remember(prefs.timerOffset) { mutableFloatStateOf(prefs.timerOffset.toFloat()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("Make it yours", "A gentler default.", "A small pause that fits the way you use your phone.")
        Panel {
            SettingRow(Icons.Outlined.Shield, "Monitoring", if (prefs.enabled) "On for your selected apps" else "Paused across all apps", trailing = {
                Switch(prefs.enabled, { value -> store.update { it.copy(enabled = value) } }, Modifier.semantics { contentDescription = "Global monitoring" })
            })
            HorizontalDivider()
            SettingRow(Icons.Outlined.ChatBubbleOutline, "Intention answers", "${prefs.answers.size} choices · edit and reorder", onAnswers)
        }
        Panel {
            SettingRow(Icons.Outlined.Schedule, "Live timer", "A quiet reminder of today's total", trailing = {
                Switch(prefs.timer, { value -> store.update { it.copy(timer = value) } }, Modifier.semantics { contentDescription = "Live timer visibility" })
            })
            if (prefs.timer) {
                Text("Timer distance from top: ${offset.toInt()} dp", style = MaterialTheme.typography.labelLarge)
                Slider(offset, { offset = it }, valueRange = 0f..120f, steps = 11, onValueChangeFinished = { store.update { it.copy(timerOffset = offset.toInt()) } },
                    modifier = Modifier.semantics { contentDescription = "Timer vertical position" })
                Text("The bar sits below system bars and lets every touch pass through. Move it down if it covers a control.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Panel {
            Text("Permissions & reliability", style = MaterialTheme.typography.titleLarge)
            SettingRow(Icons.Outlined.AccessibilityNew, "Accessibility service", if (accessibility && live.connected) "Connected" else if (accessibility) "Not connected · toggle the service off, then on" else "Off · required for prompts", { Access.accessibilitySettings(context) })
            SettingRow(Icons.Outlined.QueryStats, "Usage Access", if (usage) "Allowed" else "Off · required for reliable tracking", { Access.usageSettings(context) })
            SettingRow(Icons.Outlined.BatteryFull, "Battery & background", "Samsung setup guidance", onSetup)
            live.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        Panel {
            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Text("Private by design", style = MaterialTheme.typography.titleLarge)
            Text("Everything stays on this phone. No account, network permission, ads, or analytics. Intent observes app transitions, never messages, passwords, or screen contents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Usage records stay through app updates. Uninstalling Intent removes its data. No cloud backup is used.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            Text("A few honest limits", style = MaterialTheme.typography.titleMedium)
            Text("Android reports an app after it opens, so a brief glimpse can appear before the pause. Only the foreground app is counted; background audio and picture-in-picture are excluded once focus leaves. Split-screen follows reported activity transitions. If Android stops the service, prompts cannot run until it reconnects; unrecoverable time is not estimated.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("INTENT  ·  1.0.0\nA moment of choice, every time.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
