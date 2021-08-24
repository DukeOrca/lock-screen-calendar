//package com.duke.orca.android.kotlin.lockscreencalendar.lockscreen.service
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import androidx.core.app.NotificationCompat
//import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
//import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
//import com.duke.orca.android.kotlin.lockscreencalendar.main.views.MainActivity
//import io.reactivex.Single
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//
//object NotificationBuilder {
//    const val ID = 5337800
//
//    private const val PREFIX = "$PACKAGE_NAME.lockscreen.service.NotificationBuilder"
//    private const val CHANNEL_ID = "$PREFIX.CHANNEL_ID"
//    private const val CHANNEL_NAME = "$PREFIX.CHANNEL_NAME"
//    private const val CHANNEL_DESCRIPTION = "com.flow.android.kotlin.lockscreen.lock_screen.channel_description" // todo change real des.
//
//    fun single(context: Context, notificationManager: NotificationManager): Single<NotificationCompat.Builder> {
//        return Single.create {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val importance = NotificationManager.IMPORTANCE_MIN
//                val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
//
//                notificationChannel.description = CHANNEL_DESCRIPTION
//                notificationChannel.setShowBadge(false)
//
//                notificationManager.createNotificationChannel(notificationChannel)
//            }
//
//            val pendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                Intent(context, MainActivity::class.java),
//                PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//            GlobalScope.launch(Dispatchers.IO) {
//                val smallIcon = R.drawable.ic_round_lock_24
//
//                var contentTitle = context.getString(R.string.app_name)
//                var contentText = context.getString(R.string.notification_content_text)
//                var subText = BLANK
//
//                val simpleDateFormat = SimpleDateFormat(context.getString(R.string.format_time_002), Locale.getDefault())
//
//                calendarEventForNotification?.let {
//                    contentTitle = it.title.take(160)
//                    contentText = context.getString(R.string.notification_builder_000)
//                    subText = it.begin.toDateString(simpleDateFormat)
//                } ?: memoForNotification?.let { memo ->
//                    contentTitle = context.getString(R.string.notification_builder_001)
//                    contentText = memo.content.take(160)
//                    subText = memo.alarmTime.toDateString(simpleDateFormat)
//                } ?: run {
//                    if (memos.isNotEmpty()) {
//                        contentTitle = context.getString(R.string.notification_builder_001)
//                        contentText = memos[0].content.take(160)
//                    }
//                }
//
//                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
//                    .setSmallIcon(smallIcon)
//                    .setContentTitle(contentTitle)
//                    .setContentText(contentText)
//                    .setContentIntent(pendingIntent)
//                    .setPriority(NotificationCompat.PRIORITY_MIN)
//                    .setShowWhen(false)
//                    .setSubText(subText)
//                it.onSuccess(builder)
//            }
//        }
//    }
//}