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

import android.app.Application;
import android.os.Build;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Locale;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final FirebaseMessaging instance = FirebaseMessaging.getInstance();
        instance.subscribeToTopic("all");
        instance.subscribeToTopic(Util.safeReplaceToAlphanum(Build.DEVICE.toLowerCase()));
    }

}
