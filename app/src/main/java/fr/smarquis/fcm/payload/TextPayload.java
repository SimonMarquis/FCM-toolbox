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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import fr.smarquis.fcm.CopyToClipboardActivity;
import fr.smarquis.fcm.R;
import fr.smarquis.fcm.Truss;

import static fr.smarquis.fcm.Util.copyToClipboard;

public class TextPayload extends Payload {

    static final String KEY = "text";
    public final String text;
    private final String title;
    private final boolean clipboard;

    private TextPayload(RemoteMessage message, String title, String text, boolean clipboard) {
        super(message);
        this.title = title;
        this.text = text;
        this.clipboard = clipboard;
    }

    static TextPayload create(RemoteMessage message) throws JSONException {
        final String data = extractPayloadData(message, KEY);
        JSONObject json = new JSONObject(data);
        final String title = json.optString("title");
        final String text = json.optString("message");
        final boolean clipboard = json.optBoolean("clipboard");
        return new TextPayload(message, title, text, clipboard);
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
        final Intent intent = new Intent(context, CopyToClipboardActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        final Notification notification = getNotificationBuilder(context, message)
                .setSmallIcon(R.drawable.ic_chat_24dp)
                .setContentTitle(TextUtils.isEmpty(title) ? "Text" : title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .addAction(0, context.getString(R.string.payload_text_copy), PendingIntent.getActivity(context, 0, intent, 0))
                .build();
        showNotification(context, notification, String.valueOf(timestamp), TextPayload.class.hashCode());
        if (clipboard) {
            final Context app = context.getApplicationContext();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    copyToClipboard(app.getApplicationContext(), text);
                }
            });
        }
    }

    @Override
    public void cancelNotification(Context context) {
        cancelNotification(context, String.valueOf(timestamp), TextPayload.class.hashCode());
    }

    @Override
    public CharSequence getFormattedCharSequence(Context context) {
        if (display == null) {
            display = new Truss()
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("title: ").popSpan().append(title).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("text: ").popSpan().append(text).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("clipboard: ").popSpan().append(String.valueOf(clipboard))
                    .build();
        }
        return display;
    }
}
