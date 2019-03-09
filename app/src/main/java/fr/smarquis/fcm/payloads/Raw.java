package fr.smarquis.fcm.payloads;

import android.annotation.SuppressLint;
import android.content.Context;

import com.squareup.moshi.Json;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import fr.smarquis.fcm.Messages;
import fr.smarquis.fcm.R;

public class Raw implements Payload {

    static final String KEY = "raw";

    @Json(name = "data")
    private final Map<String, String> data;

    Raw(Map<String, String> data) {
        this.data = data;
    }

    @Nullable
    private transient CharSequence display;

    @Override
    public int icon() {
        return R.drawable.ic_code_24dp;
    }

    @Override
    public int notificationId() {
        return R.id.notification_id_raw;
    }

    @Override
    @NonNull
    public NotificationCompat.Builder configure(@NonNull NotificationCompat.Builder builder) {
        @SuppressLint("RestrictedApi") Context context = builder.mContext;
        return builder.setContentTitle(context.getString(R.string.payload_raw))
                .setContentText(display())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(display()));
    }

    @Override
    public synchronized CharSequence display() {
        if (display == null) {
            display = Messages.moshi().adapter(Map.class).indent("  ").toJson(data);
        }
        return display;
    }

}
