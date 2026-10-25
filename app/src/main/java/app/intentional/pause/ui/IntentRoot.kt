package app.intentional.pause.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.intentional.pause.data.*
import app.intentional.pause.monitor.Access
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable fun IntentRoot(store: Store) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val prefs by store.prefs.collectAsStateWithLifecycle()
    val live by store.live.collectAsStateWithLifecycle()
    val revision by store.revision.collectAsStateWithLifecycle()
    var days by remember { mutableStateOf<Map<LocalDate, Long>>(emptyMap()) }
    var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(Access.usage(context)) }
    var accessibility by remember { mutableStateOf(Access.accessibility(context)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var answers by rememberSaveable { mutableStateOf(false) }
    var setup by rememberSaveable { mutableStateOf(!prefs.onboarded) }
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            installed = withContext(Dispatchers.IO) { AppCatalog.installed(context) }
            loaded = true
            while (true) {
                usage = Access.usage(context)
                accessibility = Access.accessibility(context)
                days = withContext(Dispatchers.IO) { store.days(30) }
                delay(1000)
            }
        }
    }
    LaunchedEffect(revision) { days = withContext(Dispatchers.IO) { store.days(30) } }
    val functioning = prefs.enabled && accessibility && usage && live.connected && live.error == null
    if (setup) {
        SetupScreen(usage, accessibility, live.connected, onDone = { store.update { it.copy(onboarded = true) }; setup = false })
        return
    }
    BackHandler(answers || tab != 0) { if (answers) answers = false else tab = 0 }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            listOf("Today" to Icons.Outlined.SpaceDashboard, "History" to Icons.Outlined.BarChart,
                "Apps" to Icons.Outlined.Apps, "Settings" to Icons.Outlined.Tune).forEachIndexed { index, item ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index; answers = false },
                    icon = { Icon(item.second, null) }, label = { Text(item.first) })
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when {
                answers -> AnswersScreen(prefs.answers, store, onBack = { answers = false })
                tab == 0 -> Dashboard(days, prefs, live, functioning, installed, store.note(),
                    onSetup = { setup = true }, onApps = { tab = 2 }, onHistory = { tab = 1 }, onDismissNote = { store.dismissNote() })
                tab == 1 -> HistoryScreen(days)
                tab == 2 -> AppsScreen(prefs, installed, loaded, store)
                else -> SettingsScreen(prefs, store, usage, accessibility, live, onAnswers = { answers = true }, onSetup = { setup = true })
            }
        }
    }
}
