package app.intentional.pause

import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.intentional.pause.data.*
import app.intentional.pause.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun warningRequiresExactlyOneAnswerAndOtherNeedsNoText() {
        var chosen: String? = null
        var left = false
        compose.setContent { IntentTheme { WarningScreen("Test app", Preferences.defaultAnswers, { chosen = it }, { left = true }) } }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("To use messenger").performScrollTo().performClick()
        compose.onNodeWithText("To use messenger").assertIsSelected()
        compose.onNodeWithText("Other").performScrollTo().performClick()
        compose.onNodeWithText("Other").assertIsSelected()
        compose.onNodeWithText("To use messenger").assertIsNotSelected()
        compose.onNodeWithText("Continue").performClick()
        assertEquals("default-4", chosen)
        compose.onNodeWithText("Leave this app").performClick(); assertTrue(left)
    }
    @Test fun intentionEditorAddsEditsAndReorders() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("ui-check.db")
        val store = Store(context, "ui-check.db")
        try {
            compose.setContent { val prefs by store.prefs.collectAsState(); IntentTheme { Surface { AnswersScreen(prefs.answers, store) {} } } }
            compose.onNodeWithContentDescription("Move To follow news up").performScrollTo().performClick()
            compose.runOnIdle { assertEquals("To follow news", store.prefs.value.answers.first().label) }
            compose.onNodeWithContentDescription("Edit To follow news").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Read a saved article")
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithText("Read a saved article").assertExists()
            compose.onNodeWithText("Add an intention").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Reply to a friend")
            compose.onNodeWithText("Save").performClick()
            compose.runOnIdle { assertEquals(6, store.prefs.value.answers.size) }
            compose.onNodeWithContentDescription("Delete Reply to a friend").performScrollTo().performClick()
            compose.onNodeWithText("Remove").performClick()
            compose.runOnIdle { assertEquals(5, store.prefs.value.answers.size) }
        } finally { store.close(); context.deleteDatabase("ui-check.db") }
    }
    @Test fun largeTextKeepsWarningActionsReachable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                IntentTheme { WarningScreen("A monitored application with a long name", Preferences.defaultAnswers, {}, {}) }
            }
        }
        compose.onNodeWithText("Continue").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Other").performScrollTo().performClick()
        compose.onNodeWithText("Continue").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Leave this app").assertIsDisplayed()
    }
}
