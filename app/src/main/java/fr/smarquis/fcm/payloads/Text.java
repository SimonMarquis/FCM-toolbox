package fr.smarquis.fcm.payloads;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.squareup.moshi.Json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import fr.smarquis.fcm.CopyToClipboardActivity;
import fr.smarquis.fcm.R;
import fr.smarquis.fcm.Truss;
import fr.smarquis.fcm.Util;

public class Text implements Payload {

    static final String KEY = "text";

    @Json(name = "title")
    private final String title;
    @Json(name = "message")
    public final String text;
    @Json(name = "clipboard")
    private final boolean clipboard;

    @Nullable
    private transient CharSequence display;

    public Text(String title, String text, boolean clipboard) {
        this.title = title;
        this.text = text;
        this.clipboard = clipboard;
    }

    @Override
    public int icon() {
        return R.drawable.ic_chat_24dp;
    }

    @Override
    public int notificationId() {
        return R.id.notification_id_text;
    }

    @Override
    @NonNull
    public NotificationCompat.Builder configure(@NonNull NotificationCompat.Builder builder) {
        @SuppressLint("RestrictedApi") Context context = builder.mContext;
        final Intent intent = new Intent(context, CopyToClipboardActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return builder.setContentTitle(TextUtils.isEmpty(title) ? context.getString(R.string.payload_text) : title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .addAction(0, context.getString(R.string.payload_text_copy), PendingIntent.getActivity(context, 0, intent, 0));
    }

    @Override
    public synchronized CharSequence display() {
        if (display == null) {
            display = new Truss()
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("title: ").popSpan().append(title).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("text: ").popSpan().append(text).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("clipboard: ").popSpan().append(String.valueOf(clipboard))
                    .build();
        }
        return display;
    }

    public boolean clipboard() {
        return clipboard;
    }

    public void copyToClipboard(@NonNull Context context) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> copyToClipboard(context));
        }
        Util.copyToClipboard(context.getApplicationContext(), text);
    }
}
