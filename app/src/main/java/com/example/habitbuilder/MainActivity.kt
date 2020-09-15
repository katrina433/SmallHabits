package com.example.habitbuilder

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log.d
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.common.collect.HashBiMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*

import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.home_page.*
import kotlinx.android.synthetic.main.progress_history_page.*
import kotlinx.android.synthetic.main.settings_page.*
import kotlinx.android.synthetic.main.task_list_page.*
import java.lang.Exception
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private var totalActiveTask = 0
    private var completedTask = 0
    private var streak = 0
    private var longestStreak = 0
    private var credit = 0
    private lateinit var taskList: ArrayList<Pair<Task, Pair<RelativeLayout, RelativeLayout>>>
    private lateinit var history: ArrayList<Pair<Long, AllTasksHistoryLog>>
    private lateinit var rewards: ArrayList<Pair<String, Int>>
    private var rewardIndex = -1
    private lateinit var taskDB: SharedPreferences
    private lateinit var taskManagementDB: SharedPreferences
    private lateinit var gson: Gson
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var clickedDate = System.currentTimeMillis()
    private var themeId = 0
    private var colorPrimary = 0
    private var colorAccent = 0
    private var textColorSecondary = 0

    private val timerIntentReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val taskId = intent!!.getIntExtra(resources.getString(R.string.task_id), -1)
            val pair = createTask(taskId, resources.getString(R.string.timer_type))
            for (i in 0 until taskList.size) {
                if (taskList[i].first.id == taskId) {
                    taskList[i] = pair
                    break
                }
            }
            checkProgress()
            update()
            display()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(R.string.theme_key), "BrownTheme")
        themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)
        getThemeColors()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            timerIntentReceiver, IntentFilter("com.example.habitbuilder.POMODORO_TIMER"))

        initVariables()
        setUpListeners()
        update()
        refresh()
        display()
        checkProgress()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            resources.getInteger(R.integer.add_task_request_code) -> {
                if (resultCode == resources.getInteger(R.integer.task_added_result_code) && data != null) {
                    val taskId = data.getIntExtra(
                        resources.getString(R.string.task_id), -1)
                    val taskType = data.getStringExtra(
                        resources.getString(R.string.task_type))
                    val pair = createTask(taskId, taskType!!)
                    taskList.add(pair)
                }
                sortTaskList()
            }
            resources.getInteger(R.integer.task_detail_page_request_code) -> {
                if ((resultCode ==  resources.getInteger(R.integer.task_edited_result_code) ||
                    resultCode == resources.getInteger(R.integer.task_archived_result_code)) &&
                    data != null) {
                    val taskId = data.getIntExtra(
                        resources.getString(R.string.task_id), -1)
                    val taskType = data.getStringExtra(
                        resources.getString(R.string.task_type))
                    val pair = createTask(taskId, taskType!!)
                    for (i in 0 until taskList.size) {
                        if (taskList[i].first.id == taskId) {
                            taskList[i] = pair
                            break
                        }
                    }
                    sortTaskList()
                }
                if (resultCode ==  resources.getInteger(R.integer.task_deleted_result_code) && data != null) {
                    val taskId = data.getIntExtra(
                        resources.getString(R.string.task_id), -1)
                    for (i in 0 until taskList.size) {
                        if (taskList[i].first.id == taskId) {
                            taskList.removeAt(i)
                            break
                        }
                    }
                }
            }
            resources.getInteger(R.integer.edit_rewards_request_code) -> {
                val rewardsJson = taskManagementDB.getString(
                    resources.getString(R.string.rewards_key),
                    resources.getString(R.string.failed_to_retrieve_rewards)
                )
                if (rewardsJson != resources.getString(R.string.failed_to_retrieve_rewards)) {
                    val type = object: TypeToken<ArrayList<Pair<String, Int>>>(){}.type
                    rewards = gson.fromJson(rewardsJson, type)
                }
            }
        }
        checkProgress()
        update()
        display()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerIntentReceiver)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dailyReminderChannel = NotificationChannel(
                resources.getString(R.string.daily_notification_channel_id),
                resources.getString(R.string.daily_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val alarmChannel = NotificationChannel(
                resources.getString(R.string.timer_notification_channel_id),
                resources.getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(dailyReminderChannel, alarmChannel))
        }
    }

    private fun getThemeColors() {
        val array = intArrayOf(
            R.attr.colorPrimary,
            R.attr.colorAccent,
            android.R.attr.textColorSecondary
        )
        @StyleableRes var index = 0
        val attr = obtainStyledAttributes(array)
        colorPrimary = attr.getColor(index++, 0)
        colorAccent = attr.getColor(index++, 0)
        textColorSecondary = attr.getColor(index, 0)
        attr.recycle()
    }

    private fun initVariables() {
        taskList = ArrayList()
        taskDB = getSharedPreferences(
            resources.getString(R.string.task_database), Context.MODE_PRIVATE)
        gson = Gson()
        streak = taskManagementDB.getInt(
            resources.getString(R.string.streak_key), 0)
        longestStreak = taskManagementDB.getInt(
            resources.getString(R.string.best_streak_key), 0)
        credit = taskManagementDB.getInt(
            resources.getString(R.string.credit_key), 0)
        val historyJson = taskManagementDB.getString(
            resources.getString(R.string.history_key),
            resources.getString(R.string.failed_to_retrieve_history))
        history = if (historyJson == resources.getString(R.string.failed_to_retrieve_history)) {
            ArrayList()
        } else {
            val type = object: TypeToken<ArrayList<Pair<Long, AllTasksHistoryLog>>>(){}.type
            gson.fromJson(historyJson, type)
        }
        val rewardsJson = taskManagementDB.getString(
            resources.getString(R.string.rewards_key),
            resources.getString(R.string.failed_to_retrieve_rewards)
        )
        rewards = if (rewardsJson == resources.getString(R.string.failed_to_retrieve_rewards)) {
            ArrayList()
        } else {
            val type = object: TypeToken<ArrayList<Pair<String, Int>>>(){}.type
            gson.fromJson(rewardsJson, type)
        }

        for (key in taskDB.all.keys) {
            val json = taskDB.getString(key, resources.getString(R.string.failed_to_retrieve_task))
            var task: Task? = null
            when {
                key.contains(resources.getString(R.string.check_off_type)) -> {
                    task = gson.fromJson(json, CheckOffTask::class.java)
                }
                key.contains(resources.getString(R.string.counter_type)) -> {
                    task = gson.fromJson(json, CounterTask::class.java)
                }
                key.contains(resources.getString(R.string.timer_type)) -> {
                    task = gson.fromJson(json, TimerTask::class.java)
                }
                key.contains(resources.getString(R.string.chronometer_type)) -> {
                    task = gson.fromJson(json, ChronometerTask::class.java)
                }
            }
            val layoutPair = createTaskLayout(task!!)
            taskList.add(Pair<Task, Pair<RelativeLayout, RelativeLayout>>(task, layoutPair))
        }
        sortTaskList()
    }

    private fun setUpListeners() {
        homeButton.setOnClickListener {
            mainPageViewFlipper.displayedChild = 0
            homeButton.setColorFilter(colorAccent)
            taskListButton.setColorFilter(colorPrimary)
            progressHistoryButton.setColorFilter(colorPrimary)
            settingsButton.setColorFilter(colorPrimary)
        }
        taskListButton.setOnClickListener {
            mainPageViewFlipper.displayedChild = 1
            homeButton.setColorFilter(colorPrimary)
            taskListButton.setColorFilter(colorAccent)
            progressHistoryButton.setColorFilter(colorPrimary)
            settingsButton.setColorFilter(colorPrimary)
        }
        progressHistoryButton.setOnClickListener {
            checkProgress()
            setUpHistoryPageProgress()
            mainPageViewFlipper.displayedChild = 2
            homeButton.setColorFilter(colorPrimary)
            taskListButton.setColorFilter(colorPrimary)
            progressHistoryButton.setColorFilter(colorAccent)
            settingsButton.setColorFilter(colorPrimary)
        }
        settingsButton.setOnClickListener {
            mainPageViewFlipper.displayedChild = 3
            homeButton.setColorFilter(colorPrimary)
            taskListButton.setColorFilter(colorPrimary)
            progressHistoryButton.setColorFilter(colorPrimary)
            settingsButton.setColorFilter(colorAccent)
        }
    }

    private fun display() {
        displayHomePage()
        displayTaskListPage()
        displayProgressHistoryPage()
        displaySettingsPage()
    }

    private fun displayHomePage() {
        addTask.setOnClickListener {
            val intent = Intent(this, AddTask::class.java)
            intent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.add_task_request_code))
            startActivityForResult(intent, resources.getInteger(R.integer.add_task_request_code))
        }

        allDay.visibility = View.GONE
        morningHeaderLayout.visibility = View.GONE
        afternoonHeaderLayout.visibility = View.GONE
        eveningHeaderLayout.visibility = View.GONE
        allDayTaskLayout.removeAllViews()
        morningTaskLayout.removeAllViews()
        afternoonTaskLayout.removeAllViews()
        eveningTaskLayout.removeAllViews()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.layout_padding)
        val columns = width / resources.getDimensionPixelSize(R.dimen.task_layout_width)
        morningTaskLayout.columnCount = columns
        afternoonTaskLayout.columnCount = columns
        eveningTaskLayout.columnCount = columns
        for (entry in taskList) {
            val task = entry.first
            if (!task.archived && task.active) {
                val layout = entry.second.first
                when (task.time) {
                    Times.ALL_DAY -> {
                        allDay.visibility = View.VISIBLE
                        allDayTaskLayout.addView(layout)
                    }
                    Times.MORNING -> {
                        morningHeaderLayout.visibility = View.VISIBLE
                        morningTaskLayout.addView(layout)
                    }
                    Times.AFTERNOON -> {
                        afternoonHeaderLayout.visibility = View.VISIBLE
                        afternoonTaskLayout.addView(layout)
                    }
                    Times.EVENING -> {
                        eveningHeaderLayout.visibility = View.VISIBLE
                        eveningTaskLayout.addView(layout)
                    }
                }
            }
        }
    }

    private fun displayTaskListPage() {
        activeTasksLayout.removeAllViews()
        archivedTasksLayout.removeAllViews()
        archivedTasksText.visibility = View.GONE
        for (entry in taskList) {
            val layout = entry.second.second
            if (entry.first.archived) {
                archivedTasksText.visibility = View.VISIBLE
                archivedTasksLayout.addView(layout)
            } else {
                activeTasksLayout.addView(layout)
            }
        }
        val curSortMethod = taskManagementDB.getString(
            resources.getString(R.string.sort_key),
            resources.getString(R.string.sort_by_alphabet)
        )
        if (curSortMethod == resources.getString(R.string.sort_by_id)) {
            sortButton.isChecked = true
        }
        sortButton.setOnCheckedChangeListener { _, isChecked ->
            val sortMethod = if (isChecked) {
                resources.getString(R.string.sort_by_id)
            } else {
                resources.getString(R.string.sort_by_alphabet)
            }
            taskManagementDB.edit().apply {
                putString(resources.getString(R.string.sort_key), sortMethod)
            }.apply()
            sortTaskList()
            display()
        }
    }

    private fun displayProgressHistoryPage() {
        allTasksDisplayCharts.setOnClickListener {
            val intent = Intent(this, Charts::class.java)
            intent.putExtra(resources.getString(R.string.request), resources.getInteger(R.integer.all_tasks_graph_request_code))
            startActivity(intent)
        }

        setUpHistoryPageProgress()
        setUpCalendar()
    }

    private fun displaySettingsPage() {
        setUpThemes()
        setUpNotification()
        setUpRewards()
        readPastMemo.setOnClickListener {
            startActivity(Intent(this, DisplayMemo::class.java))
        }
    }

    private fun createTask(taskId: Int, taskType: String): Pair<Task, Pair<RelativeLayout, RelativeLayout>> {
        val json = taskDB.getString(
            taskType + taskId, resources.getString(R.string.failed_to_retrieve_task))
        var task: Task? = null
        when (taskType) {
            resources.getString(R.string.check_off_type) -> {
                task = gson.fromJson(json, CheckOffTask::class.java)
            }
            resources.getString(R.string.counter_type) -> {
                task = gson.fromJson(json, CounterTask::class.java)
            }
            resources.getString(R.string.timer_type) -> {
                task = gson.fromJson(json, TimerTask::class.java)
            }
            resources.getString(R.string.chronometer_type) -> {
                task = gson.fromJson(json, ChronometerTask::class.java)
            }
        }
        val layoutPair = createTaskLayout(task!!)
        return Pair<Task, Pair<RelativeLayout, RelativeLayout>>(task, layoutPair)
    }

    private fun createTaskLayout(task: Task): Pair<RelativeLayout, RelativeLayout> {
        val homePageLayout = RelativeLayout(this)
        homePageLayout.layoutParams = RelativeLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.task_layout_width),
            resources.getDimensionPixelSize(R.dimen.task_layout_height)
        )
        homePageLayout.gravity = Gravity.CENTER

        val progressBar =
            ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.layoutParams = ActionBar.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.task_display_size),
            resources.getDimensionPixelSize(R.dimen.task_display_size)
        )
        progressBar.progressDrawable =
            ContextCompat.getDrawable(this, R.drawable.progress_bar)
        progressBar.elevation = resources.getDimension((R.dimen.progress_bar_elevation))
        progressBar.progress = task.getProgressPercent()

        val button = Button(this)
        button.width = resources.getDimensionPixelSize((R.dimen.task_display_size))
        button.height = resources.getDimensionPixelSize((R.dimen.task_display_size))
        button.setPadding(
            resources.getDimensionPixelSize(R.dimen.task_button_horizontal_padding),
            resources.getDimensionPixelSize(R.dimen.task_button_vertical_padding),
            resources.getDimensionPixelSize(R.dimen.task_button_horizontal_padding),
            resources.getDimensionPixelSize(R.dimen.task_button_vertical_padding)
        )
        button.text = task.taskTitle
        button.textSize = resources.getDimension(R.dimen.task_display_title)
        if (task.completed()) {
            button.background =
                ContextCompat.getDrawable(this, R.drawable.completed_activity)
        } else {
            button.background =
                ContextCompat.getDrawable(this, R.drawable.incomplete_activity)
        }

        val progressLayout = LinearLayout(this)
        progressLayout.orientation = LinearLayout.HORIZONTAL
        progressLayout.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.task_display_size),
            resources.getDimensionPixelSize(R.dimen.task_display_size)
        )
        progressLayout.setPadding(
            resources.getDimensionPixelSize(R.dimen.progress_layout_padding),
            resources.getDimensionPixelSize(R.dimen.progress_layout_padding),
            resources.getDimensionPixelSize(R.dimen.progress_layout_padding),
            resources.getDimensionPixelSize(R.dimen.progress_layout_padding)
        )
        progressLayout.gravity = Gravity.BOTTOM
        progressLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)
        progressLayout.setVerticalGravity(Gravity.BOTTOM)
        progressLayout.elevation = resources.getDimension(R.dimen.progress_layout_elevation)

        when (task) {
            is CheckOffTask -> {
                val checkView = ImageView(this)
                checkView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.check))
                checkView.setColorFilter(colorAccent)
                progressLayout.addView(checkView)
            }
            is CounterTask -> {
                val progressText = TextView(this)
                progressText.text = task.getProgressString()
                progressText.textSize = resources.getDimension(R.dimen.progress_text)
                progressText.setTextColor(textColorSecondary)
                progressLayout.addView(progressText)
            }
            is TimerTask -> {
                val progressText = TextView(this)
                progressText.text = task.getSessionString()
                progressText.textSize = resources.getDimension(R.dimen.progress_text)
                progressText.setTextColor(textColorSecondary)
                progressLayout.addView(progressText)
            }
            is ChronometerTask -> {
                val chronometer = Chronometer(this)
                chronometer.textSize = resources.getDimension(R.dimen.progress_text)
                chronometer.base = task.getChronometerBase()
                progressLayout.addView(chronometer)
            }
        }
        homePageLayout.addView(button)
        homePageLayout.addView(progressBar)
        homePageLayout.addView(progressLayout)

        val listPageLayout = RelativeLayout(this)
        listPageLayout.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.task_box_layout_height)
        )
        val listPageButton = Button(this)
        listPageButton.background = ContextCompat.getDrawable(this, R.drawable.task_box)
        listPageLayout.gravity = Gravity.CENTER
        listPageButton.text = task.taskTitle
        listPageLayout.addView(listPageButton)

        return Pair<RelativeLayout, RelativeLayout>(homePageLayout, listPageLayout)
    }

    private fun sortTaskList() {
        val sortMethod = taskManagementDB.getString(
            resources.getString(R.string.sort_key),
            resources.getString(R.string.sort_by_alphabet)
        )
        if (sortMethod == resources.getString(R.string.sort_by_id)) {
            Collections.sort(taskList,
                kotlin.Comparator { pairOne, pairTwo -> pairOne.first.id - pairTwo.first.id })
        } else {
            Collections.sort(taskList,
                kotlin.Comparator { pairOne, pairTwo ->
                    Collator.getInstance(Locale.CHINESE).compare(
                        pairOne.first.taskTitle,
                        pairTwo.first.taskTitle
                    )
                }
            )
        }
    }

    private fun update() {
        for (entry in taskList) {
            val task = entry.first
            var taskType: String? = null
            val homePageLayout = entry.second.first
            val homePageButton = homePageLayout.children.elementAt(0) as Button
            val progressBar = homePageLayout.children.elementAt(1) as ProgressBar
            val progressLayout = homePageLayout.children.elementAt(2) as LinearLayout
            when (task) {
                is CheckOffTask -> {
                    taskType = resources.getString(R.string.check_off_type)
                }
                is CounterTask -> {
                    taskType = resources.getString(R.string.counter_type)
                }
                is TimerTask -> {
                    taskType = resources.getString(R.string.timer_type)
                }
                is ChronometerTask -> {
                    taskType = resources.getString(R.string.chronometer_type)
                }
            }

            homePageButton.setOnClickListener {
                if (taskType == resources.getString(R.string.timer_type)) {
                    val intent = Intent(this, PomodoroTimer::class.java)
                    intent.putExtra(resources.getString(R.string.task_id), task.id)
                    startActivity(intent)
                } else {
                    task.clickOnTask(homePageButton, progressBar, progressLayout,
                        ContextCompat.getDrawable(this, R.drawable.completed_activity)!!)
                }
                taskDB.edit().apply {
                    putString(taskType + task.id, Gson().toJson(task))
                }.apply()
                checkProgress()
            }
            homePageButton.setOnLongClickListener {
                val intent = Intent(this, TaskDetail::class.java)
                intent.putExtra(resources.getString(R.string.task_id), task.id)
                intent.putExtra(resources.getString(R.string.task_type), taskType)
                startActivityForResult(intent, resources.getInteger(R.integer.task_detail_page_request_code))
                true
            }

            val listPageLayout = entry.second.second
            val listPageButton = listPageLayout.children.elementAt(0) as Button
            listPageButton.setOnClickListener {
                val intent = Intent(this, TaskDetail::class.java)
                intent.putExtra(resources.getString(R.string.task_id), task.id)
                intent.putExtra(resources.getString(R.string.task_type), taskType)
                startActivityForResult(intent, resources.getInteger(R.integer.task_detail_page_request_code))
            }
        }
    }

    private fun checkProgress() {
        totalActiveTask = 0
        completedTask = 0
        for (entry in taskList) {
            val task = entry.first
            if (!task.archived && task.active) {
                totalActiveTask += 1
            }
            if (!task.archived && task.active && task.completed()) {
                completedTask += 1
            }
        }
        val pair = history[history.size - 1]
        val log = pair.second
        log.progressPercent = getProgressPercent()
        log.progressString = getProgressString()
        log.taskList = getTaskListString()
        taskManagementDB.edit().apply {
            putString(resources.getString(R.string.history_key), gson.toJson(history))
        }.apply()
    }

    private fun calcCredit(): Int {
        val taskNumWeight = completedTask.toFloat().pow(0.25f)
        val streakWeight = min(1f, streak.toFloat().pow(0.25F))
        val completionWeight =
            when (completedTask) {
                totalActiveTask -> {
                    1.5f
                }
                else -> {
                    1f
                }
            }
        return (taskNumWeight * streakWeight * completionWeight).toInt()
    }

    private fun getProgressPercent(): Int {
        return if (totalActiveTask == 0) {
            100
        } else {
            100 * completedTask / totalActiveTask
        }
    }

    private fun getProgressString(): String {
        return "$completedTask/$totalActiveTask"
    }

    private fun getTaskListString(): String {
        var str = ""
        for (entry in taskList) {
            val task = entry.first
            if (!task.archived && task.active) {
                str += "${task.taskTitle} (${task.getProgressString()})\n"
            }
        }
        return str
    }

    private fun refresh() {
        var lastDailyUpdated = taskManagementDB.getLong(resources.getString(R.string.last_daily_updated), 0L)
        if (lastDailyUpdated == 0L) {
            dailyUpdate(System.currentTimeMillis())
        } else {
            handler = Handler()
            runnable = Runnable {
                val missingDays = TimeUtil.getMissingDays(lastDailyUpdated)
                for (date in missingDays) {
                    dailyUpdate(date)
                }
                lastDailyUpdated = taskManagementDB.getLong(resources.getString(R.string.last_daily_updated), lastDailyUpdated)
                handler.postDelayed(runnable, TimeUtil.oneMinute)
            }
            handler.post(runnable)
        }
    }

    private fun dailyUpdate(date: Long) {
        if (history.size != 0) {
            checkProgress()
        }
        if (totalActiveTask != 0 && completedTask == totalActiveTask) {
            streak += 1
            longestStreak = max(streak, longestStreak)
        } else if (totalActiveTask != 0 && completedTask != totalActiveTask) {
            streak = 0
        }
        credit += calcCredit()
        checkForReward()

        for (entry in taskList) {
            val task = entry.first
            val taskType = when (task) {
                is CheckOffTask -> {
                    resources.getString(R.string.check_off_type)
                }
                is CounterTask -> {
                    resources.getString(R.string.counter_type)
                }
                is TimerTask -> {
                    resources.getString(R.string.timer_type)
                }
                is ChronometerTask -> {
                    resources.getString(R.string.chronometer_type)
                }
                else -> {
                    "task type not found"
                }
            }
            task.dailyUpdate(date)
            taskDB.edit().apply {
                putString(taskType + task.id, gson.toJson(task))
            }.apply()
        }
        history.add(Pair(date, AllTasksHistoryLog()))

        taskManagementDB.edit().apply {
            putLong(resources.getString(R.string.last_daily_updated), date)
            putInt(resources.getString(R.string.streak_key), streak)
            putInt(resources.getString(R.string.best_streak_key), longestStreak)
            putInt(resources.getString(R.string.credit_key), credit)
            putString(resources.getString(R.string.history_key), gson.toJson(history))
        }.apply()

        initVariables()
        update()
        display()
    }

    private fun setUpHistoryPageProgress() {
        val todaysStreak = if (totalActiveTask != 0 && completedTask == totalActiveTask) {
            streak + 1
        } else {
            streak
        }
        overallCurrentStreakString.text = todaysStreak.toString()
        overallLongestStreakString.text = max(todaysStreak, longestStreak).toString()
        displaySelectedDateInfo(clickedDate)
    }
    
    private fun setUpCalendar() {
        historyPageCalendar.removeAllEvents()
        historyPageCalendar.setFirstDayOfWeek(1)
        historyPageCalendar.setUseThreeLetterAbbreviation(true)
        for (entry in history) {
            val date = entry.first
            val log = entry.second
            when (log.progressPercent) {
                100 -> {
                    historyPageCalendar.addEvent(AllTasksEvent(Color.GREEN, date, log))
                }
                0 -> {
                    historyPageCalendar.addEvent(AllTasksEvent(Color.TRANSPARENT, date, log))
                }
                else -> {
                    historyPageCalendar.addEvent(AllTasksEvent(Color.BLUE, date, log))
                }
            }
        }
        
        historyPageCalendar.setListener(object: CompactCalendarView.CompactCalendarViewListener {
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
        historyPageCalendarDate.text = TimeUtil.dateFormat.format(date)
        historyPageCalendarDateProgressString.text = getHistoryProgressString(date)
        taskListByDate.text = getTaskListString(date)
        displayWeekProgress(date)
    }

    private fun displayWeekProgress(date: Long) {
        when (TimeUtil.dayOfWeekFormat.format(date)) {
            "Sunday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date)
                historyPageMonBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date + 2 * TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date + 3 * TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date + 4 * TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date + 5 * TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date + 6 * TimeUtil.oneDay)
            }
            "Monday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date)
                historyPageTueBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date + 2 * TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date + 3 * TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date + 4 * TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date + 5 * TimeUtil.oneDay)
            }
            "Tuesday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - 2 *TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date)
                historyPageWedBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date + 2 * TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date + 3 * TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date + 4 * TimeUtil.oneDay)
            }
            "Wednesday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - 3 * TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date - 2 * TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date)
                historyPageThuBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date + 2 * TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date + 3 * TimeUtil.oneDay)
            }
            "Thursday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - 4 * TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date - 3 * TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date - 2 * TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date)
                historyPageFriBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date + 2 * TimeUtil.oneDay)
            }
            "Friday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - 5 * TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date - 4 * TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date - 3 * TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date - 2 * TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date)
                historyPageSatBar.progress = getHistoryProgressPercent(date + TimeUtil.oneDay)
            }
            "Saturday" -> {
                historyPageSunBar.progress = getHistoryProgressPercent(date - 6 * TimeUtil.oneDay)
                historyPageMonBar.progress = getHistoryProgressPercent(date - 5 * TimeUtil.oneDay)
                historyPageTueBar.progress = getHistoryProgressPercent(date - 4 * TimeUtil.oneDay)
                historyPageWedBar.progress = getHistoryProgressPercent(date - 3 * TimeUtil.oneDay)
                historyPageThuBar.progress = getHistoryProgressPercent(date - 2 * TimeUtil.oneDay)
                historyPageFriBar.progress = getHistoryProgressPercent(date - TimeUtil.oneDay)
                historyPageSatBar.progress = getHistoryProgressPercent(date)
            }
        }
    }

    private fun getHistoryProgressString(date: Long): String {
        val events = historyPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as AllTasksEvent).getProgressString()
            }
            else -> {
                resources.getString(R.string.empty_string)
            }
        }
    }

    private fun getHistoryProgressPercent(date: Long): Int {
        val events = historyPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as AllTasksEvent).getProgressPercent()
            }
            else -> {
                0
            }
        }
    }

    private fun getTaskListString(date: Long): String {
        val events = historyPageCalendar.getEvents(date)
        return when {
            events.size != 0 -> {
                (events[0] as AllTasksEvent).getTaskList()
            }
            else -> {
                resources.getString(R.string.empty_string)
            }
        }
    }

    private fun setUpThemes() {
        val themes = HashBiMap.create<Int, Int>()
        themes[R.id.grayButton] = R.style.GrayTheme
        themes[R.id.lightGrayButton] = R.style.LightGrayTheme
        themes[R.id.brownButton] = R.style.BrownTheme
        themes[R.id.redOrangeButton] = R.style.RedOrangeTheme
        themes[R.id.orangeButton] = R.style.OrangeTheme
        themes[R.id.pastelGreenButton] = R.style.PastelGreenTheme
        themes[R.id.brightGreenButton] = R.style.BrightGreenTheme
        themes[R.id.avocadoGreenButton] = R.style.AvocadoGreenTheme
        themes[R.id.aquaButton] = R.style.AquaTheme
        themes[R.id.greenButton] = R.style.GreenTheme
        themes[R.id.ashBlueButton] = R.style.AshBlueTheme
        themes[R.id.pastelBlueButton] = R.style.PastelBlueTheme
        themes[R.id.blueButton] = R.style.BlueTheme
        themes[R.id.darkBlueButton] = R.style.DarkBlueTheme
        themes[R.id.purpleButton] = R.style.PurpleTheme
        themes[R.id.lightPurpleButton] = R.style.LightPurpleTheme
        themes[R.id.violetButton] = R.style.VioletTheme
        themes[R.id.pinkPurpleButton] = R.style.PinkPurpleTheme
        themes[R.id.lightPinkPurpleButton] = R.style.LightPinkPurpleTheme
        themes[R.id.pinkButton] = R.style.PinkTheme
        themes[R.id.babyPinkButton] = R.style.BabyPinkTheme

        var themeButtonId = themes.inverse()[themeId]
        if (themeButtonId == null) {
            themeButtonId = R.id.brownButton
            themeId = R.style.BrownTheme
        }

        findViewById<RadioButton>(themeButtonId).isChecked = true

        themeColorInput.setOnCheckedChangeListener { _, checkedId ->
            themeId = themes[checkedId]!!
            taskManagementDB.edit().apply {
                putString(resources.getString(R.string.theme_key), getThemeName())
            }.apply()
            Toast.makeText(this, resources.getString(R.string.restart_to_see_changes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getThemeName(): String {
        return when (themeId) {
            R.style.GrayTheme -> {
                "GrayTheme"
            }
            R.style.LightGrayTheme -> {
                "LightGrayTheme"
            }
            R.style.BrownTheme -> {
                "BrownTheme"
            }
            R.style.RedOrangeTheme -> {
                "RedOrangeTheme"
            }
            R.style.OrangeTheme -> {
                "OrangeTheme"
            }
            R.style.PastelGreenTheme -> {
                "PastelGreenTheme"
            }
            R.style.BrightGreenTheme -> {
                "BrightGreenTheme"
            }
            R.style.AvocadoGreenTheme -> {
                "AvocadoGreenTheme"
            }
            R.style.AquaTheme -> {
                "AquaTheme"
            }
            R.style.GreenTheme -> {
                "GreenTheme"
            }
            R.style.AshBlueTheme -> {
                "AshBlueTheme"
            }
            R.style.PastelBlueTheme -> {
                "BlueTheme"
            }
            R.style.BlueTheme -> {
                "BlueTheme"
            }
            R.style.DarkBlueTheme -> {
                "DarkBlueTheme"
            }
            R.style.PurpleTheme -> {
                "PurpleTheme"
            }
            R.style.LightPurpleTheme -> {
                "LightPurpleTheme"
            }
            R.style.VioletTheme -> {
                "VioletTheme"
            }
            R.style.PinkPurpleTheme -> {
                "PinkPurpleTheme"
            }
            R.style.LightPinkPurpleTheme -> {
                "LightPinkPurpleTheme"
            }
            R.style.PinkTheme -> {
                "PinkTheme"
            }
            R.style.BabyPinkTheme -> {
                "BabyPinkTheme"
            }
            else -> {
                "BrownTheme"
            }
        }
    }

    private fun setUpNotification() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent =
            Intent(this, ReminderBroadcast::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        var dailyReminderOn = taskManagementDB.getBoolean(
            resources.getString(R.string.daily_reminder_notification_key), false)
        if (dailyReminderOn) {
            dailyReminderNotificationInput.text =
                taskManagementDB.getString(resources.getString(R.string.daily_reminder_time_key),
                    resources.getString(R.string.off))
        }

        dailyReminderNotificationInput.setOnClickListener {
            if (dailyReminderOn) {
                dailyReminderOn = false
                alarmManager.cancel(pendingIntent)
                dailyReminderNotificationInput.text = resources.getString(R.string.off)
                taskManagementDB.edit().apply {
                    putBoolean(resources.getString(R.string.daily_reminder_notification_key), false)
                }.apply()
            } else {
                val timePicker = TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                        dailyReminderOn = true
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                        }

                        alarmManager.setInexactRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                        d("katrina", "notify at ${SimpleDateFormat().format(calendar.timeInMillis)}")

                        taskManagementDB.edit().apply{
                            putBoolean(resources.getString(R.string.daily_reminder_notification_key), true)
                            putString(
                                resources.getString(R.string.daily_reminder_time_key),
                                TimeUtil.getTimeString(hourOfDay, minute)
                            )
                        }.apply()
                        dailyReminderNotificationInput.text = TimeUtil.getTimeString(hourOfDay, minute)
                    },
                    10,
                    0,
                    false
                )
                timePicker.show()
            }
        }
    }

    private fun setUpRewards() {
        var rewardAvailable = false
        for (i in rewards.indices) {
            val reward = rewards[i]
            if (reward.second > credit) {
                rewardProgressLayout.visibility = View.VISIBLE
                rewardName.text = reward.first
                rewardsProgressBar.progress = 100 * credit / reward.second
                val rewardProgressText = "$credit/${reward.second}"
                rewardProgress.text = rewardProgressText
                rewardIndex = i
                rewardAvailable = true
                break
            }
        }
        if (!rewardAvailable) {
            rewardProgressLayout.visibility = View.GONE
            rewardIndex = -1
        }

        editRewards.setOnClickListener {
            val intent = Intent(this, EditRewards::class.java)
            startActivityForResult(intent, resources.getInteger(R.integer.edit_rewards_request_code))
        }
    }

    private fun checkForReward() {
        if (rewardIndex != -1 && credit >= rewards[rewardIndex].second) {
            var completedRewards = rewards[rewardIndex].first
            for (i in rewardIndex + 1 until rewards.size) {
                if (credit >= rewards[i].second) {
                    completedRewards += ", ${rewards[i].first}"
                }
            }
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle(resources.getString(R.string.reward_achieved_title))
            val message = "${resources.getString(R.string.reward_achieved_message_one)} $credit " +
                    "${resources.getString(R.string.reward_achieved_message_two)} $completedRewards"
            dialogBuilder.setMessage(message)
            dialogBuilder.setPositiveButton(resources.getString(R.string.reward_achieved_message_button)) { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = dialogBuilder.create()
            dialog.show()
        }
    }

}