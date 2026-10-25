package app.intentional.pause.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.intentional.pause.monitor.Access

@Composable fun SetupScreen(usage: Boolean, accessibility: Boolean, connected: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler { if (step > 0) step-- else onDone() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Pause, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp)); Text("intent", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f)); Text("${step + 1} / 3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Spacer(Modifier.height(18.dp))
                when (step) {
                    0 -> {
                        Surface(color = DeepTeal, shape = RoundedCornerShape(32.dp), modifier = Modifier.size(104.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Pause, null, Modifier.size(56.dp), tint = Mint) }
                        }
                        PageTitle("Welcome to Intent", "Open with purpose.\nLeave with time.", "Turn the automatic tap into a conscious choice.")
                        Panel {
                            SettingRow(Icons.Outlined.TouchApp, "Pause before scrolling", "Choose why you're opening an app, then continue freely.")
                            SettingRow(Icons.Outlined.Schedule, "See the whole picture", "One daily timer across your monitored apps.")
                            SettingRow(Icons.Outlined.Lock, "Entirely on your phone", "No accounts, uploads, or time limits.")
                        }
                    }
                    1 -> {
                        PageTitle("Two permissions. One purpose.", "Let Intent help.", "You stay in control. Both permissions can be turned off at any time in Android settings.")
                        Panel {
                            Text("1 · Usage Access", style = MaterialTheme.typography.titleLarge)
                            Text("Checks which app is active and helps recover the last session after an interruption. It does not reveal what you read or type.", style = MaterialTheme.typography.bodyMedium)
                            FilledTonalButton(onClick = { Access.usageSettings(context) }, Modifier.fillMaxWidth()) { Text(if (usage) "Allowed · review access" else "Allow Usage Access") }
                        }
                        Panel {
                            Text("2 · Accessibility", style = MaterialTheme.typography.titleLarge)
                            Text("Notices app/window changes and displays the intention screen and timer. Android bundles window metadata with screen-access permission. Intent uses only window types, focus, and app identifiers; it never queries screen contents, messages, or passwords.", style = MaterialTheme.typography.bodyMedium)
                            FilledTonalButton(onClick = { Access.accessibilitySettings(context) }, Modifier.fillMaxWidth()) { Text(if (accessibility) "Enabled · review service" else "Enable Intent service") }
                        }
                        Text("If Android shows Restricted settings, open Intent's App info and use Allow restricted settings if Android offers it and you trust this APK. Then return to Accessibility → Installed apps → Intent. Device policy may prevent enabling it; Intent cannot bypass that protection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { Access.appSettings(context) }) { Text("Open Intent app info") }
                    }
                    else -> {
                        PageTitle("Ready for everyday use", "Keep your pause\nwithin reach.", "Samsung may suspend apps in the background. These settings help Intent stay available.")
                        Panel {
                            Text("On your Galaxy A36", style = MaterialTheme.typography.titleLarge)
                            Text("1. Settings → Apps → Intent → Battery → Unrestricted, if shown.\n\n2. Settings → Battery → Background usage limits → Never sleeping apps (or Never auto sleeping apps) → add Intent.\n\n3. Keep Intent out of Sleeping and Deep sleeping apps. Labels may vary with One UI updates.", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { Access.appSettings(context) }, Modifier.fillMaxWidth()) { Text("Open app battery settings") }
                            TextButton(onClick = { Access.batterySettings(context) }, Modifier.fillMaxWidth()) { Text("Review battery optimization") }
                        }
                        Panel {
                            Text(if (usage && accessibility && connected) "You're ready." else "Setup still needs attention", style = MaterialTheme.typography.titleLarge)
                            Text("Usage Access: ${if (usage) "allowed" else "off"}\nAccessibility: ${if (accessibility) "enabled" else "off"}\nService: ${if (connected) "connected" else "not connected"}", style = MaterialTheme.typography.bodyMedium)
                            if (!usage || !accessibility) TextButton(onClick = { step = 1 }) { Text("Review permissions") }
                        }
                        Text("No separate overlay or notification permission is needed. Android restores enabled accessibility services after a reboot; check Intent if Samsung reports it as stopped.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = { if (step < 2) step++ else onDone() }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) {
                Text(when (step) { 0 -> "Get started"; 1 -> "Continue setup"; else -> "Go to my dashboard" })
            }
            TextButton(onClick = onDone, Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text(if (step < 2) "Set up later" else "I'll review the settings later") }
        }
    }
}
