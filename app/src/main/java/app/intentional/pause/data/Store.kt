package app.intentional.pause.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import app.intentional.pause.core.UsageMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class TrackedApp(val pkg: String, val label: String, val enabled: Boolean = true)
data class Answer(val id: String = UUID.randomUUID().toString(), val label: String)
data class Preferences(
    val enabled: Boolean = true,
    val timer: Boolean = true,
    val timerOffset: Int = 8,
    val onboarded: Boolean = false,
    val apps: List<TrackedApp> = defaults,
    val answers: List<Answer> = defaultAnswers,
) {
    companion object {
        val defaults = listOf(
            TrackedApp("com.instagram.android", "Instagram"),
            TrackedApp("com.google.android.youtube", "YouTube"),
            TrackedApp("com.zhiliaoapp.musically", "TikTok"),
            TrackedApp("com.twitter.android", "X"),
            TrackedApp("com.facebook.katana", "Facebook"),
            TrackedApp("com.snapchat.android", "Snapchat"),
            TrackedApp("com.instagram.barcelona", "Threads"),
            TrackedApp("com.upscrolled.app", "UpScrolled"),
            TrackedApp("com.reddit.frontpage", "Reddit"),
        )
        val defaultAnswers = listOf(
            "To use messenger", "To follow news", "To watch/read some educational content",
            "To post on social media", "Other"
        ).mapIndexed { i, text -> Answer("default-$i", text) }
    }
}

data class Journal(val pkg: String, val wall: Long, val elapsed: Long, val boot: Long, val zone: String)
data class LiveState(val connected: Boolean = false, val active: String? = null, val counting: Boolean = false, val error: String? = null)

class Store(context: Context, name: String = "intent.db") : SQLiteOpenHelper(context, name, null, 1) {
    private val mutablePrefs = MutableStateFlow(Preferences())
    val prefs = mutablePrefs.asStateFlow()
    val revision = MutableStateFlow(0L)
    val live = MutableStateFlow(LiveState())
    private var anchor: Journal? = null
    private val boot: Long get() = android.provider.Settings.Global.getLong(appContext.contentResolver, "boot_count", -1)
    private val appContext = context.applicationContext

    init {
        setWriteAheadLoggingEnabled(true)
        mutablePrefs.value = readPreferences()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE preferences (id INTEGER PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE daily (day TEXT PRIMARY KEY, millis INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE journal (id INTEGER PRIMARY KEY, package TEXT NOT NULL, wall INTEGER NOT NULL, elapsed INTEGER NOT NULL, boot INTEGER NOT NULL, zone TEXT NOT NULL)")
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, value TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized fun update(transform: (Preferences) -> Preferences) {
        val next = transform(mutablePrefs.value)
        require(next.answers.isNotEmpty() && next.answers.all { it.label.isNotBlank() && it.label.length <= 120 })
        require(next.answers.map { it.id }.distinct().size == next.answers.size)
        val json = JSONObject().put("enabled", next.enabled).put("timer", next.timer)
            .put("offset", next.timerOffset).put("onboarded", next.onboarded)
            .put("apps", JSONArray().apply { next.apps.distinctBy { it.pkg }.forEach { put(JSONObject().put("pkg", it.pkg).put("label", it.label).put("enabled", it.enabled)) } })
            .put("answers", JSONArray().apply { next.answers.forEach { put(JSONObject().put("id", it.id).put("label", it.label.trim())) } })
        writableDatabase.insertWithOnConflict("preferences", null, ContentValues().apply { put("id", 1); put("value", json.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
        mutablePrefs.value = next
    }

    private fun readPreferences(): Preferences = readableDatabase.rawQuery("SELECT value FROM preferences WHERE id=1", null).use { cursor ->
        if (!cursor.moveToFirst()) {
            val regional = listOf("com.ss.android.ugc.trill" to "TikTok")
                .filter { (pkg, _) -> appContext.packageManager.getLaunchIntentForPackage(pkg) != null }
                .map { (pkg, label) -> TrackedApp(pkg, label) }
            return@use Preferences(apps = Preferences.defaults + regional)
        }
        runCatching {
            val json = JSONObject(cursor.getString(0))
            val apps = json.getJSONArray("apps")
            val answers = json.getJSONArray("answers")
            Preferences(json.optBoolean("enabled", true), json.optBoolean("timer", true), json.optInt("offset", 8), json.optBoolean("onboarded"),
                List(apps.length()) { val a = apps.getJSONObject(it); TrackedApp(a.getString("pkg"), a.getString("label"), a.optBoolean("enabled", true)) },
                List(answers.length()) { val a = answers.getJSONObject(it); Answer(a.getString("id"), a.getString("label")) }.ifEmpty { Preferences.defaultAnswers })
        }.getOrElse { Preferences() }
    }

    @Synchronized fun journal(): Journal? = readableDatabase.rawQuery("SELECT package,wall,elapsed,boot,zone FROM journal WHERE id=1", null).use {
        if (!it.moveToFirst()) null else Journal(it.getString(0), it.getLong(1), it.getLong(2), it.getLong(3), it.getString(4))
    }

    private fun saveAnchor(db: SQLiteDatabase, value: Journal?) {
        db.delete("journal", null, null)
        value?.let { j -> db.insertOrThrow("journal", null, ContentValues().apply {
            put("id", 1); put("package", j.pkg); put("wall", j.wall); put("elapsed", j.elapsed); put("boot", j.boot); put("zone", j.zone)
        }) }
    }

    /** The monotonic clock measures duration; wall time only assigns local calendar days. */
    @Synchronized fun start(pkg: String) {
        if (anchor?.pkg == pkg) return
        stop()
        anchor = Journal(pkg, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot, ZoneId.systemDefault().id)
        saveAnchor(writableDatabase, anchor)
    }

    @Synchronized fun checkpoint(stop: Boolean = false) {
        val current = anchor ?: return
        val elapsed = SystemClock.elapsedRealtime()
        val duration = (elapsed - current.elapsed).coerceAtLeast(0)
        val next = if (stop) null else Journal(current.pkg, System.currentTimeMillis(), elapsed, boot, ZoneId.systemDefault().id)
        val db = writableDatabase
        db.beginTransaction()
        try {
            addSlices(db, UsageMath.split(current.wall, current.wall + duration, ZoneId.of(current.zone)))
            saveAnchor(db, next)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        anchor = next
        revision.value++
    }
    @Synchronized fun stop() = checkpoint(true)

    @Synchronized fun recover(until: Long, note: String? = null) {
        val old = journal() ?: return
        val db = writableDatabase
        db.beginTransaction()
        try {
            addSlices(db, UsageMath.split(old.wall, until.coerceAtLeast(old.wall), ZoneId.of(old.zone)))
            saveAnchor(db, null)
            if (note != null) db.insertWithOnConflict("notes", null, ContentValues().apply { put("id", 1); put("value", note) }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        anchor = null
        revision.value++
    }

    private fun addSlices(db: SQLiteDatabase, slices: Map<LocalDate, Long>) {
        slices.forEach { (date, millis) ->
            db.execSQL("INSERT INTO daily(day,millis) VALUES(?,?) ON CONFLICT(day) DO UPDATE SET millis=millis+excluded.millis", arrayOf<Any>(date.toString(), millis))
        }
    }

    @Synchronized fun days(count: Int = 30): Map<LocalDate, Long> {
        val today = LocalDate.now()
        val result = (count - 1 downTo 0).associate { today.minusDays(it.toLong()) to 0L }.toMutableMap()
        readableDatabase.rawQuery("SELECT day,millis FROM daily WHERE day>=? AND day<=?", arrayOf(today.minusDays(count - 1L).toString(), today.toString())).use {
            while (it.moveToNext()) result[LocalDate.parse(it.getString(0))] = it.getLong(1)
        }
        anchor?.let { a ->
            UsageMath.split(a.wall, a.wall + (SystemClock.elapsedRealtime() - a.elapsed).coerceAtLeast(0), ZoneId.of(a.zone)).forEach { (day, ms) ->
                if (day in result) result[day] = (result[day] ?: 0) + ms
            }
        }
        return result
    }
    fun note(): String? = readableDatabase.rawQuery("SELECT value FROM notes WHERE id=1", null).use { if (it.moveToFirst()) it.getString(0) else null }
    fun dismissNote() { writableDatabase.delete("notes", null, null); revision.value++ }
}
