package app.intentional.pause.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.intentional.pause.data.Answer

@Composable fun WarningScreen(appName: String, answers: List<Answer>, onContinue: (String) -> Unit, onLeave: () -> Unit) {
    var selected by remember(appName, answers) { mutableStateOf<String?>(null) }
    MaterialTheme(colorScheme = darkColorScheme(primary = AlertRed, background = Color.Black, surface = Color.Black)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val wide = maxWidth > 600.dp
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                if (wide) {
                    Row(Modifier.weight(1f).padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { WarningIntro(appName, compact = true) }
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { AnswerChoices(answers, selected, { selected = it }, compact = true) }
                    }
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onLeave, Modifier.weight(1f)) { Text("Leave this app", color = Color(0xFFBEBEBE)) }
                        ContinueButton(selected, onContinue, Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 16.dp)) {
                        WarningIntro(appName, compact = false)
                        Spacer(Modifier.height(22.dp))
                        AnswerChoices(answers, selected, { selected = it }, compact = false)
                    }
                    ContinueButton(selected, onContinue, Modifier.fillMaxWidth())
                    TextButton(onLeave, Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Leave this app", color = Color(0xFFBEBEBE)) }
                }
            }
        }
    }
}

@Composable private fun WarningIntro(appName: String, compact: Boolean) {
    Icon(Icons.Outlined.WarningAmber, "Warning", tint = AlertRed, modifier = Modifier.size(if (compact) 32.dp else 44.dp))
    Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
    Text("Pause before you scroll.", style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge, color = AlertRed)
    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
    Text("Excessive scrolling can weaken concentration, fuel compulsive habits, and take time from what matters.", color = Color(0xFFBEBEBE),
        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
    Surface(color = Color(0xFF202020), shape = RoundedCornerShape(50)) {
        Text("OPENING  ·  $appName", Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable private fun AnswerChoices(answers: List<Answer>, selected: String?, choose: (String) -> Unit, compact: Boolean) {
    Text("Why are you opening this app?", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, color = Color.White)
    Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
    answers.forEach { answer ->
        val checked = selected == answer.id
        Surface(color = if (checked) Color(0xFF321B1B) else Color(0xFF131313),
            border = BorderStroke(1.dp, if (checked) AlertRed else Color(0xFF303030)), shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = if (compact) 6.dp else 9.dp).selectable(checked, role = Role.RadioButton, onClick = { choose(answer.id) })) {
            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 13.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = checked, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AlertRed, unselectedColor = Color(0xFF8A8A8A)))
                Spacer(Modifier.width(12.dp))
                Text(answer.label, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }
    }
}

@Composable private fun ContinueButton(selected: String?, onContinue: (String) -> Unit, modifier: Modifier) {
    Button(onClick = { selected?.let(onContinue) }, enabled = selected != null,
        shape = RoundedCornerShape(16.dp), modifier = modifier.heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.Black, disabledContainerColor = Color(0xFF282828), disabledContentColor = Color(0xFF858585))) {
        Text("Continue")
    }
}
