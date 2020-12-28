package com.example.smallhabits

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            0
        )

        val builder = NotificationCompat.Builder(
            context!!,
            context.resources.getString(R.string.daily_notification_channel_id)
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.resources.getString(R.string.daily_reminder_title))
            .setContentText(context.resources.getString(R.string.daily_reminder_description))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.resources.getString(R.string.daily_reminder_description)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(
            context.resources.getInteger(R.integer.daily_reminder_id),
            builder.build()
        )
    }

}