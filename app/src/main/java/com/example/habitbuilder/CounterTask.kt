package com.example.habitbuilder

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log.d
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.children
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

class CounterTask (
    override val id: Int,
    override var taskTitle: String,
    override var time: Times,
    override var repeatDays: HashMap<Weekdays, Boolean>,
    var goalCount: Int
) : Task{

    override var active = false
    override var streak = 0
    override var longestStreak = 0
    override var history = ArrayList<Pair<Long, TaskHistoryLog>>()
    override var archived = false
    var counter = 0

    override fun clickOnTask(
        button: Button,
        progressBar: ProgressBar,
        progressLayout: LinearLayout,
        completedBackground: Drawable
    ) {
        counter += 1
        progressBar.progress = getProgressPercent()
        val progressText = progressLayout.children.elementAt(0) as TextView
        progressText.text = getProgressString()
        if (completed()) {
            button.background = completedBackground
        }
        updateProgress()
    }

    override fun completed(): Boolean {
        return counter >= goalCount
    }

    override fun getProgressCount(): Int {
        return counter
    }

    override fun getProgressPercent(): Int {
        return min(100, 100 * counter / goalCount)
    }

    override fun getProgressString(): String {
        if (!active) {
            return ""
        }
        return "$counter/$goalCount"
    }

    override fun getProgressInterval(): Int {
        return goalCount
    }

    override fun setProgress(progress: Int) {
        counter = progress
        updateProgress()
    }

    fun getGoal(): String {
        return goalCount.toString()
    }
}