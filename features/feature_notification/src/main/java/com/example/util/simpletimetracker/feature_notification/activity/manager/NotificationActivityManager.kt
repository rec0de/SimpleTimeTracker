package com.example.util.simpletimetracker.feature_notification.activity.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.view.ContextThemeWrapper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.util.simpletimetracker.feature_views.extension.getBitmapFromView
import com.example.util.simpletimetracker.feature_views.extension.measureExactly
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.utils.PendingIntents
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import com.example.util.simpletimetracker.feature_notification.R
import com.example.util.simpletimetracker.feature_notification.recordType.customView.NotificationIconView
import com.example.util.simpletimetracker.navigation.Router
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationActivityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val colorMapper: ColorMapper,
    private val router: Router
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)
    private val iconView =
        NotificationIconView(ContextThemeWrapper(context, R.style.AppTheme))
    private val iconSize by lazy {
        context.resources.getDimensionPixelSize(R.dimen.notification_icon_size)
    }

    fun show(params: NotificationActivityParams) {
        val notification: Notification = buildNotification(params)
        createAndroidNotificationChannel()
        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    fun hide() {
        notificationManager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    private fun buildNotification(params: NotificationActivityParams): Notification {
        val notificationLayout = prepareView(params)

        val startIntent = router.getMainStartIntent().apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            startIntent,
            PendingIntents.getFlags(),
        )

        return NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setCustomContentView(notificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
    }

    private fun createAndroidNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATIONS_CHANNEL_ID,
                NOTIFICATIONS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun prepareView(params: NotificationActivityParams): RemoteViews {
        val iconBitmap = iconView.apply {
            itemIcon = RecordTypeIcon.Image(R.drawable.unknown)
            itemColor = colorMapper.toUntrackedColor(params.isDarkTheme)
            measureExactly(iconSize)
        }.getBitmapFromView()

        return RemoteViews(context.packageName, R.layout.notification_inactivity_layout).apply {
            setImageViewBitmap(R.id.ivNotificationInactivityIcon, iconBitmap)
            setTextViewText(R.id.tvNotificationInactivityText, params.title)
            setTextViewText(R.id.tvNotificationInactivityDescription, params.subtitle)
        }
    }

    companion object {
        private const val NOTIFICATIONS_CHANNEL_ID = "ACTIVITY"
        private const val NOTIFICATIONS_CHANNEL_NAME = "Activity"

        private const val NOTIFICATION_TAG = "activity_tag"
        private const val NOTIFICATION_ID = 0
    }
}