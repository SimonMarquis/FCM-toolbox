/*
 * Copyright 2017 Simon Marquis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.smarquis.fcm.payload;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.smarquis.fcm.FcmPayloadActivity;
import fr.smarquis.fcm.R;

public abstract class Payload implements Comparable<Payload> {

    private static final String KEY = "payloads";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    long timestamp;

    @Nullable
    transient RemoteMessage message;

    transient CharSequence display;

    Payload() {
        this.timestamp = System.currentTimeMillis();
    }

    Payload(@NonNull RemoteMessage message) {
        this.timestamp = System.currentTimeMillis();
        this.message = message;
    }

    @NonNull
    public static Payload with(RemoteMessage message) {
        Map<String, String> data = message.getData();
        Set<Map.Entry<String, String>> entries = data.entrySet();
        if (entries.size() == 1) {
            for (Map.Entry<String, String> entry : entries) {
                try {
                    switch (entry.getKey()) {
                        case PingPayload.KEY:
                            return PingPayload.create(message);
                        case TextPayload.KEY:
                            return TextPayload.create(message);
                        case LinkPayload.KEY:
                            return LinkPayload.create(message);
                        case AppPayload.KEY:
                            return AppPayload.create(message);
                        default:
                            break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return RawPayload.create(message);
    }

    @Nullable
    public static Payload with(@NonNull String key, @NonNull String value) {
        final String[] split = key.split("\\|");
        if (split.length != 2) {
            return null;
        }
        switch (split[1]) {
            case PingPayload.KEY:
                return GSON.fromJson(value, PingPayload.class);
            case TextPayload.KEY:
                return GSON.fromJson(value, TextPayload.class);
            case LinkPayload.KEY:
                return GSON.fromJson(value, LinkPayload.class);
            case AppPayload.KEY:
                return GSON.fromJson(value, AppPayload.class);
            case RawPayload.KEY:
                return GSON.fromJson(value, RawPayload.class);
            default:
                return null;
        }
    }

    @NonNull
    public static List<Payload> fetchPayloads(@NonNull Context context) {
        final List<Payload> payloads = new ArrayList<>();
        final SharedPreferences sp = getSharedPreferences(context);
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            final String key = entry.getKey();
            final String value = (String) entry.getValue();
            Payload payload = with(key, value);
            if (payload != null) {
                payloads.add(payload);
            }
        }
        Collections.sort(payloads);
        return payloads;
    }

    @Nullable
    static String extractPayloadData(RemoteMessage message, String key) {
        return message.getData().get(key);
    }

    public static boolean remove(@NonNull Context context, @NonNull Payload payload) {
        payload.cancelNotification(context);
        final SharedPreferences sp = getSharedPreferences(context);
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            if (entry.getKey().equals(payload.timestamp + "|" + payload.key())) {
                sp.edit().remove(entry.getKey()).apply();
                return true;
            }
        }
        return false;
    }

    public static void removeAll(Context context) {
        getNotificationManager(context).cancelAll();
        getSharedPreferences(context).edit().clear().apply();
    }

    public static void registerOnSharedPreferenceChanges(@NonNull Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        getSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChanges(@NonNull Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        getSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(listener);
    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
    }

    private static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected abstract String key();

    public final Payload saveToSharedPreferences(@NonNull Context context) {
        getSharedPreferences(context).edit().putString(timestamp + "|" + key(), GSON.toJson(this)).apply();
        return this;
    }

    public abstract void showNotification(Context context);

    public abstract void cancelNotification(Context context);

    @NonNull
    final NotificationCompat.Builder getNotificationBuilder(@NonNull Context context, RemoteMessage message) {
        return new NotificationCompat.Builder(context)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentIntent(FcmPayloadActivity.createPendingIntent(context, message))
                .setLocalOnly(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                ;
    }

    final void showNotification(@NonNull Context context, @NonNull Notification notification, @NonNull String tag, int id) {
        getNotificationManager(context).notify(tag, id, notification);
    }

    final void showNotification(@NonNull Context context, @NonNull Notification notification, int id) {
        getNotificationManager(context).notify(id, notification);
    }

    final void cancelNotification(@NonNull Context context, @NonNull String tag, int id) {
        getNotificationManager(context).cancel(tag, id);
    }

    final void cancelNotification(@NonNull Context context, int id) {
        getNotificationManager(context).cancel(id);
    }

    @Override
    public int compareTo(@NonNull Payload payload) {
        return (payload.timestamp < timestamp) ? -1 : ((payload.timestamp == timestamp) ? 0 : 1);
    }

    public abstract CharSequence getFormattedCharSequence(Context context);

    public final CharSequence getFormattedTimestamp() {
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
    }

}
