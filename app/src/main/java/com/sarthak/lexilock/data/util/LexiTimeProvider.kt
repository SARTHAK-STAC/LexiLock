package com.sarthak.lexilock.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LexiTimeProvider {
    @Volatile
    var debugTimeOffsetMillis: Long = 0L

    fun currentTimeMillis(): Long {
        return System.currentTimeMillis() + debugTimeOffsetMillis
    }

    fun getCalendar(): Calendar {
        val cal = Calendar.getInstance()
        if (debugTimeOffsetMillis != 0L) {
            cal.timeInMillis = System.currentTimeMillis() + debugTimeOffsetMillis
        }
        return cal
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(getCalendar().time)
    }

    fun advanceHours(hours: Double) {
        val delta = (hours * 3600.0 * 1000.0).toLong()
        debugTimeOffsetMillis += delta
    }

    fun setHourOfDay(hour: Int, minute: Int = 0) {
        val realCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        debugTimeOffsetMillis = targetCal.timeInMillis - realCal.timeInMillis
    }

    fun resetTime() {
        debugTimeOffsetMillis = 0L
    }
}
