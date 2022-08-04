package com.faithdeveloper.giveaway.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData

object NotificationUtil {
    private const val CHANNEL_ID = "Notifications"
    private const val COMMENTS_NOTIFICATION_GROUP_ID = "CommentsNotificationGroup"
    private const val COMMENTS_SUMMARY_ID = 0
    fun sendNotifications(context: Context, comments:List<CommentData>){
        val notifications = mutableListOf<Notification>()
        comments.onEach {
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${it.author!!.name} added a comment: ")
                .setContentText(it.comment!!.commentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setGroup(COMMENTS_NOTIFICATION_GROUP_ID)
                .build()
            notifications.add(notificationBuilder)
        }
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("New comments")
            .setContentText("${comments.size} new comments")
            .setGroup(COMMENTS_NOTIFICATION_GROUP_ID)
            .setGroupSummary(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = CHANNEL_ID
            val descriptionText = "Notifies you of new comments and posts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            with(NotificationManagerCompat.from(context)){
                comments.onEachIndexed { index, it ->
                    notify(index, notifications[index] )
                }
                notify(COMMENTS_SUMMARY_ID, summaryNotification)
            }
        }
    }


}