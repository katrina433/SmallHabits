package com.example.habitbuilder

import android.graphics.Color
import android.net.Uri
import com.github.sundeepk.compactcalendarview.domain.Event

class TaskEvent(markColor: Int, date: Long, val log: TaskHistoryLog) :
    Event(markColor, date) {

    fun getProgressPercent(): Int {
        return log.progressPercent
    }

    fun getProgressString(): String {
        return log.progressString
    }

    fun getMemoString(): String {
        return log.memoString
    }

    fun getMemoImage(): String {
        return log.memoImage
    }
}