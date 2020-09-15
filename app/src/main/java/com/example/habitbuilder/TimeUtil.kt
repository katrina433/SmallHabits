package com.example.habitbuilder

import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    val oneSecond = 1000L
    val oneMinute= oneSecond * 60L
    val oneHour = oneMinute * 60L
    val oneDay = oneHour * 24L

    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)

    fun sameDay(dateOne: Long, dateTwo: Long): Boolean {
        return dateFormat.format(dateOne) == dateFormat.format(dateTwo)
    }

    fun getMissingDays(fromDay: Long): ArrayList<Long> {
        val missingDays = ArrayList<Long>()
        var curDay = fromDay
        while (!sameDay(curDay, System.currentTimeMillis())) {
            missingDays.add(curDay + oneDay)
            curDay += oneDay
        }
        return missingDays
    }

    fun getTimeString(duration: Long): String {
        val hour = duration / oneHour
        val minute = duration % oneHour / oneMinute
        val second = duration % oneHour % oneMinute / oneSecond

        fun fillZero(duration: Long): String {
            if (duration < 10) {
                return "0$duration"
            }
            return "$duration"
        }
        if (hour == 0L) {
            return "${fillZero(minute)}:${fillZero(second)}"
        }
        return "${fillZero(hour)}:${fillZero(minute)}:${fillZero(second)}"
    }

    fun getTimeString(hour: Int, minute: Int): String {
        var amPm = "am"
        var hourString = hour.toString()
        var minuteString = minute.toString()
        if (hour >= 12) {
            amPm = "pm"
            hourString = (hour - 12).toString()
        }
        if (hourString == "0") {
            hourString = "12"
        }
        if (minute < 10) {
            minuteString = "0$minute"
        }
        return "$hourString:$minuteString $amPm"
    }
}