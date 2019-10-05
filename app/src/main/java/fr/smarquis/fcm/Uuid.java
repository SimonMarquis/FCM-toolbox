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
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

final class Uuid {

    private static final String KEY = "uuid";

    /**
     * @return UUID.randomUUID() or a previously generated UUID, stored in SharedPreferences
     */
    static String get(@NonNull Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (prefs.contains(KEY)) {
            final String value = prefs.getString(KEY, null);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        final String uuid = UUID.randomUUID().toString();
        prefs.edit().putString(KEY, uuid).apply();
        return uuid;
    }

}
