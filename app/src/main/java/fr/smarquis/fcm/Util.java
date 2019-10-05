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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {

    static String printStackTrace(@NonNull Throwable exception) {
        StringWriter trace = new StringWriter();
        exception.printStackTrace(new PrintWriter(trace));
        return trace.toString();
    }

    public static void copyToClipboard(@NonNull Context context, @Nullable CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(null, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    public static void safeStartActivity(@NonNull Context context, @Nullable Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, Util.printStackTrace(e), Toast.LENGTH_LONG).show();
        }
    }
}
