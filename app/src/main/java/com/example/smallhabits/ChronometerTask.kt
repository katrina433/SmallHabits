package com.keepsimple.smallhabits

import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.view.children
import com.keepsimple.smallhabits.TimeUtil.oneHour
import com.keepsimple.smallhabits.TimeUtil.oneMinute
import com.keepsimple.smallhabits.TimeUtil.oneSecond
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

class ChronometerTask (
    override val id: Int,
    override var taskTitle: String,
    override var time: Times,
    override var repeatDays: HashMap<Weekdays, Boolean>,
    var goalDuration: Long
) : Task {

    override var active = false
    override var streak = 0
    override var longestStreak = 0
    override var history = ArrayList<Pair<Long, TaskHistoryLog>>()
    override var archived = false
    private var chronometerBase = SystemClock.elapsedRealtime()
    private var timeElapsed = 0L
    private var isRunning = false

    override fun clickOnTask(
        button: Button,
        progressBar: ProgressBar,
        progressLayout: LinearLayout,
        completedBackground: Drawable
    ) {
        val chronometer = progressLayout.children.elementAt(0) as Chronometer
        if (!isRunning) {
            isRunning = true
            chronometerBase = SystemClock.elapsedRealtime() - timeElapsed
            chronometer.base = chronometerBase
            chronometer.start()

            chronometer.setOnChronometerTickListener {
                progressBar.progress = getProgressPercent()
                if (completed()) {
                    button.background = completedBackground
                }
            }
        } else {
            timeElapsed = SystemClock.elapsedRealtime() - chronometerBase
            isRunning = false
            chronometer.stop()
        }
        updateProgress()
    }

    override fun completed(): Boolean {
        return getTimeElapsed() >= goalDuration
    }

    override fun getProgressCount(): Int {
        return (getProgressInterval() * getTimeElapsed() / goalDuration).toInt()
    }

    override fun getProgressPercent(): Int {
        return min(100, 100 * getTimeElapsed() / goalDuration).toInt()
    }

    override fun getProgressString(): String {
        return "${TimeUtil.getTimeString(getTimeElapsed())} / ${TimeUtil.getTimeString(goalDuration)}"
    }

    override fun setProgress(progress: Int) {
        isRunning = false
        timeElapsed = min(progress * getIntervalTime(), goalDuration)
        updateProgress()
    }

    override fun getProgressInterval(): Int {
        val intervalTime = getIntervalTime()
        if (goalDuration % intervalTime == 0L) {
            return (goalDuration / intervalTime).toInt()
        }
        return (goalDuration / getIntervalTime()).toInt() + 1
    }

    override fun dailyUpdate(date: Long) {
        if (isRunning) {
            timeElapsed = SystemClock.elapsedRealtime() - chronometerBase
            isRunning = false
            updateProgress()
        }
        super.dailyUpdate(date)
    }

    private fun getTimeElapsed(): Long {
        if (isRunning) {
            return SystemClock.elapsedRealtime() - chronometerBase
        }
        return timeElapsed
    }

    private fun getIntervalTime(): Long {
        return when {
            goalDuration <= oneMinute -> {
                oneSecond
            }
            goalDuration <= 3 * oneMinute -> {
                5 * oneSecond
            }
            goalDuration <= 5 * oneMinute -> {
                10 * oneSecond
            }
            goalDuration <= 10 * oneMinute -> {
                30 * oneSecond
            }
            goalDuration <= oneHour -> {
                oneMinute
            }
            goalDuration <= 3 * oneHour -> {
                5 * oneMinute
            }
            goalDuration <= 10 * oneHour -> {
                30 * oneMinute
            }
            else -> {
                oneHour
            }
        }
    }

    fun getChronometerBase(): Long {
        isRunning = false
        return SystemClock.elapsedRealtime() - timeElapsed
    }

    fun getGoal(): Pair<String, String> {
        val hour = goalDuration / TimeUtil.oneHour
        val minute = goalDuration % TimeUtil.oneHour / TimeUtil.oneMinute
        return Pair(hour.toString(), minute.toString())
    }

}