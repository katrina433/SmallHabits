package com.keepsimple.smallhabits

import com.github.sundeepk.compactcalendarview.domain.Event

class AllTasksEvent(markColor: Int, date: Long, val log: AllTasksHistoryLog) :
    Event(markColor, date) {

    fun getProgressPercent(): Int {
        return log.progressPercent
    }

    fun getProgressString(): String {
        return log.progressString
    }

    fun getTaskList(): String {
        return log.taskList
    }
}