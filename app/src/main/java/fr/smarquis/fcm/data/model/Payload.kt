package fr.smarquis.fcm.data.model

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.firebase.messaging.RemoteMessage
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import fr.smarquis.fcm.R
import fr.smarquis.fcm.view.ui.CopyToClipboardActivity
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.IOException

sealed class Payload {

    @IdRes
    abstract fun notificationId(): Int

    @DrawableRes
    abstract fun icon(): Int

    abstract fun display(): CharSequence?

    abstract fun configure(builder: Builder): Builder

    data class App(
            @Json(name = "title")
            private val title: String? = null,
            @Json(name = "package")
            private val packageName: String? = null
    ) : Payload() {

        override fun notificationId(): Int = R.id.notification_id_app

        override fun icon(): Int = R.drawable.ic_shop_24dp

        private val display: CharSequence by lazy {
            buildSpannedString {
                bold { append("title: ") }
                append("$title\n")
                bold { append("package: ") }
                append(packageName)
            }
        }

        override fun display(): CharSequence? = display

        @SuppressLint("RestrictedApi")
        override fun configure(builder: Builder): Builder = builder.apply {
            builder.setContentTitle(if (TextUtils.isEmpty(title)) mContext.getString(R.string.payload_app) else title)
                    .setContentText(packageName)
                    .addAction(0, mContext.getString(R.string.payload_app_store), PendingIntent.getActivity(mContext, 0, playStore(), 0))
            if (isInstalled(mContext)) {
                builder.addAction(0, mContext.getString(R.string.payload_app_uninstall), PendingIntent.getActivity(mContext, 0, uninstall(), 0))
            }
        }

        fun playStore(): Intent = Intent(ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }

        fun uninstall(): Intent = Intent(ACTION_DELETE, Uri.parse("package:$packageName")).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }

        fun isInstalled(context: Context): Boolean = try {
            context.packageManager.getPackageInfo(packageName.orEmpty(), 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    }

    data class Link(
            @Json(name = "title")
            private val title: String? = null,
            @Json(name = "url")
            private val url: String? = null,
            @Json(name = "open")
            val open: Boolean = false
    ) : Payload() {

        override fun notificationId(): Int = R.id.notification_id_link

        override fun icon(): Int = R.drawable.ic_link_24dp

        private val display: CharSequence by lazy {
            buildSpannedString {
                bold { append("title: ") }
                append("$title\n")
                bold { append("url: ") }
                append("$url\n")
                bold { append("open: ") }
                append(open.toString())
            }
        }

        override fun display(): CharSequence? = display

        @SuppressLint("RestrictedApi")
        override fun configure(builder: Builder): Builder = builder.apply {
            setContentTitle(if (TextUtils.isEmpty(title)) mContext.getString(R.string.payload_link) else title).setContentText(url)
            if (!TextUtils.isEmpty(url)) {
                addAction(0, mContext.getString(R.string.payload_link_open), PendingIntent.getActivity(mContext, 0, intent(), 0))
            }
        }

        fun intent(): Intent = Intent(ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }

    }

    @Suppress("CanSealedSubClassBeObject")
    class Ping : Payload() {

        override fun notificationId(): Int = R.id.notification_id_ping

        override fun icon(): Int = R.drawable.ic_notifications_none_24dp

        override fun display(): CharSequence? = null

        @SuppressLint("RestrictedApi")
        override fun configure(builder: Builder): Builder = builder.apply { setContentTitle(mContext.getString(R.string.payload_ping)) }
    }

    data class Text(
            @Json(name = "title")
            private val title: String? = null,
            @Json(name = "message")
            val text: String? = null,
            @Json(name = "clipboard")
            val clipboard: Boolean = false
    ) : Payload() {

        override fun notificationId(): Int = R.id.notification_id_text

        override fun icon(): Int = R.drawable.ic_chat_24dp

        private val display: CharSequence by lazy {
            buildSpannedString {
                bold { append("title: ") }
                append("$title\n")
                bold { append("text: ") }
                append("$text\n")
                bold { append("clipboard: ") }
                append(clipboard.toString())
            }
        }

        override fun display(): CharSequence? = display

        @SuppressLint("RestrictedApi")
        override fun configure(builder: Builder): Builder = builder.apply {
            val intent = Intent(mContext, CopyToClipboardActivity::class.java)
            intent.putExtra(EXTRA_TEXT, text)
            setContentTitle(if (TextUtils.isEmpty(title)) mContext.getString(R.string.payload_text) else title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .addAction(0, mContext.getString(R.string.payload_text_copy), PendingIntent.getActivity(mContext, 0, intent, 0))
        }

    }

    data class Raw(
            @Json(name = "data")
            private val data: Map<String, String>? = null
    ) : Payload(), KoinComponent {

        override fun notificationId(): Int = R.id.notification_id_raw

        override fun icon(): Int = R.drawable.ic_code_24dp

        private val display: CharSequence by lazy {
            moshi.adapter<Map<*, *>>(MutableMap::class.java).indent("  ").toJson(data)
        }

        override fun display(): CharSequence? = display

        @SuppressLint("RestrictedApi")
        override fun configure(builder: Builder): Builder = builder.apply {
            setContentTitle(mContext.getString(R.string.payload_raw))
                    .setContentText(display())
                    .setStyle(NotificationCompat.BigTextStyle().bigText(display()))
        }
    }

    companion object : KoinComponent {

        private val moshi by inject<Moshi>()

        private val lut by inject<Map<String, Class<out Payload>>>()

        fun extract(message: RemoteMessage): Payload {
            val data = message.data
            val entries: Set<Map.Entry<String, String>> = data.entries
            for ((key, value) in entries) {
                val clazz = lut[key] ?: continue
                try {
                    val adapter = moshi.adapter(clazz)
                    val payload = adapter.fromJson(value)
                    if (payload != null) {
                        return payload
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return Raw(data)
        }
    }

}