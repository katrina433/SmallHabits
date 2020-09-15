package com.example.habitbuilder

/**
 * Records the daily completion of all the tasks.
 */
class AllTasksHistoryLog(
    var progressPercent: Int = 0,
    var progressString: String = "",
    var taskList: String = ""
) {
}