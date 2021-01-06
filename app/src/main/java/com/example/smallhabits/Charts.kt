package com.keepsimple.smallhabits

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.charts.*
import java.lang.Exception
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Charts : AppCompatActivity() {

    private var colorPrimary = Color.DKGRAY
    private var colorAccent = Color.BLACK
    private var textColor = Color.BLACK
    private var themeId = 0
    private var sortMethod = ""
    private val taskNotFoundException =
        Resources.NotFoundException("failed to retrieve task")

    override fun onCreate(savedInstanceState: Bundle?) {
        val taskManagementDB = getSharedPreferences(
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
        sortMethod = taskManagementDB.getString(
            resources.getString(R.string.sort_key),
            resources.getString(R.string.sort_by_alphabet)
        )!!

        super.onCreate(savedInstanceState)
        setContentView(R.layout.charts)
        getThemeColors()

        when (intent.getIntExtra(resources.getString(R.string.request), resources.getInteger(R.integer.all_tasks_graph_request_code))) {
            resources.getInteger(R.integer.individual_task_graph_request_code) -> {
                val task = getTask()
                displayIndividualTaskCharts(task)
            }
            resources.getInteger(R.integer.all_tasks_graph_request_code) -> {
                displayAllTasksCharts()
            }
        }

        nextChart.setOnClickListener {
            chartFlipper.showNext()
            animate(chartFlipper[chartFlipper.displayedChild])
        }

        prevChart.setOnClickListener {
            chartFlipper.showPrevious()
            animate(chartFlipper[chartFlipper.displayedChild])
        }
    }

    private fun getThemeColors() {
        val array = intArrayOf(
            R.attr.colorPrimary,
            R.attr.colorAccent,
            android.R.attr.textColorPrimary
        )
        @StyleableRes var index = 0
        val attr = obtainStyledAttributes(array)
        colorPrimary = attr.getColor(index++, Color.DKGRAY)
        colorAccent = attr.getColor(index++, Color.BLACK)
        textColor = attr.getColor(index, Color.BLACK)
        attr.recycle()
    }

    private fun getTask(): Task {
        val taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
        val taskId = intent.getIntExtra(resources.getString(R.string.task_id), 0)
        val taskType = intent.getStringExtra(resources.getString(R.string.task_type))
        val gson = Gson()
        val json = taskDB.getString(taskType!! + taskId,
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

    private fun displayIndividualTaskCharts(task: Task) {
        val progressList =
            task.history.map { Pair(it.first, it.second.progressPercent) }
        val stats = getStats(progressList)

        displayPieChart(stats)
        displayRadarChart(stats)
    }

    private fun displayAllTasksCharts() {
        val taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val historyJson = taskManagementDB.getString(
            resources.getString(R.string.history_key),
            resources.getString(R.string.failed_to_retrieve_history))
        val history = if (historyJson == resources.getString(R.string.failed_to_retrieve_history)) {
            ArrayList<Pair<Long, AllTasksHistoryLog>>()
        } else {
            val type = object: TypeToken<ArrayList<Pair<Long, AllTasksHistoryLog>>>(){}.type
            Gson().fromJson(historyJson, type)
        }
        val progressList = history.map { Pair(it.first, it.second.progressPercent) }
        val stats = getStats(progressList)
        val allTaskStats = getAllTaskStat()

        displayPieChart(stats)
        displayRadarChart(stats)
        displayBarChart(allTaskStats)
    }

    private fun displayPieChart(stats: HashMap<String, Int>) {
        val pie = pieChart
        pie.description.isEnabled = false
        pie.setUsePercentValues(true)
        pie.setExtraOffsets(5f, 10f, 5f, 5f)
        pie.dragDecelerationFrictionCoef = 0.95f
        pie.setDrawCenterText(true)
        pie.centerText = resources.getString(R.string.progress_history)
        pie.setCenterTextSize(20f)
        pie.setCenterTextColor(textColor)
        pie.isDrawHoleEnabled = true
        pie.setHoleColor(Color.TRANSPARENT)
        pie.setTransparentCircleColor(Color.WHITE)
        pie.setTransparentCircleAlpha(110)
        pie.holeRadius = 58f
        pie.transparentCircleRadius = 61f
        val legend = pie.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 0f
        legend.yOffset = 0f
        legend.textSize = 14f
        legend.textColor = textColor

        val colors = ArrayList<Int>()
        val entries = ArrayList<PieEntry>()
        if (stats[resources.getString(R.string.completed)] != 0) {
            entries.add(PieEntry(
                stats[resources.getString(R.string.completed)]!!.toFloat(),
                resources.getString(R.string.completed))
            )
            colors.add(ContextCompat.getColor(this, R.color.pastel_green_3))
        }
        if (stats[resources.getString(R.string.started)] != 0) {
            entries.add(PieEntry(
                stats[resources.getString(R.string.started)]!!.toFloat(),
                resources.getString(R.string.started))
            )
            colors.add(ContextCompat.getColor(this, R.color.blue_2))
        }
        if (stats[resources.getString(R.string.no_progress)] != 0) {
            entries.add(PieEntry(
                stats[resources.getString(R.string.no_progress)]!!.toFloat(),
                resources.getString(R.string.no_progress))
            )
            colors.add(ContextCompat.getColor(this, R.color.dark_pink))
        }
        val dataSet = PieDataSet(entries, resources.getString(R.string.empty_string))
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = colors
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pie))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)
        pie.data = data
        pie.highlightValues(null)
        pie.invalidate()
        pie.animateXY(1400, 1400)
    }

    private fun displayRadarChart(stats: HashMap<String, Int>) {
        val radar = radarChart
        radar.description.isEnabled = false
        radar.webLineWidth = 2f
        radar.webColor = Color.LTGRAY
        radar.webLineWidthInner = 1f
        radar.webColorInner = Color.LTGRAY
        radar.webAlpha = 100

        val xAxis = radar.xAxis
        xAxis.textSize = 11f
        xAxis.xOffset = 0f
        xAxis.yOffset = 0f
        xAxis.textColor = textColor
        xAxis.valueFormatter = object: ValueFormatter() {
            private val days = listOf(
                resources.getString(R.string.sun),
                resources.getString(R.string.mon),
                resources.getString(R.string.tue),
                resources.getString(R.string.wed),
                resources.getString(R.string.thu),
                resources.getString(R.string.fri),
                resources.getString(R.string.sat)
            )

            override fun getFormattedValue(value: Float): String {
                return days[value.toInt() % days.size]
            }
        }
        val yAxis = radar.yAxis
        yAxis.setLabelCount(6, true)
        yAxis.textSize = 11f
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 100f
        yAxis.setDrawLabels(false)

        val legend = radar.legend
                legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 0f
        legend.yOffset = 0f
        legend.textSize = 14f
        legend.textColor = textColor

        val entries = ArrayList<RadarEntry>()
        entries.add(RadarEntry(stats["Sunday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Monday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Tuesday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Wednesday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Thursday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Friday"]!!.toFloat()))
        entries.add(RadarEntry(stats["Saturday"]!!.toFloat()))
        val dataSet = RadarDataSet(entries, resources.getString(R.string.progress_history))
        dataSet.color = colorAccent
        dataSet.fillColor = colorPrimary
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 180
        dataSet.isDrawHighlightCircleEnabled = true
        dataSet.setDrawHighlightIndicators(false)
        val sets = ArrayList<IRadarDataSet>()
        sets.add(dataSet)
        val data = RadarData(sets)
        data.setValueTextSize(8f)
        data.setDrawValues(false)
        data.setValueTextColor(textColor)
        radar.data = data
        radar.invalidate()
    }

    private fun displayBarChart(stats: List<Pair<String, Int>>) {
        val bar = HorizontalBarChart(this)
        bar.description.isEnabled = false
        bar.setDrawBarShadow(false)
        bar.setDrawValueAboveBar(false)
        bar.setMaxVisibleValueCount(60)
        bar.setPinchZoom(true)
        bar.setDrawGridBackground(false)

        val xAxis = bar.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 14f
        xAxis.textColor = textColor
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = stats.size
        xAxis.valueFormatter = object: ValueFormatter() {
            private val taskTitles = stats.map { it.first }
            override fun getFormattedValue(value: Float): String {
                return taskTitles[value.toInt() % taskTitles.size]
            }
        }

        val yAxisLeft = bar.axisLeft
        yAxisLeft.textSize = 11f
        yAxisLeft.textColor = textColor
        yAxisLeft.setLabelCount(6, true)
        yAxisLeft.setDrawAxisLine(false)
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        val yAxisRight = bar.axisRight
        yAxisLeft.textSize = 11f
        yAxisRight.textColor = textColor
        yAxisLeft.setLabelCount(6, true)
        yAxisRight.setDrawAxisLine(false)
        yAxisRight.setDrawGridLines(false)
        yAxisRight.axisMinimum = 0f
        yAxisRight.axisMaximum = 100f
        yAxisRight.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        bar.setFitBars(true)
        bar.legend.isEnabled = false

        val entries = ArrayList<BarEntry>()
        for (i in stats.indices) {
            entries.add(BarEntry(i.toFloat(), stats[i].second.toFloat()))
        }
        val dataSet = BarDataSet(entries, resources.getString(R.string.progress_history))
        dataSet.setDrawIcons(false)
        val colors = listOf(
            ContextCompat.getColor(this, R.color.brown_2),
            ContextCompat.getColor(this, R.color.orange_2),
            ContextCompat.getColor(this, R.color.yellow),
            ContextCompat.getColor(this, R.color.pastel_green_2),
            ContextCompat.getColor(this, R.color.blue_2),
            ContextCompat.getColor(this, R.color.purple_2),
            ContextCompat.getColor(this, R.color.pink_2)
        )
        dataSet.colors = colors
        val sets = ArrayList<IBarDataSet>()
        sets.add(dataSet)
        val data = BarData(sets)
        data.setValueTextSize(10f)
        data.setValueTextColor(textColor)
        data.barWidth = 0.9f
        bar.data = data
        bar.setDrawValueAboveBar(true)
        chartFlipper.addView(bar)
    }

    private fun getStats(progressList: List<Pair<Long, Int>>): HashMap<String, Int> {
        val stats = HashMap<String, Int>()
        var noProgress = 0
        var started = 0
        var completed = 0
        val weeklyProgress = HashMap<String, Array<Int>>()
        weeklyProgress["Sunday"] = Array(2) {0}
        weeklyProgress["Monday"] = Array(2) {0}
        weeklyProgress["Tuesday"] = Array(2) {0}
        weeklyProgress["Wednesday"] = Array(2) {0}
        weeklyProgress["Thursday"] = Array(2) {0}
        weeklyProgress["Friday"] = Array(2) {0}
        weeklyProgress["Saturday"] = Array(2) {0}

        for (entry in progressList) {
            val date = entry.first
            val progressPercent = entry.second
            val weekday = TimeUtil.dayOfWeekFormat.format(date)

            when {
                progressPercent == 0 -> {
                    noProgress += 1
                }
                progressPercent < 100 -> {
                    started += 1
                }
                progressPercent == 100 -> {
                    completed += 1
                }
            }
            weeklyProgress[weekday]!![0] += 1
            weeklyProgress[weekday]!![1] += progressPercent
        }

        stats[resources.getString(R.string.no_progress)] = noProgress
        stats[resources.getString(R.string.started)] = started
        stats[resources.getString(R.string.completed)] = completed

        for (entry in weeklyProgress) {
            val days = entry.value[0]
            val progressSum = entry.value[1]
            val progressAverage = if (days == 0) {
                0
            } else {
                progressSum / days
            }
            stats[entry.key] = progressAverage
        }
        return stats
    }

    private fun getAllTaskStat(): List<Pair<String, Int>> {

        val taskStat = ArrayList<Pair<Task, Int>>()

        val taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
        val gson = Gson()
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
            var progressSum = 0
            for (entry in task!!.history) {
                val log = entry.second
                progressSum += log.progressPercent
            }

            val progressPercent = if (task.history.size == 0) {
                0
            } else {
                progressSum / task.history.size
            }
            val pair = Pair(task, progressPercent)
            taskStat.add(pair)
        }
        sortTaskList(taskStat)
        return taskStat.map { Pair(it.first.taskTitle, it.second) }
    }

    private fun sortTaskList(taskList: ArrayList<Pair<Task, Int>>) {
        if (sortMethod == resources.getString(R.string.sort_by_id)) {
            Collections.sort(taskList,
                kotlin.Comparator { pairOne, pairTwo -> pairTwo.first.id - pairOne.first.id })
        } else {
            Collections.sort(taskList,
                kotlin.Comparator { pairOne, pairTwo ->
                    Collator.getInstance(Locale.CHINESE).compare(
                        pairTwo.first.taskTitle,
                        pairOne.first.taskTitle
                    )
                }
            )
        }
    }

    private fun animate(view: View) {
        when (view) {
            is PieChart -> {
                view.animateXY(1400, 1400)
            }
            is RadarChart -> {
                view.animateXY(1400, 1400, Easing.EaseInOutQuad)
            }
            is BarChart -> {
                view.animateY(1400)
            }
        }
    }
}