package com.keepsimple.smallhabits

import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max

interface Task {
    val id: Int
    var taskTitle: String
    var time: Times
    var repeatDays: HashMap<Weekdays, Boolean>
    var active: Boolean
    var streak: Int
    var longestStreak: Int
    var history: ArrayList<Pair<Long, TaskHistoryLog>>
    var archived: Boolean

    fun dailyUpdate(date: Long) {
        if (active) {
            if (completed()) {
                streak += 1
                longestStreak = max(streak, longestStreak)
            } else {
                streak = 0
            }
        }
        updateActiveness(date)
        updateHistory(date)
    }

    fun updateActiveness(date: Long){
        when (TimeUtil.dayOfWeekFormat.format(date)) {
            "Sunday" -> {
                active = repeatDays[Weekdays.SUN]!!
            }
            "Monday" -> {
                active = repeatDays[Weekdays.MON]!!
            }
            "Tuesday" -> {
                active = repeatDays[Weekdays.TUE]!!
            }
            "Wednesday" -> {
                active = repeatDays[Weekdays.WED]!!
            }
            "Thursday" -> {
                active = repeatDays[Weekdays.THU]!!
            }
            "Friday" -> {
                active = repeatDays[Weekdays.FRI]!!
            }
            "Saturday" -> {
                active = repeatDays[Weekdays.SAT]!!
            }
        }
    }

    fun updateHistory(date: Long) {
        if (!archived && active) {
            history.add(Pair(date, TaskHistoryLog()))
            setProgress(0)
        }
    }

    fun updateProgress() {
        val pair = history[history.size - 1]
        val log = pair.second
        log.progressPercent = getProgressPercent()
        log.progressString = getProgressString()
    }

    fun clickOnTask(
        button: Button,
        progressBar: ProgressBar,
        progressLayout: LinearLayout,
        completedBackground: Drawable
    )
    fun completed() : Boolean
    fun getProgressCount() : Int
    fun getProgressPercent() : Int
    fun getProgressString() : String
    fun getProgressInterval() : Int
    fun setProgress(progress: Int)
}