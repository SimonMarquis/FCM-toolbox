package fr.smarquis.fcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

@Keep
final class Token {

    private static final String INTENT_ACTION = Token.class.getName() + "Update";

    private static final Intent INTENT = new Intent(INTENT_ACTION);

    private static final IntentFilter INTENT_FILTER = new IntentFilter(INTENT_ACTION);

    private static final String EXTRA_KEY = "token";

    interface Handler {
        void handle(String token);
    }

    private static LocalBroadcastManager manager(@NonNull Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

    static void broadcast(@NonNull Context context, @NonNull String token) {
        Intent intent = new Intent(INTENT);
        intent.putExtra(EXTRA_KEY, token);
        manager(context).sendBroadcast(intent);
    }

    @NonNull
    static BroadcastReceiver create(Handler handler) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handler.handle(intent.getStringExtra(EXTRA_KEY));
            }
        };
    }

    static void register(@NonNull Context context, @NonNull BroadcastReceiver receiver) {
        manager(context).registerReceiver(receiver, INTENT_FILTER);
    }

    static void unregister(@NonNull Context context, @NonNull BroadcastReceiver receiver) {
        manager(context).unregisterReceiver(receiver);
    }

}
