package com.sarthak.lexilock.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sarthak.lexilock.MainActivity
import com.sarthak.lexilock.data.preferences.LexiPreferences
import com.sarthak.lexilock.data.repository.WordRepository
import java.util.Calendar

class DailyWordReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = LexiPreferences.getInstance(context)
        if (!prefs.isDailyNotificationEnabled) return

        val repository = WordRepository(context)
        val words = repository.getTodayWords()

        if (words.size == 1) {
            val word = words[0]
            showSingleNotification(context, word.word, word.definition)
        } else {
            val title = "Today's ${words.size} words: " + words.joinToString(" · ") { it.word.replaceFirstChar { w -> w.uppercase() } }
            val bigContent = words.joinToString("\n\n") {
                "${it.word.replaceFirstChar { w -> w.uppercase() }} (${it.partOfSpeech.lowercase()}):\n${it.definition}"
            }
            showMultiNotification(context, "📚 Today's ${words.size} Words", title, bigContent)
        }

        // Reschedule for next day
        DailyWordNotificationScheduler.scheduleDailyNotification(context)
    }

    private fun showSingleNotification(context: Context, word: String, definition: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "lexilock_daily_channel"
        createNotificationChannel(notificationManager, channelId)

        val pendingIntent = createContentIntent(context)

        val formattedWord = word.replaceFirstChar { it.uppercase() }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Word of the Day: $formattedWord")
            .setContentText(definition)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$formattedWord\n\n$definition\n\nTap to open LexiLock →")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }

    private fun showMultiNotification(context: Context, title: String, summary: String, bigText: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "lexilock_daily_channel"
        createNotificationChannel(notificationManager, channelId)

        val pendingIntent = createContentIntent(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$bigText\n\nTap to practice today's vocabulary →")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Vocabulary Words",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily notification with today's English vocabulary words"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}

object DailyWordNotificationScheduler {

    fun scheduleDailyNotification(context: Context) {
        val prefs = LexiPreferences.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyWordReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!prefs.isDailyNotificationEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.notificationHour)
            set(Calendar.MINUTE, prefs.notificationMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time is already past for today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyWordReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNewWordNotification(context: Context, word: com.sarthak.lexilock.domain.model.WordItem) {
        val prefs = LexiPreferences.getInstance(context)
        if (!prefs.isDailyNotificationEnabled) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelId = "lexilock_daily_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Vocabulary Words",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily notification with scheduled English vocabulary words"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedWord = word.word.replaceFirstChar { it.uppercase() }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📖 New Scheduled Word: $formattedWord")
            .setContentText(word.definition)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$formattedWord (${word.partOfSpeech.lowercase()})\n\n${word.definition}\n\n“${word.exampleSentence}”")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(102, notification)
    }
}

