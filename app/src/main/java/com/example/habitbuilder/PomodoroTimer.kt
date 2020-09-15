package com.example.habitbuilder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log.d
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.pomodoro_timer.*
import java.lang.Exception

class PomodoroTimer : AppCompatActivity() {
    private lateinit var task: TimerTask
    private var timer: CountDownTimer? = null
    private lateinit var taskDB: SharedPreferences
    private lateinit var notificationManager: NotificationManagerCompat
    private val gson = Gson()
    private var timeLeft = 0L

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
        setContentView(R.layout.pomodoro_timer)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(resources.getInteger(R.integer.alarm_notification_id))
        taskDB = getSharedPreferences(resources.getString(R.string.task_database), Context.MODE_PRIVATE)
        val taskId = intent.getIntExtra(resources.getString(R.string.task_id), 0)
        val json = taskDB.getString(resources.getString(R.string.timer_type) + taskId, resources.getString(R.string.failed_to_retrieve_task))
        task = gson.fromJson(json, TimerTask::class.java)
        timeLeft = task.goalDuration
        sessionText.text = getSessionString()
        timeLeftText.text = timeLeftString()
        pomodoroProgressBar.progress = 0

        start.visibility = View.VISIBLE
        pause.visibility = View.GONE
        takeBreak.visibility = View.GONE
        skipBreak.visibility = View.GONE
        start.setOnClickListener {
            startSession()
        }
        endSession.setOnClickListener {
            endSession()
        }
    }

    override fun onBackPressed() {
        endSession()
    }

    override fun finish() {
        timer?.cancel()
        val resultIntent = Intent("com.example.habitbuilder.POMODORO_TIMER")
        resultIntent.putExtra(resources.getString(R.string.task_id), task.id)
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent)
        super.finish()
    }

    private fun startSession() {
        sessionText.text = getSessionString()
        timer = object: CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                pomodoroProgressBar.progress = getProgressPercent(task.goalDuration)
                timeLeftText.text = timeLeftString()
            }
            override fun onFinish() {
                timeLeft = 0L
                pomodoroProgressBar.progress = 100
                task.sessionsDone += 1
                task.updateProgress()
                taskDB.edit().apply{
                    putString(resources.getString(R.string.timer_type) + task.id, Gson().toJson(task))
                }.apply()
                pause.visibility = View.GONE
                takeBreak.visibility = View.VISIBLE
                skipBreak.visibility = View.VISIBLE
                takeBreak.setOnClickListener {
                    if (task.sessionsDone % 4 == 0) {
                        startBreak(30 * TimeUtil.oneMinute)
                    } else {
                        startBreak(5 * TimeUtil.oneMinute)
                    }
                }
                skipBreak.setOnClickListener {
                    skipBreak()
                }
                sendNotification(resources.getString(R.string.session_alarm_description))
            }
        }.start()

        start.visibility = View.GONE
        pause.visibility = View.VISIBLE
        pause.setOnClickListener {
            pauseSession()
        }
    }

    private fun pauseSession() {
        timer?.cancel()
        start.visibility = View.VISIBLE
        pause.visibility = View.GONE
        start.setOnClickListener {
            startSession()
        }
    }

    private fun startBreak(breakTime: Long) {
        sessionText.text = resources.getString(R.string.break_time)
        timer = object: CountDownTimer(breakTime,1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                pomodoroProgressBar.progress = getProgressPercent(breakTime)
                timeLeftText.text = timeLeftString()
            }
            override fun onFinish() {
                timeLeft = 0L
                pomodoroProgressBar.progress = 100
                start.visibility = View.VISIBLE
                skipBreak.visibility = View.GONE
                start.setOnClickListener {
                    startSession()
                }
                sendNotification(resources.getString(R.string.break_alarm_description))
            }
        }.start()

        takeBreak.visibility = View.GONE
        skipBreak.setOnClickListener {
            skipBreak()
        }
    }

    private fun skipBreak() {
        timer?.cancel()
        timeLeft = task.goalDuration
        takeBreak.visibility = View.GONE
        skipBreak.visibility = View.GONE
        startSession()
    }

    private fun endSession() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.end_session)
        dialogBuilder.setMessage(R.string.end_session_message)
        dialogBuilder.setPositiveButton(R.string.yes) { _, _ ->
            timer?.cancel()
            finish()
        }
        dialogBuilder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun sendNotification(description: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val builder = NotificationCompat.Builder(
            this@PomodoroTimer,
            resources.getString(R.string.timer_notification_channel_id)
        )
            .setSmallIcon(R.drawable.alarm)
            .setContentTitle(resources.getString(R.string.alarm_title))
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(
            resources.getInteger(R.integer.alarm_notification_id),
            builder.build()
        )
    }

    private fun timeLeftString(): String {
        return TimeUtil.getTimeString(timeLeft)
    }

    private fun getProgressPercent(goalDuration: Long): Int {
        return (100 * (goalDuration - timeLeft) / goalDuration).toInt()
    }

    private fun getSessionString(): String {
        return "${task.sessionsDone + 1}/${task.goalSessions}"
    }
}