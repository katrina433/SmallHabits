package com.example.smallhabits

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.task_detail.*
import java.lang.Exception
import java.util.*
import kotlin.math.max

class TaskDetail: AppCompatActivity() {

    private var taskId: Int? = null
    private var taskType: String? = null
    private lateinit var task: Task
    private var resultCode = Activity.RESULT_CANCELED
    private var resultIntent = Intent()
    private var clickedDate = System.currentTimeMillis()

    private val taskNotFoundException =
        Resources.NotFoundException("failed to retrieve task")


    override fun onCreate(savedInstanceState: Bundle?) {
        val taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(R.string.theme_key), "BrownTheme")
        val themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_detail)

        taskId = intent.getIntExtra(resources.getString(R.string.task_id), 0)
        taskType = intent.getStringExtra(resources.getString(R.string.task_type))

        resultIntent.putExtra(resources.getString(R.string.task_id), taskId!!)
        resultIntent.putExtra(resources.getString(R.string.task_type), taskType)

        task = getTask()
        setUpTaskInfo()
        setUpTodaysProgress()
        setUpStreak()
        setUpCalendar()
        setUpMemo()

        displayCharts.setOnClickListener {
            val intent = Intent(this, Charts::class.java)
            intent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.individual_task_graph_request_code))
            intent.putExtra(resources.getString(R.string.task_id), taskId!!)
            intent.putExtra(resources.getString(R.string.task_type), taskType)
            startActivity(intent)
        }

        editTask.setOnClickListener {
            val editTaskIntent = Intent(this, AddTask::class.java)
            editTaskIntent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.edit_task_request_code))
            editTaskIntent.putExtra(resources.getString(R.string.task_id), task.id)
            editTaskIntent.putExtra(resources.getString(R.string.task_type), taskType)
            startActivityForResult(editTaskIntent, resources.getInteger(R.integer.edit_task_request_code))
        }

        archiveTask.isChecked = task.archived
        archiveTask.setOnCheckedChangeListener { _, isChecked ->
            task.archived = isChecked
            if (!TimeUtil.sameDay(task.history[task.history.size - 1].first, System.currentTimeMillis())) {
                task.updateHistory(System.currentTimeMillis())
            }
            val taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
            taskDB.edit().apply {
                putString(taskType + taskId, Gson().toJson(task))
            }.apply()
            resultCode = resources.getInteger(R.integer.task_archived_result_code)
            setResult(resultCode, resultIntent)
        }

        deleteTask.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle(R.string.delete_task)
            dialogBuilder.setMessage(R.string.delete_box_message)
            dialogBuilder.setPositiveButton(R.string.yes) { dialog, _ ->
                val taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
                taskDB.edit().apply {
                    remove(taskType + taskId)
                }.apply()
                resultCode = resources.getInteger(R.integer.task_deleted_result_code)
                setResult(resultCode, resultIntent)
                dialog.dismiss()
                finish()
            }
            dialogBuilder.setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = dialogBuilder.create()
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.edit_task_request_code) && resultCode == resources.getInteger(R.integer.task_edited_result_code)) {
            task = getTask()
            setUpTaskInfo()
            setUpTodaysProgress()
            setUpStreak()
            calendarDateProgressString.text = getProgressString(clickedDate)
            displayWeekProgress(clickedDate)
            this.resultCode = resources.getInteger(R.integer.task_edited_result_code)
            setResult(this.resultCode, resultIntent)
        } else if (requestCode == resources.getInteger(R.integer.add_memo_request_code)) {
            when (resultCode) {
                resources.getInteger(R.integer.memo_added_result_code) -> {
                    val memoString = data!!.getStringExtra(resources.getString(R.string.memo_text))
                    val memoImage = data.getStringExtra(resources.getString(R.string.memo_image))
                    val events = detailPageCalendar.getEvents(clickedDate)
                    val log = (events[0] as TaskEvent).log
                    log.memoString = memoString!!
                    log.memoImage = memoImage!!
                }
                resources.getInteger(R.integer.memo_deleted_result_code) -> {
                    val events = detailPageCalendar.getEvents(clickedDate)
                    val log = (events[0] as TaskEvent).log
                    log.memoString = resources.getString(R.string.empty_string)
                    log.memoImage = resources.getString(R.string.empty_string)
                }
            }
            displayMemo(clickedDate)
            val taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
            taskDB.edit().apply {
                putString(taskType + taskId, Gson().toJson(task))
            }.apply()
        }
    }

    private fun getTask(): Task {
        val gson = Gson()
        val taskDB = getSharedPreferences(
            resources.getString(R.string.task_database), Context.MODE_PRIVATE)

        val json = taskDB.getString(taskType + taskId,
            resources.getString(R.string.failed_to_retrieve_task))
        return when (taskType) {
            resources.getString(R.string.check_off_type) -> {
                gson.fromJson(json, CheckOffTask::class.java)
            }
            resources.getString(R.string.counter_type) -> {
                gson.fromJson(json, CounterTask::class.java)
            }
            resources.getString(R.string.timer_type) -> {
                gson.fromJson(json, TimerTask::class.java)
            }
            resources.getString(R.string.chronometer_type) -> {
                gson.fromJson(json, ChronometerTask::class.java)
            }
            else -> throw taskNotFoundException
        }
    }

    private fun setUpTaskInfo() {
        detailPageTaskTitle.text = task.taskTitle
        detailPageSun.isChecked = task.repeatDays[Weekdays.SUN]!!
        detailPageMon.isChecked = task.repeatDays[Weekdays.MON]!!
        detailPageTue.isChecked = task.repeatDays[Weekdays.TUE]!!
        detailPageWed.isChecked = task.repeatDays[Weekdays.WED]!!
        detailPageThu.isChecked = task.repeatDays[Weekdays.THU]!!
        detailPageFri.isChecked = task.repeatDays[Weekdays.FRI]!!
        detailPageSat.isChecked = task.repeatDays[Weekdays.SAT]!!
    }

    private fun setUpTodaysProgress(){
        if (task.archived || !task.active) {
            detailPageTodaysProgressLayout.visibility = View.GONE
        } else {
            detailPageTodaysProgressLayout.visibility = View.VISIBLE
            todaysProgressString.text = task.getProgressString()
            if (task is TimerTask) {
                todaysProgressBar.visibility = View.GONE
                todaysTimerProgressBar.visibility = View.VISIBLE
                todaysTimerProgressBar.progress = task.getProgressPercent()
            } else {
                todaysProgressBar.max = task.getProgressInterval()
                todaysProgressBar.progress = task.getProgressCount()
                todaysProgressBar.setOnSeekBarChangeListener(
                    object : OnSeekBarChangeListener {
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            setUpStreak()
                            calendarDateProgressString.text = getProgressString(clickedDate)
                            displayWeekProgress(clickedDate)
                            val taskDB = getSharedPreferences(
                                resources.getString(R.string.task_database), Context.MODE_PRIVATE)
                            taskDB.edit().apply {
                                val json = Gson().toJson(task)
                                putString(taskType + task.id, json)
                            }.apply()
                            resultCode = resources.getInteger(R.integer.task_edited_result_code)
                            setResult(resultCode, resultIntent)
                        }

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            task.setProgress(progress)
                            todaysProgressString.text = task.getProgressString()
                        }
                    }
                )
            }
        }
    }

    private fun setUpStreak() {
        val todaysStreak = if (task.completed()) {
            task.streak + 1
        } else {
            task.streak
        }
        currentStreakString.text = todaysStreak.toString()
        longestStreakString.text = (max(todaysStreak, task.longestStreak)).toString()
    }

    private fun setUpCalendar() {
        detailPageCalendar.removeAllEvents()
        detailPageCalendar.setFirstDayOfWeek(1)
        detailPageCalendar.setUseThreeLetterAbbreviation(true)
        for (entry in task.history) {
            val date = entry.first
            val log = entry.second
            when (log.progressPercent) {
                100 -> {
                    detailPageCalendar.addEvent(
                        TaskEvent(Color.GREEN, date, log))
                }
                0 -> {
                    detailPageCalendar.addEvent(
                        TaskEvent(Color.TRANSPARENT, date, log))
                }
                else -> {
                    detailPageCalendar.addEvent(
                        TaskEvent(Color.BLUE, date, log))
                }
            }
        }
        calendarDate.text = TimeUtil.dateFormat.format(System.currentTimeMillis())
        calendarDateProgressString.text = task.getProgressString()
        displayWeekProgress(System.currentTimeMillis())
        setUpCalendarListener()
    }

    private fun setUpCalendarListener() {
        detailPageCalendar.setListener(object: CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date?) {
                displaySelectedDateInfo(dateClicked!!.time)
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                displaySelectedDateInfo(firstDayOfNewMonth!!.time)
            }
        })
    }

    private fun displaySelectedDateInfo(date: Long) {
        clickedDate = date
        calendarDate.text = TimeUtil.dateFormat.format(date)
        displayWeekProgress(date)
        calendarDateProgressString.text = getProgressString(date)
        displayMemo(date)
    }

    private fun displayWeekProgress(date: Long) {
        when (TimeUtil.dayOfWeekFormat.format(date)) {
            "Sunday" -> {
                detailPageSunBar.progress = getProgressPercent(date)
                detailPageMonBar.progress = getProgressPercent(date + TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date + 2 * TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date + 3 * TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date + 4 * TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date + 5 * TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date + 6 * TimeUtil.oneDay)
            }
            "Monday" -> {
                detailPageSunBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date)
                detailPageTueBar.progress = getProgressPercent(date + TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date + 2 * TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date + 3 * TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date + 4 * TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date + 5 * TimeUtil.oneDay)
            }
            "Tuesday" -> {
                detailPageSunBar.progress = getProgressPercent(date - 2 *TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date)
                detailPageWedBar.progress = getProgressPercent(date + TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date + 2 * TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date + 3 * TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date + 4 * TimeUtil.oneDay)
            }
            "Wednesday" -> {
                detailPageSunBar.progress = getProgressPercent(date - 3 * TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date - 2 * TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date)
                detailPageThuBar.progress = getProgressPercent(date + TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date + 2 * TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date + 3 * TimeUtil.oneDay)
            }
            "Thursday" -> {
                detailPageSunBar.progress = getProgressPercent(date - 4 * TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date - 3 * TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date - 2 * TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date)
                detailPageFriBar.progress = getProgressPercent(date + TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date + 2 * TimeUtil.oneDay)
            }
            "Friday" -> {
                detailPageSunBar.progress = getProgressPercent(date - 5 * TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date - 4 * TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date - 3 * TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date - 2 * TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date)
                detailPageSatBar.progress = getProgressPercent(date + TimeUtil.oneDay)
            }
            "Saturday" -> {
                detailPageSunBar.progress = getProgressPercent(date - 6 * TimeUtil.oneDay)
                detailPageMonBar.progress = getProgressPercent(date - 5 * TimeUtil.oneDay)
                detailPageTueBar.progress = getProgressPercent(date - 4 * TimeUtil.oneDay)
                detailPageWedBar.progress = getProgressPercent(date - 3 * TimeUtil.oneDay)
                detailPageThuBar.progress = getProgressPercent(date - 2 * TimeUtil.oneDay)
                detailPageFriBar.progress = getProgressPercent(date - TimeUtil.oneDay)
                detailPageSatBar.progress = getProgressPercent(date)
            }
        }
    }

    private fun getProgressString(date: Long): String {
        val events = detailPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as TaskEvent).getProgressString()
            }
            else -> {
                resources.getString(R.string.empty_string)
            }
        }
    }

    private fun getProgressPercent(date: Long): Int {
        val events = detailPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as TaskEvent).getProgressPercent()
            }
            else -> {
                0
            }
        }
    }

    private fun getMemoString(date: Long): String? {
        val events = detailPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as TaskEvent).getMemoString()
            }
            else -> {
                resources.getString(R.string.empty_string)
            }
        }
    }

    private fun getImageUri(date: Long): String {
        val events = detailPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as TaskEvent).getMemoImage()
            }
            else -> {
                resources.getString(R.string.empty_string)
            }
        }
    }

    private fun setUpMemo() {
        addMemo.setOnClickListener {
            val memoString = getMemoString(clickedDate)
            val memoImage = getImageUri(clickedDate)
            if (memoString == resources.getString(R.string.empty_string) && memoImage == resources.getString(R.string.empty_string)) {
                val addMemoIntent = Intent(this, AddMemo::class.java)
                addMemoIntent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.add_memo_request_code))
                startActivityForResult(addMemoIntent, resources.getInteger(R.integer.add_memo_request_code))
            } else {
                val editMemoIntent = Intent(this, AddMemo::class.java)
                editMemoIntent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.edit_memo_request_code))
                editMemoIntent.putExtra(resources.getString(R.string.memo_text), memoString)
                editMemoIntent.putExtra(resources.getString(R.string.memo_image), memoImage)
                startActivityForResult(editMemoIntent, resources.getInteger(R.integer.add_memo_request_code))
            }
        }
        displayMemo(clickedDate)
    }

    private fun displayMemo(date: Long) {
        if (detailPageCalendar.getEvents(date).size != 0) {
            memoTitle.visibility = View.VISIBLE
            addMemo.visibility = View.VISIBLE

            val memoString = getMemoString(date)
            val memoImageString = getImageUri(date)
            val memoImageUri = Uri.parse(memoImageString)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(memoImageUri, takeFlags)
            } catch (e: Exception){}
            memoLayout.visibility = View.GONE
            if (memoString == resources.getString(R.string.empty_string) && memoImageString != resources.getString(R.string.empty_string)) {
                memoLayout.visibility = View.VISIBLE
                detailPageMemoImage.visibility = View.VISIBLE
                detailPageMemoText.visibility = View.GONE
                try {
                    detailPageMemoImage.setImageURI(memoImageUri)
                } catch (e: Exception) {
                    memoLayout.visibility = View.GONE
                }
            } else if (memoString != resources.getString(R.string.empty_string) && memoImageString == resources.getString(R.string.empty_string)) {
                memoLayout.visibility = View.VISIBLE
                detailPageMemoImage.visibility = View.GONE
                detailPageMemoText.visibility = View.VISIBLE
                detailPageMemoText.text = memoString
            } else if (memoString != resources.getString(R.string.empty_string) && memoImageString != resources.getString(R.string.empty_string)){
                memoLayout.visibility = View.VISIBLE
                detailPageMemoImage.visibility = View.VISIBLE
                detailPageMemoText.visibility = View.VISIBLE
                detailPageMemoText.text = memoString
                try {
                    detailPageMemoImage.setImageURI(memoImageUri)
                } catch (e: Exception) {
                    detailPageMemoImage.visibility = View.GONE
                }
            }
        } else {
            memoTitle.visibility = View.GONE
            addMemo.visibility = View.GONE
        }
    }

}