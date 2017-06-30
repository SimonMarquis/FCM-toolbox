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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

class TokenFetcher extends AsyncTask<Void, Void, String> {

    private static final String INTENT_ACTION = "TokenFetcher.Update";
    static final IntentFilter INTENT_FILTER = new IntentFilter(INTENT_ACTION);
    private static final Intent INTENT = new Intent(INTENT_ACTION);
    private final DatabaseReference ref;

    @SuppressLint("StaticFieldLeak")
    private final Context context;

    TokenFetcher(Context context, DatabaseReference ref) {
        this.context = context.getApplicationContext();
        this.ref = ref;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return FirebaseInstanceId.getInstance().getToken(context.getString(R.string.gcm_defaultSenderId), FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String token) {
        super.onPostExecute(token);
        if (token != null) {
            PresenceEventListener.updateConnectionReference(ref, token);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(INTENT);
    }
}
