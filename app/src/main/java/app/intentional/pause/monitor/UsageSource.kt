package app.intentional.pause.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import app.intentional.pause.data.Store

data class AppEvent(val pkg: String, val type: Int, val time: Long)

class UsageSource(private val context: Context) {
    fun events(from: Long, until: Long): List<AppEvent> {
        if (!Access.usage(context) || until <= from) return emptyList()
        return runCatching {
            val query = context.getSystemService(UsageStatsManager::class.java).queryEvents(from, until) ?: return emptyList()
            val event = UsageEvents.Event()
            buildList {
                while (query.hasNextEvent()) {
                    query.getNextEvent(event)
                    if (event.eventType in setOf(UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.ACTIVITY_PAUSED,
                            UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN, UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP)) {
                        add(AppEvent(event.packageName.orEmpty(), event.eventType, event.timeStamp))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun recover(store: Store) {
        val old = store.journal() ?: return
        val now = System.currentTimeMillis()
        val sameBoot = old.boot == Settings.Global.getLong(context.contentResolver, "boot_count", -2)
        val elapsed = SystemClock.elapsedRealtime() - old.elapsed
        val clockStable = sameBoot && elapsed >= 0 && kotlin.math.abs((now - old.wall) - elapsed) < 5000
        if (!Access.usage(context) || now < old.wall || now - old.wall > 7 * 86_400_000L) {
            store.recover(old.wall, "A monitoring interruption could not be recovered. Saved usage is preserved; the unobserved interval was not estimated.")
            return
        }
        val end = events(old.wall, now).firstOrNull {
            (it.type == UsageEvents.Event.ACTIVITY_PAUSED && it.pkg == old.pkg) ||
                (it.type == UsageEvents.Event.ACTIVITY_RESUMED && it.pkg != old.pkg) ||
                it.type in setOf(UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN, UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP)
        }?.time
        val until = end ?: if (clockStable) now else old.wall
        store.recover(until, if (end != null && now - end > 10_000 || !clockStable)
            "Monitoring was interrupted. The last confirmed session was recovered where Android supplied events. Later unobserved usage may be missing." else null)
    }
}
