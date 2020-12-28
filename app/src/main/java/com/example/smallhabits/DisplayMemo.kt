package com.example.smallhabits

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.memo_page.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class DisplayMemo : AppCompatActivity() {
    private var clickedDate = System.currentTimeMillis()
    private var memoList: ArrayList<MemoLog>? = null
    private var memoIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(R.string.theme_key), "BrownTheme")
        val themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.memo_page)
        setUpCalendar()

        prevMemo.setOnClickListener {
            memoIndex -= 1
            if (memoIndex == -1) {
                memoIndex = memoList!!.size - 1
            }
            displayMemo()
        }

        nextMemo.setOnClickListener {
            memoIndex = (memoIndex + 1) % memoList!!.size
            displayMemo()
        }
    }

    private fun setUpCalendar() {
        val taskDB = getSharedPreferences(
            resources.getString(R.string.task_database), Context.MODE_PRIVATE)
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
            for (entry in task!!.history) {
                val log = entry.second
                if (log.memoImage != "" || log.memoString != "") {
                    addMemoEvent(entry.first, MemoLog(task.taskTitle, log.memoImage, log.memoString))
                }
            }
        }

        memoPageCalendar.setListener(object: CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date?) {
                displaySelectedDateInfo(dateClicked!!.time)
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                displaySelectedDateInfo(firstDayOfNewMonth!!.time)
            }

        })
        displaySelectedDateInfo(clickedDate)
    }

    private fun addMemoEvent(date: Long, memo: MemoLog) {
        val events = memoPageCalendar.getEvents(date)
        if (events.size == 0) {
            val memoList = ArrayList<MemoLog>()
            memoList.add(memo)
            memoPageCalendar.addEvent(MemoEvent(date, memoList))
        } else {
            (events[0] as MemoEvent).memoList.add(memo)
        }
    }

    private fun displaySelectedDateInfo(date: Long) {
        clickedDate = date
        memoPageCalendarDate.text = TimeUtil.dateFormat.format(date)
        memoList = getMemoList()
        memoIndex = 0
        displayMemo()
    }

    private fun displayMemo() {
        if (memoList != null) {
            memoPageTaskName.text = memoList!![memoIndex].taskTitle
            val memoImageString = memoList!![memoIndex].memoImage
            val memoString = memoList!![memoIndex].memoString
            val memoImageUri = Uri.parse(memoImageString)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(memoImageUri, takeFlags)
            } catch (e: Exception){}
            memoPageMemoLayout.visibility = View.VISIBLE
            if (memoString == resources.getString(R.string.empty_string) && memoImageString != resources.getString(R.string.empty_string)) {
                memoPageMemoImage.visibility = View.VISIBLE
                memoPageMemoText.visibility = View.GONE
                try {
                    memoPageMemoImage.setImageURI(memoImageUri)
                } catch (e: Exception) {
                    memoPageMemoImage.visibility = View.GONE
                }
            } else if (memoString != resources.getString(R.string.empty_string) && memoImageString == resources.getString(R.string.empty_string)) {
                memoPageMemoImage.visibility = View.GONE
                memoPageMemoText.visibility = View.VISIBLE
                memoPageMemoText.text = memoString
            } else if (memoString != resources.getString(R.string.empty_string) && memoImageString != resources.getString(R.string.empty_string)){
                memoPageMemoImage.visibility = View.VISIBLE
                memoPageMemoText.visibility = View.VISIBLE
                memoPageMemoText.text = memoString
                try {
                    memoPageMemoImage.setImageURI(memoImageUri)
                } catch (e: Exception) {
                    memoPageMemoImage.visibility = View.GONE
                }
            }
        } else {
            memoPageMemoLayout.visibility = View.GONE
        }
    }

    private fun getMemoList(): ArrayList<MemoLog>? {
        val events = memoPageCalendar.getEvents(clickedDate)
        return when {
            events.size != 0 -> {
                (events[0] as MemoEvent).memoList
            }
            else -> {
                null
            }
        }
    }
}