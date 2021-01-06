package com.keepsimple.smallhabits

import android.graphics.drawable.Drawable
import android.widget.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

class TimerTask (
    override val id: Int,
    override var taskTitle: String,
    override var time: Times,
    override var repeatDays: HashMap<Weekdays, Boolean>,
    var goalDuration: Long,
    var goalSessions: Int
) : Task {

    override var active = false
    override var streak = 0
    override var longestStreak = 0
    override var history = ArrayList<Pair<Long, TaskHistoryLog>>()
    override var archived = false
    var sessionsDone = 0

    override fun clickOnTask(
        button: Button,
        progressBar: ProgressBar,
        progressLayout: LinearLayout,
        completedBackground: Drawable
    ) {}

    override fun completed(): Boolean {
        return sessionsDone >= goalSessions
    }

    override fun getProgressInterval(): Int {
        return goalSessions
    }

    override fun getProgressCount(): Int {
        return sessionsDone
    }

    override fun getProgressPercent(): Int {
        return min(100, 100 * sessionsDone / goalSessions)
    }

    override fun getProgressString(): String {
        return "${TimeUtil.getTimeString(sessionsDone * goalDuration)} / " +
                TimeUtil.getTimeString(goalSessions * goalDuration)
    }

    override fun setProgress(progress: Int) {
        sessionsDone = progress
        updateProgress()
    }

    fun getSessionString(): String {
        return "$sessionsDone/$goalSessions"
    }

    fun getGoal(): Array<String> {
        val hour = goalDuration / TimeUtil.oneHour
        val minute = goalDuration % TimeUtil.oneHour / TimeUtil.oneMinute
        return arrayOf(hour.toString(), minute.toString(), goalSessions.toString())
    }
}