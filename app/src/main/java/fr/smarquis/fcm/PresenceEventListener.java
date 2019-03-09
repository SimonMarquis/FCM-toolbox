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

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;

import static android.os.Build.MANUFACTURER;
import static android.os.Build.MODEL;

class PresenceEventListener implements ValueEventListener {

    @SuppressLint("StaticFieldLeak")
    private static volatile PresenceEventListener instance;

    private final Context context;

    private final DatabaseReference presenceRef;

    private final DatabaseReference connectionRef;

    private boolean isConnected = false;

    @NonNull
    static PresenceEventListener instance(@NonNull Context context) {
        if (instance == null) {
            synchronized (Messages.class) {
                if (instance == null) {
                    instance = new PresenceEventListener(context);
                }
            }
        }
        return instance;
    }

    private PresenceEventListener(Context context) {
        this.context = context.getApplicationContext();
        String uuid = Uuid.get(context);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        presenceRef = database.getReference(".info/connected");
        connectionRef = database.getReference("devices/" + uuid);
        connectionRef.onDisconnect().removeValue();
    }

    boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        if (isConnected) {
            isConnected = false;
            Presence.broadcast(context, false);
        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
        if (connected == isConnected) {
            return;
        }
        isConnected = connected;
        Presence.broadcast(context, isConnected);
        if (connected) {
            Task<InstanceIdResult> instanceId = FirebaseInstanceId.getInstance().getInstanceId();
            instanceId.addOnSuccessListener(instanceIdResult -> setConnectionReference(instanceIdResult.getToken()));
        }
    }

    private void setConnectionReference(String token) {
        HashMap<String, Object> result = new HashMap<>(3);
        Locale locale = Locale.getDefault();
        final String displayName = MODEL.toLowerCase(locale).startsWith(MANUFACTURER.toLowerCase(locale)) ? MODEL : MANUFACTURER.toUpperCase(locale) + " " + MODEL;
        result.put("name", displayName);
        result.put("token", token);
        result.put("timestamp", ServerValue.TIMESTAMP);
        connectionRef.setValue(result);
    }

    @NonNull
    private final Set<Object> listeners = new HashSet<>();

    void register(Object listener) {
        synchronized (listeners) {
            boolean empty = listeners.isEmpty();
            listeners.add(listener);
            if (empty) {
                goOnline();
            }
        }
    }

    void unregister(Object listener) {
        synchronized (listeners) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                goOffline();
            }
        }
    }

    void reset() {
        goOffline();
        goOnline();
    }

    private void goOffline() {
        connectionRef.removeValue();
        DatabaseReference.goOffline();
        presenceRef.removeEventListener(this);
        isConnected = false;
    }

    private void goOnline() {
        presenceRef.addValueEventListener(this);
        DatabaseReference.goOnline();
    }

}
