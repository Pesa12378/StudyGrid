package com.pesa.studygrid.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pesa.studygrid.MainActivity
import com.pesa.studygrid.R
import com.pesa.studygrid.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

object Reminder {
    private const val CHANNEL = "reminders"
    private const val NOTIF_ID = 1

    fun nextTrigger(now: ZonedDateTime, minutes: Int): ZonedDateTime {
        val today = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minutes.toLong())
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        if (!prefs.getBoolean("reminder_on", false)) { am.cancel(pi); return }
        val at = nextTrigger(ZonedDateTime.now(), prefs.getInt("reminder_minutes", 1200))
        // Inexact window: no SCHEDULE_EXACT_ALARM permission needed.
        am.setWindow(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), 10 * 60_000L, pi)
    }

    internal suspend fun notifyIfMissing(context: Context) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val nm = NotificationManagerCompat.from(context)
        if (!granted || !nm.areNotificationsEnabled()) return
        val dao = AppDatabase.get(context).dao()
        val today = LocalDate.now().toEpochDay()
        val done = dao.allLogs().filter { it.epochDay == today && it.intensity > 0 }.map { it.goalId }.toSet()
        val missing = dao.allGoals().filter { it.id !in done }.map { it.title }
        if (missing.isEmpty()) return
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Recordatorio diario").build(),
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = "Te falta: " + missing.joinToString(", ")
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle("¿Ya estudiaste hoy?")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Reminder.schedule(app)
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminder.notifyIfMissing(app)
            } finally {
                Reminder.schedule(app) // next day, even if the DB read failed
                pending.finish()
            }
        }
    }
}
