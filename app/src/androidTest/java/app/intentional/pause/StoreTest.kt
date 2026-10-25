package app.intentional.pause

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.intentional.pause.data.Store
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class StoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "verification.db"
    private lateinit var store: Store
    @Before fun prepare() { context.deleteDatabase(name); store = Store(context, name) }
    @After fun finish() { store.close(); context.deleteDatabase(name) }
    @Test fun settingsAndAnswerOrderSurviveReopening() {
        store.update { it.copy(enabled = false, timer = false, timerOffset = 45, answers = it.answers.reversed(), apps = it.apps.drop(2)) }
        val expected = store.prefs.value
        store.close(); store = Store(context, name)
        assertEquals(expected, store.prefs.value)
    }
    @Test fun cannotSaveAnEmptyAnswerList() {
        assertThrows(IllegalArgumentException::class.java) { store.update { it.copy(answers = emptyList()) } }
        assertEquals(5, store.prefs.value.answers.size)
    }
    @Test fun recoveryIsAtomicAndIdempotentAcrossMidnight() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val midnight = today.atStartOfDay(zone).toInstant().toEpochMilli()
        store.writableDatabase.execSQL("INSERT INTO journal VALUES(1,?,?,?,?,?)", arrayOf("test.app", midnight - 10_000, 0, 1, zone.id))
        store.recover(midnight + 20_000)
        assertNull(store.journal())
        assertEquals(10_000L, store.days(2)[today.minusDays(1)])
        assertEquals(20_000L, store.days(2)[today])
        store.recover(midnight + 90_000)
        assertEquals(20_000L, store.days(2)[today])
        store.close(); store = Store(context, name)
        assertEquals(30_000L, store.days(2).values.sum())
    }
    @Test fun duplicateStartsDoNotOverlapAndStoppedTimerFreezes() {
        store.start("a"); Thread.sleep(120); store.start("a"); Thread.sleep(120); store.stop()
        val total = store.days(1).values.sum()
        assertTrue("Total was $total", total in 220..1500)
        Thread.sleep(100); assertEquals(total, store.days(1).values.sum()); assertNull(store.journal())
    }
}
