package app.intentional.pause.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.intentional.pause.data.*

@Composable fun AnswersScreen(answers: List<Answer>, store: Store, onBack: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Answer?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to settings") }; PageTitle("A reason to open", "Your intentions", "Keep the choices meaningful to you. Use the arrows to change their order.") }
        itemsIndexed(answers, key = { _, answer -> answer.id }) { index, answer ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}".padStart(2, '0'), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp)); Text(answer.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { store.update { p -> p.copy(answers = p.answers.toMutableList().apply { add(index - 1, removeAt(index)) }) } }, enabled = index > 0) { Icon(Icons.Outlined.ArrowUpward, "Move ${answer.label} up") }
                        IconButton(onClick = { store.update { p -> p.copy(answers = p.answers.toMutableList().apply { add(index + 1, removeAt(index)) }) } }, enabled = index < answers.lastIndex) { Icon(Icons.Outlined.ArrowDownward, "Move ${answer.label} down") }
                        IconButton(onClick = { editing = answer.id; text = answer.label }) { Icon(Icons.Outlined.Edit, "Edit ${answer.label}") }
                        IconButton(onClick = { deleting = answer }, enabled = answers.size > 1) { Icon(Icons.Outlined.DeleteOutline, "Delete ${answer.label}") }
                    }
                }
            }
        }
        item { FilledTonalButton(onClick = { editing = "new"; text = "" }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add an intention") } }
        item { Text("At least one answer is required. Choosing an answer never starts a time limit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (editing != null) AlertDialog(onDismissRequest = { editing = null }, title = { Text(if (editing == "new") "Add an intention" else "Edit intention") }, text = {
        OutlinedTextField(text, { if (it.length <= 120) text = it }, label = { Text("Answer") }, supportingText = { Text("${text.length}/120") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    }, confirmButton = { TextButton(onClick = {
        store.update { p -> p.copy(answers = if (editing == "new") p.answers + Answer(label = text.trim()) else p.answers.map { if (it.id == editing) it.copy(label = text.trim()) else it }) }
        editing = null
    }, enabled = text.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    deleting?.let { answer -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Remove this intention?") }, text = { Text(answer.label) },
        confirmButton = { TextButton(onClick = { store.update { p -> p.copy(answers = p.answers.filterNot { it.id == answer.id }.ifEmpty { p.answers }) }; deleting = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}
