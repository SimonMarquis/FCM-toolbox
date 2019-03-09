package fr.smarquis.fcm.payloads;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public interface Payload {

    @DrawableRes
    int icon();

    @Nullable
    CharSequence display();

    @IdRes
    int notificationId();

    @NonNull
    NotificationCompat.Builder configure(@NonNull NotificationCompat.Builder builder);

}
