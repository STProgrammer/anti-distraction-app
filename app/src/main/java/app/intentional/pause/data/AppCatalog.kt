package app.intentional.pause.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(val pkg: String, val label: String, val icon: Drawable)

object AppCatalog {
    fun installed(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
            .mapNotNull { info -> runCatching {
                InstalledApp(info.activityInfo.packageName, info.loadLabel(pm).toString(), info.loadIcon(pm))
            }.getOrNull() }.filter { it.pkg != context.packageName }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
}
