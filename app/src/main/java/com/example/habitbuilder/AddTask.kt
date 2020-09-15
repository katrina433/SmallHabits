package com.example.habitbuilder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.habitbuilder.R.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.add_task.*
import java.lang.Exception
import kotlin.collections.HashMap

/**
 * The activity to add/edit a task.
 * User inputs: title, time, weekly repeats, type, and daily goal for certain types.
 */
class AddTask: AppCompatActivity() {

    private val resultIntent = Intent()
    private var resultCode = Activity.RESULT_CANCELED
    private lateinit var gson: Gson
    private lateinit var taskDB: SharedPreferences

    private lateinit var taskNotFoundException: Exception
    private lateinit var taskTypeNotFoundException: Exception
    private lateinit var taskTitleNotFoundException: Exception
    private lateinit var taskTimeNotFoundException: Exception
    private lateinit var repeatDayNotFoundException: Exception
    private lateinit var counterGoalException: Exception
    private lateinit var timerGoalException: Exception
    private lateinit var cycleGoalException: Exception

    private var taskType: String? = null

    /**
     * Create the activity for adding/editing a task.
     * Save the task information to the device shared preferences
     * when the confirm button is clicked.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val taskManagementDB = getSharedPreferences(
            resources.getString(string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(string.theme_key), "BrownTheme")
        val themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(layout.add_task)
        
        initVariables()

        val request = intent.getIntExtra(resources.getString(string.request), resources.getInteger(integer.add_task_request_code))
        if (request == resources.getInteger(integer.add_task_request_code)) {
            addTaskDisplay()
        } else if (request == resources.getInteger(integer.edit_task_request_code)) {
            editTaskDisplay()
        }

        confirm.setOnClickListener {
            try {
                if (request == resources.getInteger(integer.add_task_request_code)) {
                    val id = getId()
                    val taskTitle = getTaskTitle()
                    val taskTime = getTaskTime()
                    val repeatDays = getRepeatDays()
                    val type = getType()
                    val task = when (type) {
                        resources.getString(string.check_off_type) -> {
                            CheckOffTask(id, taskTitle, taskTime, repeatDays)
                        }
                        resources.getString(string.counter_type) -> {
                            CounterTask(id, taskTitle, taskTime, repeatDays, getCounterGoal())
                        }
                        resources.getString(string.timer_type) -> {
                            TimerTask(id, taskTitle, taskTime, repeatDays, getTimerGoal(), getCycleGoal())
                        }
                        resources.getString(string.chronometer_type) -> {
                            ChronometerTask(id, taskTitle, taskTime, repeatDays, getTimerGoal())
                        }
                        else -> {
                            throw taskTypeNotFoundException
                        }
                    }
                    task.updateActiveness(System.currentTimeMillis())
                    task.updateHistory(System.currentTimeMillis())

                    taskDB.edit().apply {
                        val json = gson.toJson(task)
                        putString(type + task.id, json)
                    }.apply()

                    taskManagementDB.edit().apply {
                        putInt(resources.getString(string.task_id), task.id)
                    }.apply()

                    resultCode = resources.getInteger(integer.task_added_result_code)
                    resultIntent.putExtra(resources.getString(string.task_id), task.id)
                    resultIntent.putExtra(resources.getString(string.task_type), type)
                    setResult(resultCode, resultIntent)
                    finish()

                } else if (request == resources.getInteger(integer.edit_task_request_code)) {
                    val task = getTask()
                    val type = getType()
                    task.taskTitle = getTaskTitle()
                    task.time = getTaskTime()
                    task.repeatDays = getRepeatDays()
                    when (task) {
                        is CounterTask -> {
                            task.goalCount = getCounterGoal()
                        }
                        is TimerTask -> {
                            task.goalDuration = getTimerGoal()
                            task.goalSessions = getCycleGoal()
                        }
                        is ChronometerTask -> {
                            task.goalDuration = getTimerGoal()
                        }
                    }
                    val originalActiveness = task.active
                    task.updateActiveness(System.currentTimeMillis())
                    if (!originalActiveness) {
                        task.updateHistory(System.currentTimeMillis())
                    }
                    taskDB.edit().apply {
                        val json = gson.toJson(task)
                        putString(type + task.id, json)
                    }.apply()

                    resultCode = resources.getInteger(integer.task_edited_result_code)
                    resultIntent.putExtra(resources.getString(string.task_id), task.id)
                    resultIntent.putExtra(resources.getString(string.task_type), type)
                    setResult(resultCode, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Initiate variables and exceptions.
     */
    private fun initVariables() {
        gson = Gson()
        taskDB = getSharedPreferences(resources.getString(string.task_database), Context.MODE_PRIVATE)
        taskNotFoundException = 
            Exception(resources.getString(string.failed_to_retrieve_task))
        taskTypeNotFoundException =
            Exception(resources.getString(string.task_type_not_found_exception))
        taskTitleNotFoundException =
            Exception(resources.getString(string.task_title_not_found_exception))
        taskTimeNotFoundException =
            Exception(resources.getString(string.task_time_not_found_exception))
        repeatDayNotFoundException =
            Exception(resources.getString(string.repeat_day_not_found_exception))
        counterGoalException =
            Exception(resources.getString(string.counter_goal_exception))
        timerGoalException =
            Exception(resources.getString(string.timer_goal_exception))
        cycleGoalException =
            Exception(resources.getString(string.cycle_goal_exception))
    }

    /**
     * Display the layout for adding a new task.
     * Show or remove corresponding daily goal when a specific task type is chosen.
     */
    private fun addTaskDisplay() {
        taskTypeInput.setOnCheckedChangeListener { _, checkedId ->
            val button = findViewById<RadioButton>(checkedId)
            when (button.text.toString()) {
                resources.getString(string.check_off) -> {
                    taskType = resources.getString(string.check_off_type)
                    dailyGoal.visibility = View.GONE
                    dailyGoalNumInput.visibility = View.GONE
                    dailyGoalTimeLayout.visibility = View.GONE
                }
                resources.getString(string.counter) -> {
                    taskType = resources.getString(string.counter_type)
                    dailyGoal.visibility = View.VISIBLE
                    dailyGoalNumInput.visibility = View.VISIBLE
                    dailyGoalTimeLayout.visibility = View.GONE
                    dailyGoalNumInput.setText("1")
                }
                resources.getString(string.timer) -> {
                    taskType = resources.getString(string.timer_type)
                    dailyGoal.visibility = View.VISIBLE
                    dailyGoalNumInput.visibility = View.GONE
                    dailyGoalTimeLayout.visibility = View.VISIBLE
                    cycles.visibility = View.VISIBLE
                    dailyGoalCyclesInput.visibility = View.VISIBLE
                    dailyGoalHourInput.setText("0")
                    dailyGoalMinInput.setText("25")
                    dailyGoalCyclesInput.setText("4")
                }
                resources.getString(string.chronometer) -> {
                    taskType = resources.getString(string.chronometer_type)
                    dailyGoal.visibility = View.VISIBLE
                    dailyGoalNumInput.visibility = View.GONE
                    dailyGoalTimeLayout.visibility = View.VISIBLE
                    cycles.visibility = View.GONE
                    dailyGoalCyclesInput.visibility = View.GONE
                    dailyGoalHourInput.setText("0")
                    dailyGoalMinInput.setText("30")
                }
            }
        }
    }

    /**
     * Display the layout for editing an existing task.
     * Show the current task information.
     */
    private fun editTaskDisplay() {
        taskTypeText.visibility = View.GONE
        taskTypeInput.visibility = View.GONE

        val task = getTask()
        taskTitleInput.setText(task.taskTitle)

        val taskTime = when(task.time) {
            Times.ALL_DAY -> {
                0
            }
            Times.MORNING -> {
                1
            }
            Times.AFTERNOON -> {
                2
            }
            Times.EVENING -> {
                3
            }
        }
        taskTimeInput.setSelection(taskTime)

        sun.isChecked = task.repeatDays[Weekdays.SUN]!!
        mon.isChecked = task.repeatDays[Weekdays.MON]!!
        tue.isChecked = task.repeatDays[Weekdays.TUE]!!
        wed.isChecked = task.repeatDays[Weekdays.WED]!!
        thu.isChecked = task.repeatDays[Weekdays.THU]!!
        fri.isChecked = task.repeatDays[Weekdays.FRI]!!
        sat.isChecked = task.repeatDays[Weekdays.SAT]!!

        when (task) {
            is CounterTask -> {
                dailyGoal.visibility = View.VISIBLE
                dailyGoalNumInput.visibility = View.VISIBLE
                dailyGoalNumInput.setText(task.getGoal())
            }
            is TimerTask -> {
                dailyGoal.visibility = View.VISIBLE
                dailyGoalTimeLayout.visibility = View.VISIBLE
                cycles.visibility = View.VISIBLE
                dailyGoalCyclesInput.visibility = View.VISIBLE
                val goal = task.getGoal()
                dailyGoalHourInput.setText(goal[0])
                dailyGoalMinInput.setText(goal[1])
                dailyGoalCyclesInput.setText(goal[2])
            }
            is ChronometerTask -> {
                dailyGoal.visibility = View.VISIBLE
                dailyGoalTimeLayout.visibility = View.VISIBLE
                dailyGoalHourInput.setText(task.getGoal().first)
                dailyGoalMinInput.setText(task.getGoal().second)
            }
        }
    }

    /**
     * @return the task to be edited from shared preferences.
     */
    private fun getTask(): Task {
        taskType = intent.getStringExtra(resources.getString(string.task_type))
        val taskId = intent.getIntExtra(resources.getString(string.task_id), -1)
        val json = taskDB.getString(taskType + taskId, resources.getString(string.failed_to_retrieve_task))
        return when (taskType) {
            resources.getString(string.check_off_type) -> {
                gson.fromJson(json, CheckOffTask::class.java)
            }
            resources.getString(string.counter_type) -> {
                gson.fromJson(json, CounterTask::class.java)
            }
            resources.getString(string.timer_type) -> {
                gson.fromJson(json, TimerTask::class.java)
            }
            resources.getString(string.chronometer_type) -> {
                gson.fromJson(json, ChronometerTask::class.java)
            }
            else -> throw taskNotFoundException
        }
    }

    /**
     * @return the task type.
     * @throws taskTypeNotFoundException if the user has not chosen a task type.
     */
    private fun getType(): String {
        if (taskType == null) {
            throw taskTypeNotFoundException
        }
        return taskType as String
    }

    /**
     * @return the id of the new task.
     */
    private fun getId(): Int {
        return getSharedPreferences(
            resources.getString(string.task_management_database),
            Context.MODE_PRIVATE).getInt(
            resources.getString(string.task_id), 0) + 1
    }

    /**
     * @return the task title.
     * @throws taskTitleNotFoundException if the user leaves the title blank.
     */
    private fun getTaskTitle(): String {
        val taskTitle = taskTitleInput.text.toString()
        if (taskTitle.isBlank()) {
            throw taskTitleNotFoundException
        }
        return taskTitle
    }

    /**
     * @return the time of the task.
     * @throws taskTimeNotFoundException if the user has not chosen a time for the task.
     */
    private fun getTaskTime(): Times {
        val timeString = taskTimeInput.selectedItem.toString()
        var taskTime: Times? = null
        when (timeString) {
            resources.getString(string.all_day) -> {
                taskTime = Times.ALL_DAY
            }
            resources.getString(string.morning) -> {
                taskTime = Times.MORNING
            }
            resources.getString(string.afternoon) -> {
                taskTime = Times.AFTERNOON
            }
            resources.getString(string.evening) -> {
                taskTime = Times.EVENING
            }
        }
        if (taskTime == null) {
            throw taskTimeNotFoundException
        }
        return taskTime
    }

    /**
     * @return the weekly repeat of the task.
     * @throws repeatDayNotFoundException if the user did not select any days to be repeated.
     */
    private fun getRepeatDays(): HashMap<Weekdays, Boolean> {
        val repeatDays = HashMap<Weekdays, Boolean>()
        var noRepeats = true
        for (weekday in Weekdays.values()){
            repeatDays[weekday] = false
        }
        if (sun.isChecked) {
            repeatDays[Weekdays.SUN] = true
            noRepeats = false
        }
        if (mon.isChecked) {
            repeatDays[Weekdays.MON] = true
            noRepeats = false
        }
        if (tue.isChecked) {
            repeatDays[Weekdays.TUE] = true
            noRepeats = false
        }
        if (wed.isChecked) {
            repeatDays[Weekdays.WED] = true
            noRepeats = false
        }
        if (thu.isChecked) {
            repeatDays[Weekdays.THU] = true
            noRepeats = false
        }
        if (fri.isChecked) {
            repeatDays[Weekdays.FRI] = true
            noRepeats = false
        }
        if (sat.isChecked) {
            repeatDays[Weekdays.SAT] = true
            noRepeats = false
        }
        if (noRepeats) {
            throw repeatDayNotFoundException
        }
        return repeatDays
    }

    /**
     * @return the daily goal for a counter task.
     * @throws counterGoalException the goal is not a positive integer.
     */
    private fun getCounterGoal(): Int {
        try {
            val counterGoal = dailyGoalNumInput.text.toString().toInt()
            if (counterGoal <= 0) {
                throw counterGoalException
            }
            return counterGoal
        } catch (e: Exception) {
            throw counterGoalException
        }
    }

    /**
     * @return the duration goal for a Pomodoro timer or chronometer task.
     * @throws timerGoalException if the duration is not between 0 minute and 23h59m.
     */
    private fun getTimerGoal(): Long {
        try {
            val hour = dailyGoalHourInput.text.toString().toLong()
            val min = dailyGoalMinInput.text.toString().toLong()
            val duration = hour * TimeUtil.oneHour + min * TimeUtil.oneMinute
            if (duration > 0 && duration < (24L * 3600000L)) {
                return duration
            }
            throw timerGoalException
        } catch (e: Exception) {
            throw timerGoalException
        }
    }

    /**
     * @return the goal number of cycles for a Pomodoro task.
     * @throws cycleGoalException if the goal is not a positive integer.
     */
    private fun getCycleGoal(): Int {
        try {
            val cycles = dailyGoalCyclesInput.text.toString().toInt()
            if (cycles <= 0) {
                throw cycleGoalException
            }
            return cycles
        } catch (e: Exception) {
            throw cycleGoalException
        }
    }
}