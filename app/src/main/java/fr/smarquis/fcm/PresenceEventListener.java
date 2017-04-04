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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Locale;

class PresenceEventListener implements ValueEventListener {

    private static final String INTENT_ACTION = "PresenceEventListener.Update";
    static final IntentFilter INTENT_FILTER = new IntentFilter(INTENT_ACTION);
    private static final Intent INTENT = new Intent(INTENT_ACTION);
    private Context context;

    private DatabaseReference presenceReference;

    private DatabaseReference connectionRef;

    private boolean isConnected = false;

    PresenceEventListener(Context context) {
        this.context = context.getApplicationContext();
        String uuid = Uuid.get(context);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        presenceReference = database.getReference(".info/connected");
        connectionRef = database.getReference("devices/" + uuid);
    }

    static void updateConnectionReference(DatabaseReference connectionRef, String token) {
        HashMap<String, Object> result = new HashMap<>(3);
        Locale locale = Locale.getDefault();
        final String displayName = Build.MODEL.toLowerCase(locale).startsWith(Build.MANUFACTURER.toLowerCase(locale)) ? Build.MODEL
                : Build.MANUFACTURER.toUpperCase(locale) + " " + Build.MODEL;
        result.put("name", displayName);
        result.put("token", token);
        result.put("timestamp", ServerValue.TIMESTAMP);
        connectionRef.setValue(result);
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        boolean connected = snapshot.getValue(Boolean.class);
        if (connected) {
            final String token = FirebaseInstanceId.getInstance().getToken();
            if (TextUtils.isEmpty(token)) {
                new TokenFetcher(context, connectionRef).execute();
            } else {
                updateConnectionReference(connectionRef, token);
            }
            connectionRef.onDisconnect().removeValue();
        }
        isConnected = connected;
        LocalBroadcastManager.getInstance(context).sendBroadcast(INTENT);
    }

    @Override
    public void onCancelled(DatabaseError error) {
    }

    void register() {
        DatabaseReference.goOnline();
        presenceReference.addValueEventListener(this);
    }

    void unregister() {
        presenceReference.removeEventListener(this);
        connectionRef.removeValue();
        DatabaseReference.goOffline();
    }

    boolean isConnected() {
        return isConnected;
    }
}
