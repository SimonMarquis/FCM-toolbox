package fr.smarquis.fcm.payloads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import fr.smarquis.fcm.Message;
import fr.smarquis.fcm.Messages;
import fr.smarquis.fcm.R;
import fr.smarquis.fcm.TimeAgoTextView;
import fr.smarquis.fcm.Util;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public final class ViewHolder extends RecyclerView.ViewHolder {

    public interface OnClickListener {
        void onClick(@Nullable Message<Payload> message);
    }

    enum Action {
        PRIMARY,
        SECONDARY,
        TERTIARY
    }

    @Nullable
    private Message<Payload> message;

    private boolean selected = false;

    private final ImageView icon;

    private final TimeAgoTextView timestamp;

    private final TextView raw, text;

    private final Button button1, button2, button3;

    private final View selector;

    public ViewHolder(@NonNull View itemView, @NonNull OnClickListener listener) {
        super(itemView);
        selector = itemView.findViewById(R.id.item_selector);
        icon = itemView.findViewById(R.id.item_icon);
        timestamp = itemView.findViewById(R.id.item_timestamp);
        raw = itemView.findViewById(R.id.item_raw);
        text = itemView.findViewById(R.id.item_text);
        button1 = itemView.findViewById(R.id.item_btn_1);
        button2 = itemView.findViewById(R.id.item_btn_2);
        button3 = itemView.findViewById(R.id.item_btn_3);
        button1.setOnClickListener(v -> execute(Action.PRIMARY, payload()));
        button2.setOnClickListener(v -> execute(Action.SECONDARY, payload()));
        button3.setOnClickListener(v -> execute(Action.TERTIARY, payload()));
        itemView.setOnClickListener(v -> listener.onClick(message));
    }

    @Nullable
    private Payload payload() {
        return message != null ? message.payload() : null;
    }

    private void renderContent() {
        selector.setActivated(selected);
        if (selected) {
            text.setText(null);
            text.setVisibility(GONE);
            Map data = message != null ? message.data() : null;
            String display = data != null ? Messages.moshi().adapter(Message.class).indent("  ").toJson(message) : null;
            raw.setText(display);
            raw.setVisibility(TextUtils.isEmpty(display) ? GONE : VISIBLE);
        } else {
            Payload payload = payload();
            CharSequence display = payload != null ? payload.display() : null;
            text.setText(display);
            text.setVisibility(TextUtils.isEmpty(display) ? GONE : VISIBLE);
            raw.setText(null);
            raw.setVisibility(GONE);
        }
    }

    private void render(@NonNull Action action, @NonNull Button button, @Nullable Payload payload) {
        if (payload instanceof App) {
            switch (action) {
                case PRIMARY:
                    button.setVisibility(VISIBLE);
                    button.setText(R.string.payload_app_store);
                    return;
                case SECONDARY:
                    button.setText(R.string.payload_app_uninstall);
                    button.setVisibility(((App) payload).isInstalled(itemView.getContext()) ? VISIBLE : GONE);
                    return;
            }
        } else if (payload instanceof Link) {
            if (action == Action.PRIMARY) {
                button.setVisibility(VISIBLE);
                button.setText(R.string.payload_link_open);
                return;
            }
        } else if (payload instanceof Text) {
            if (action == Action.PRIMARY) {
                button.setVisibility(VISIBLE);
                button.setText(R.string.payload_text_copy);
                return;
            }
        }

        button.setVisibility(GONE);
        button.setText(null);
    }

    private void execute(@NonNull Action action, @Nullable Payload payload) {
        Context context = itemView.getContext();
        if (payload instanceof App) {
            App app = (App) payload;
            switch (action) {
                case PRIMARY:
                    Util.safeStartActivity(context, app.playStore());
                    break;
                case SECONDARY:
                    Util.safeStartActivity(context, app.uninstall());
                    break;
            }
        } else if (payload instanceof Link) {
            if (action == Action.PRIMARY) {
                Util.safeStartActivity(context, ((Link) payload).intent());
            }
        } else if (payload instanceof Text) {
            if (action == Action.PRIMARY) {
                Util.copyToClipboard(context, ((Text) payload).text);
            }
        }
    }

    public void onBind(@NonNull Message<Payload> message, boolean selected) {
        this.message = message;
        this.selected = selected;
        icon.setImageResource(message.payload().icon());
        timestamp.setTimestamp(Math.min(message.sentTime(), System.currentTimeMillis()));
        renderContent();
        renderButtons();
    }

    private void renderButtons() {
        Payload payload = message != null ? message.payload() : null;
        render(Action.PRIMARY, button1, payload);
        render(Action.SECONDARY, button2, payload);
        render(Action.TERTIARY, button3, payload);
    }

    public void onUnbind() {
        this.message = null;
        timestamp.setTimestamp(TimeAgoTextView.NO_TIMESTAMP);
    }

}
