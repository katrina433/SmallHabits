package com.keepsimple.smallhabits

import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import java.util.*
import kotlin.collections.HashMap

class CheckOffTask (
    override val id: Int,
    override var taskTitle: String,
    override var time: Times,
    override var repeatDays: HashMap<Weekdays, Boolean>
) : Task {
    override var active = false
    override var streak = 0
    override var longestStreak = 0
    override var history = ArrayList<Pair<Long, TaskHistoryLog>>()
    override var archived = false
    var completed = false

    override fun clickOnTask(
        button: Button,
        progressBar: ProgressBar,
        progressLayout: LinearLayout,
        completedBackground: Drawable
    ) {
        if(!completed) {
            completed = true
            button.background = completedBackground
            progressBar.progress = getProgressPercent()
            updateProgress()
        }
    }

    override fun completed(): Boolean {
        return completed
    }

    override fun getProgressCount(): Int {
        return if (completed) {
            1
        } else 0
    }

    override fun getProgressPercent(): Int {
        if (completed) {
            return 100
        }
        return 0
    }

    override fun getProgressString(): String {
        if (completed) {
            return "completed"
        }
        return "incomplete"
    }

    override fun getProgressInterval(): Int {
        return 1
    }

    override fun setProgress(progress: Int) {
        completed = progress != 0
        updateProgress()
    }
}