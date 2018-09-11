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
import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.text.style.StyleSpan;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

import fr.smarquis.fcm.R;
import fr.smarquis.fcm.Truss;

public class RawPayload extends Payload {

    static final String KEY = "raw";

    public final String text;

    private RawPayload(RemoteMessage message, String text) {
        super(message);
        this.text = text;
    }

    static RawPayload create(RemoteMessage message) {
        final String text = extractAsJsonObject(message);
        return new RawPayload(message, text);
    }

    private static String extractAsJsonObject(RemoteMessage message) {
        try {
            JSONObject obj = new JSONObject(message.getData());
            return obj.toString();
        } catch (Exception e) {
            return extractCharSequence(message).toString();
        }
    }

    private static CharSequence extractCharSequence(RemoteMessage message) {
        Truss truss = new Truss();
        final Map<String, String> data = message.getData();
        Set<String> strings = data.keySet();
        for (String key : strings) {
            String value = data.get(key);
            truss.pushSpan(new StyleSpan(Typeface.BOLD));
            truss.append(key).append(":\n");
            truss.popSpan();
            truss.append(value).append('\n');
        }
        return truss.build();
    }

    private CharSequence getCharSequence() {
        try {
            return new JSONObject(text).toString(4);
        } catch (JSONException e) {
            return text;
        }
    }


    @Override
    protected String key() {
        return KEY;
    }

    @Override
    public void showNotification(Context context) {
        if (message == null) {
            return;
        }
        CharSequence content = getCharSequence();
        final Notification notification = getNotificationBuilder(context, message)
                .setSmallIcon(R.drawable.ic_cloud_queue_24dp)
                .setContentTitle("Raw")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .build();
        showNotification(context, notification, String.valueOf(timestamp), R.id.raw_notification_id);
    }

    @Override
    public void cancelNotification(Context context) {
        cancelNotification(context, String.valueOf(timestamp), R.id.raw_notification_id);
    }

    @Override
    public CharSequence getFormattedCharSequence(Context context) {
        if (display == null) {
            display = getCharSequence();
        }
        return display;
    }

    @Override
    public void execute(Context context) {

    }
}
