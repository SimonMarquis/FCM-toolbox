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

package fr.smarquis.fcm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import fr.smarquis.fcm.payloads.Link;
import fr.smarquis.fcm.payloads.Payload;
import fr.smarquis.fcm.payloads.Text;


public class FcmService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        Token.broadcast(this, token);
    }

    @Override
    @WorkerThread
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Message<Payload> message = Message.from(remoteMessage);
        boolean silent = Boolean.valueOf(remoteMessage.getData().get("hide"));
        new Handler(Looper.getMainLooper()).post(() -> notifyAndExecute(message, silent, this));
    }

    @UiThread
    private void notifyAndExecute(Message<Payload> message, boolean silent, Context context) {
        if (!silent) {
            Notifications.show(context, message);
        }
        Messages.instance(context).add(message);
        Payload payload = message.payload();
        if (payload instanceof Link) {
            Link link = (Link) payload;
            if (link.open()) {
                startActivity(link.intent());
            }
        } else if (payload instanceof Text) {
            Text text = (Text) payload;
            if (text.clipboard()) {
                text.copyToClipboard(this);
            }
        }
    }

}
