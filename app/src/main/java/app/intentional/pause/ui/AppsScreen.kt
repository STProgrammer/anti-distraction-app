package app.intentional.pause.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.intentional.pause.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppsScreen(prefs: Preferences, installed: List<InstalledApp>, loaded: Boolean, store: Store) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("Your attention, your choice", "Mindful apps", "Choose where a moment of intention would help. Changes take effect immediately.") }
        item {
            FilledTonalButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add an app")
            }
        }
        if (!loaded) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (prefs.apps.isEmpty()) item { Panel { Text("Your list is clear", style = MaterialTheme.typography.titleLarge); Text("Add any app you want to open more intentionally.") } }
        items(prefs.apps, key = { it.pkg }) { app ->
            val installedApp = installed.firstOrNull { it.pkg == app.pkg }
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(installedApp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(installedApp?.label ?: app.label, style = MaterialTheme.typography.titleMedium)
                            Text(if (!loaded) "Checking installation…" else if (installedApp == null) "Not installed" else if (app.enabled) "Pause before opening" else "Monitoring off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(app.enabled, onCheckedChange = { enabled -> store.update { p -> p.copy(apps = p.apps.map { if (it.pkg == app.pkg) it.copy(enabled = enabled) else it }) } },
                            modifier = Modifier.semantics { contentDescription = "Monitor ${app.label}" })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app.pkg, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { store.update { p -> p.copy(apps = p.apps.filterNot { it.pkg == app.pkg }) } }) { Text("Remove") }
                    }
                }
            }
        }
        item { Text("Missing apps stay on your list and are monitored if installed later. Remove them whenever you like.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (picker) ModalBottomSheet(onDismissRequest = { picker = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.91f).padding(horizontal = 22.dp)) {
            Text("Choose apps", style = MaterialTheme.typography.headlineMedium)
            Text("Installed on this phone", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(query, { query = it }, label = { Text("Search apps") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
            val results = installed.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                if (results.isEmpty()) item { Text(if (loaded) "No apps match your search." else "Loading installed apps…", Modifier.padding(vertical = 28.dp)) }
                items(results, key = { it.pkg }) { app ->
                    val added = prefs.apps.any { it.pkg == app.pkg }
                    Surface(onClick = {
                        store.update { p -> p.copy(apps = if (added) p.apps.filterNot { it.pkg == app.pkg } else p.apps + TrackedApp(app.pkg, app.label)) }
                    }, shape = RoundedCornerShape(16.dp), color = if (added) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app); Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) { Text(app.label, style = MaterialTheme.typography.titleSmall); Text(app.pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Checkbox(checked = added, onCheckedChange = null)
                        }
                    }
                }
            }
            Button(onClick = { picker = false }, Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("Done") }
        }
    }
}

@Composable private fun AppIcon(app: InstalledApp?) {
    if (app != null) {
        val bitmap = remember(app.pkg, app.icon) { app.icon.toBitmap(96, 96).asImageBitmap() }
        Image(bitmap, null, Modifier.size(42.dp))
    } else Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Icon(Icons.Outlined.Apps, null, Modifier.padding(9.dp).size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
