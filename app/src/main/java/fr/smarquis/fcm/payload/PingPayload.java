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

import com.google.firebase.messaging.RemoteMessage;

import fr.smarquis.fcm.R;

public class PingPayload extends Payload {

    static final String KEY = "ping";

    private PingPayload(RemoteMessage message) {
        super(message);
    }

    static PingPayload create(RemoteMessage message) {
        return new PingPayload(message);
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
        final Notification notification = getNotificationBuilder(context, message)
                .setSmallIcon(R.drawable.ic_notifications_none_24dp)
                .setContentTitle("Ping")
                .build();
        showNotification(context, notification, R.id.ping_notification_id);
    }

    @Override
    public void cancelNotification(Context context) {
        cancelNotification(context, R.id.ping_notification_id);
    }

    @Override
    public CharSequence getFormattedCharSequence(Context context) {
        return null;
    }

    @Override
    public void execute(Context context) {

    }
}
