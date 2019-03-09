package fr.smarquis.fcm.payloads;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.squareup.moshi.Json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import fr.smarquis.fcm.R;
import fr.smarquis.fcm.Truss;

public class App implements Payload {

    static final String KEY = "app";

    @Json(name = "title")
    private final String title;

    @Json(name = "package")
    private final String packageName;

    @Nullable
    private transient CharSequence display;

    public App(String packageName, String title) {
        this.packageName = packageName;
        this.title = title;
    }

    @Override
    public int icon() {
        return R.drawable.ic_shop_24dp;
    }

    @Override
    public int notificationId() {
        return R.id.notification_id_app;
    }

    @Override
    @NonNull
    public NotificationCompat.Builder configure(@NonNull NotificationCompat.Builder builder) {
        @SuppressLint("RestrictedApi") Context context = builder.mContext;
        builder.setContentTitle(TextUtils.isEmpty(title) ? context.getString(R.string.payload_app) : title)
                .setContentText(packageName)
                .addAction(0, context.getString(R.string.payload_app_store), PendingIntent.getActivity(context, 0, playStore(), 0));
        if (isInstalled(context)) {
            builder.addAction(0, context.getString(R.string.payload_app_uninstall), PendingIntent.getActivity(context, 0, uninstall(), 0));
        }
        return builder;
    }

    @Override
    public synchronized CharSequence display() {
        if (display == null) {
            display = new Truss()
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("title: ").popSpan().append(title).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("package: ").popSpan().append(packageName)
                    .build();
        }
        return display;
    }


    Intent playStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    Intent uninstall() {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    boolean isInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
