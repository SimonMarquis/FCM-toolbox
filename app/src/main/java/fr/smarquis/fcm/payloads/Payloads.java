package fr.smarquis.fcm.payloads;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.smarquis.fcm.Messages;

public final class Payloads {

    public static final PolymorphicJsonAdapterFactory JSON_ADAPTER = PolymorphicJsonAdapterFactory.of(Payload.class, "type")
            .withSubtype(App.class, App.KEY)
            .withSubtype(Link.class, Link.KEY)
            .withSubtype(Ping.class, Ping.KEY)
            .withSubtype(Raw.class, Raw.KEY)
            .withSubtype(Text.class, Text.KEY);

    private static final Map<String, Class<? extends Payload>> CLASSES = new HashMap<String, Class<? extends Payload>>() {{
        put(App.KEY, App.class);
        put(Link.KEY, Link.class);
        put(Ping.KEY, Ping.class);
        put(Text.KEY, Text.class);
    }};

    private Payloads() {
    }

    @NonNull
    public static Payload extract(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        Set<Map.Entry<String, String>> entries = data.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            Class<? extends Payload> clazz = CLASSES.get(key);
            if (clazz != null) {
                try {
                    JsonAdapter<? extends Payload> adapter = Messages.moshi().adapter(clazz);
                    Payload payload = adapter.fromJson(value);
                    if (payload != null) {
                        return payload;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new Raw(data);
    }

}
