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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import fr.smarquis.fcm.R;
import fr.smarquis.fcm.Truss;

public class AppPayload extends Payload {

    static final String KEY = "app";
    private final String packageName;
    private final String title;

    private AppPayload(RemoteMessage message, String title, String packageName) {
        super(message);
        this.title = title;
        this.packageName = packageName;
    }

    static AppPayload create(RemoteMessage message) throws JSONException {
        final String data = extractPayloadData(message, KEY);
        JSONObject json = new JSONObject(data);
        String title = json.optString("title");
        String packageName = json.optString("package");
        return new AppPayload(message, title, packageName);
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
        final Intent installIntent = getInstallIntent();
        final Intent removeIntent = getRemoveIntent();
        final NotificationCompat.Builder builder = getNotificationBuilder(context, message)
                .setSmallIcon(R.drawable.ic_shop_24dp)
                .setContentTitle(TextUtils.isEmpty(title) ? "App" : title)
                .setContentText(packageName)
                .addAction(0, context.getString(R.string.payload_app_install), PendingIntent.getActivity(context, 0, installIntent, 0))
                .addAction(0, context.getString(R.string.payload_app_remove), PendingIntent.getActivity(context, 0, removeIntent, 0));
        showNotification(context, builder.build(), String.valueOf(timestamp), AppPayload.class.hashCode());
    }

    public Intent getInstallIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public Intent getRemoveIntent() {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void cancelNotification(Context context) {
        cancelNotification(context, String.valueOf(timestamp), AppPayload.class.hashCode());
    }

    @Override
    public CharSequence getFormattedCharSequence(Context context) {
        if (display == null) {
            display = new Truss()
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("title: ").popSpan().append(title).append('\n')
                    .pushSpan(new StyleSpan(android.graphics.Typeface.BOLD)).append("package: ").popSpan().append(packageName)
                    .build();
        }
        return display;
    }

    @Override
    public void execute(Context context) {

    }
}
