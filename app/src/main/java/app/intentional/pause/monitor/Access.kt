package app.intentional.pause.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

object Access {
    fun usage(context: Context): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    fun accessibility(context: Context): Boolean = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        .orEmpty().split(':').any { ComponentName.unflattenFromString(it)?.packageName == context.packageName }
    fun accessibilitySettings(context: Context) = open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    fun usageSettings(context: Context) = open(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    fun appSettings(context: Context) = open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    fun batterySettings(context: Context) = open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    private fun open(context: Context, intent: Intent, fallback: Intent = Intent(Settings.ACTION_SETTINGS)) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        runCatching { context.startActivity(intent.addFlags(flags)) }
            .onFailure { runCatching { context.startActivity(fallback.addFlags(flags)) } }
    }
}
