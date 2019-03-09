package fr.smarquis.fcm.payloads;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import fr.smarquis.fcm.R;

public class Ping implements Payload {

    static final String KEY = "ping";

    @Override
    public int icon() {
        return R.drawable.ic_notifications_none_24dp;
    }

    @Override
    public int notificationId() {
        return R.id.notification_id_ping;
    }

    @Override
    @NonNull
    public NotificationCompat.Builder configure(@NonNull NotificationCompat.Builder builder) {
        @SuppressLint("RestrictedApi") Context context = builder.mContext;
        return builder.setContentTitle(context.getString(R.string.payload_ping));
    }

    @Nullable
    @Override
    public CharSequence display() {
        return null;
    }

}
