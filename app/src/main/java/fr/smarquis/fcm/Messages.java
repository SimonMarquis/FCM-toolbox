package fr.smarquis.fcm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.smarquis.fcm.payloads.Payload;
import fr.smarquis.fcm.payloads.Payloads;

public final class Messages {

    private static final String SHARED_PREFERENCES_NAME = "messages";

    private static final String KEY = "data";

    private static final Moshi MOSHI = new Moshi.Builder().add(Payloads.JSON_ADAPTER).build();

    @SuppressLint("StaticFieldLeak")
    private static volatile Messages instance = null;

    private final SharedPreferences sp;

    @NonNull
    private final List<Message<Payload>> messages = new ArrayList<>();

    private static final Type TYPE = Types.newParameterizedType(List.class, Message.class);

    private static final JsonAdapter<List<Message<Payload>>> ADAPTER = MOSHI.adapter(TYPE);

    private final List<Listener> listeners = Collections.synchronizedList(new ArrayList<>());

    interface Listener {
        void onNewMessage(@NonNull Message<Payload> message);
    }

    public static Moshi moshi() {
        return MOSHI;
    }

    @NonNull
    static Messages instance(@NonNull Context context) {
        if (instance == null) {
            synchronized (Messages.class) {
                if (instance == null) {
                    instance = new Messages(context);
                }
            }
        }
        return instance;
    }

    private Messages(@NonNull Context context) {
        sp = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String value = sp.getString(KEY, null);
        if (!TextUtils.isEmpty(value)) {
            try {
                List<Message<Payload>> list = ADAPTER.fromJson(value);
                if (list != null) {
                    messages.addAll(list);
                    Collections.sort(messages);
                }
            } catch (IOException e) {
                e.printStackTrace();
                sp.edit().remove(KEY).apply();
            }
        }
    }

    void register(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    void unregister(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @NonNull
    List<Message<Payload>> get() {
        return new ArrayList<>(messages);
    }

    void add(@NonNull Message<Payload> message) {
        if (messages.contains(message)) {
            return;
        }
        messages.add(message);
        Collections.sort(messages);
        persist();
        notifyListeners(message);
    }

    void add(@NonNull List<Message<Payload>> messages) {
        this.messages.addAll(messages);
        Collections.sort(this.messages);
        persist();
        for (int i = messages.size() - 1; i >= 0; i--) {
            notifyListeners(messages.get(i));
        }
    }

    private void notifyListeners(@NonNull Message<Payload> message) {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.onNewMessage(message);
            }
        }
    }

    void remove(@NonNull Message message) {
        if (!messages.contains(message)) {
            return;
        }
        messages.remove(message);
        persist();
    }

    void clear() {
        if (messages.isEmpty()) {
            return;
        }
        messages.clear();
        persist();
    }

    private void persist() {
        sp.edit().putString(KEY, ADAPTER.toJson(messages)).apply();
    }

}
