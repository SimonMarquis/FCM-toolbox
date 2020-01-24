package fr.smarquis.fcm.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat.*
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import fr.smarquis.fcm.R
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.view.ui.MainActivity

object Notifications {

    @RequiresApi(api = O)
    private fun createNotificationChannel(context: Context) {
        val id = context.getString(R.string.notification_channel_id)
        val name: CharSequence = context.getString(R.string.notification_channel_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setShowBadge(true)
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun getNotificationBuilder(context: Context): Builder {
        if (SDK_INT >= O) {
            createNotificationChannel(context)
        }
        return Builder(context, context.getString(R.string.notification_channel_id))
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0))
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setDefaults(DEFAULT_ALL)
                .setPriority(PRIORITY_MAX)
                .setCategory(CATEGORY_MESSAGE)
    }

    fun show(context: Context, message: Message) {
        val payload = message.payload ?: return
        val notification = payload.configure(getNotificationBuilder(context).setSmallIcon(payload.icon())).build()
        context.getSystemService<NotificationManager>()?.notify(message.messageId, payload.notificationId(), notification)
    }

    fun removeAll(context: Context) {
        context.getSystemService<NotificationManager>()?.cancelAll()
    }
}