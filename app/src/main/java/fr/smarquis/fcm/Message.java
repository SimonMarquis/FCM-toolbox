package fr.smarquis.fcm;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;
import com.squareup.moshi.Json;

import java.util.Map;

import fr.smarquis.fcm.payloads.Payload;
import fr.smarquis.fcm.payloads.Payloads;

public class Message<P extends Payload> implements Comparable<Message<P>> {

    @Json(name = "from")
    private final String from;
    @Json(name = "to")
    private final String to;
    @Json(name = "data")
    private final Map<String, String> data;
    @Json(name = "collapseKey")
    private final String collapseKey;
    @Json(name = "messageId")
    private final String messageId;
    @Json(name = "messageType")
    private final String messageType;
    @Json(name = "sentTime")
    private final long sentTime;
    @Json(name = "ttl")
    private final int ttl;
    @Json(name = "priority")
    private final int priority;
    @Json(name = "originalPriority")
    private final int originalPriority;

    @Json(name = "payload")
    private final Payload payload;

    private Message(String from, String to, Map<String, String> data, String collapseKey, String messageId, String messageType, long sentTime, int ttl, int priority, int originalPriority, P payload) {
        this.from = from;
        this.to = to;
        this.data = data;
        this.collapseKey = collapseKey;
        this.messageId = messageId;
        this.messageType = messageType;
        this.sentTime = sentTime;
        this.ttl = ttl;
        this.priority = priority;
        this.originalPriority = originalPriority;
        this.payload = payload;
    }

    @NonNull
    static Message<Payload> from(@NonNull RemoteMessage message) {
        return new Message<>(
                message.getFrom(),
                message.getTo(),
                message.getData(),
                message.getCollapseKey(),
                message.getMessageId(),
                message.getMessageType(),
                message.getSentTime(),
                message.getTtl(),
                message.getOriginalPriority(),
                message.getPriority(),
                Payloads.extract(message));
    }

    public String id() {
        return messageId;
    }

    public Map<String, String> data() {
        return data;
    }

    @NonNull
    public Payload payload() {
        return payload;
    }

    public long sentTime() {
        return sentTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message<?> message = (Message<?>) o;

        return messageId != null ? messageId.equals(message.messageId) : message.messageId == null;
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }

    @Override
    public int compareTo(@NonNull Message<P> other) {
        return (other.sentTime() < sentTime()) ? -1 : ((other.sentTime() == sentTime()) ? id().compareTo(other.id()) : 1);
    }
}
