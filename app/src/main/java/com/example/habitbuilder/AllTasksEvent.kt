package com.example.habitbuilder

import com.github.sundeepk.compactcalendarview.domain.Event

/**
 * The calendar event recording daily completion of all the tasks.
 */
class AllTasksEvent(markColor: Int, date: Long, val log: AllTasksHistoryLog) :
    Event(markColor, date) {

    /**
     * @return the percent of the daily completion progress.
     */
    fun getProgressPercent(): Int {
        return log.progressPercent
    }

    /**
     * @return the text showing number of completed tasks / total active tasks.
     */
    fun getProgressString(): String {
        return log.progressString
    }

    /**
     * @return the title and the completion progress of all the active tasks.
     */
    fun getTaskList(): String {
        return log.taskList
    }
}