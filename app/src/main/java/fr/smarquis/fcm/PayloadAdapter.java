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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.smarquis.fcm.payload.AppPayload;
import fr.smarquis.fcm.payload.LinkPayload;
import fr.smarquis.fcm.payload.Payload;
import fr.smarquis.fcm.payload.PingPayload;
import fr.smarquis.fcm.payload.RawPayload;
import fr.smarquis.fcm.payload.TextPayload;

public class PayloadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @NonNull
    private final List<Payload> payloads = new ArrayList<>();

    PayloadAdapter(List<Payload> payloads) {
        this.payloads.addAll(payloads);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        switch (viewType) {
            case PingViewHolder.LAYOUT:
                return new PingViewHolder(v);
            case TextViewHolder.LAYOUT:
                return new TextViewHolder(v);
            case LinkViewHolder.LAYOUT:
                return new LinkViewHolder(v);
            case AppViewHolder.LAYOUT:
                return new AppViewHolder(v);
            case RawViewHolder.LAYOUT:
            default:
                return new RawViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        final Payload payload = payloads.get(position);
        switch (viewHolder.getItemViewType()) {
            case PingViewHolder.LAYOUT:
                ((PingViewHolder) viewHolder).onBind(payload);
                return;
            case TextViewHolder.LAYOUT:
                ((TextViewHolder) viewHolder).onBind(payload);
                return;
            case LinkViewHolder.LAYOUT:
                ((LinkViewHolder) viewHolder).onBind(payload);
                return;
            case AppViewHolder.LAYOUT:
                ((AppViewHolder) viewHolder).onBind(payload);
                return;
            case RawViewHolder.LAYOUT:
            default:
                ((RawViewHolder) viewHolder).onBind(payload);
                return;
        }
    }

    @Override
    public int getItemViewType(int position) {
        final Payload payload = payloads.get(position);
        if (payload instanceof PingPayload) {
            return PingViewHolder.LAYOUT;
        } else if (payload instanceof TextPayload) {
            return TextViewHolder.LAYOUT;
        } else if (payload instanceof LinkPayload) {
            return LinkViewHolder.LAYOUT;
        } else if (payload instanceof AppPayload) {
            return AppViewHolder.LAYOUT;
        } else if (payload instanceof RawPayload || payload != null) {
            return RawViewHolder.LAYOUT;
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return payloads.size();
    }

    Payload getItem(int position) {
        return payloads.get(position);
    }

    void removeItemAtPosition(int position) {
        payloads.remove(position);
        notifyItemRemoved(position);
    }

    void removeAll() {
        int size = payloads.size();
        payloads.clear();
        notifyItemRangeRemoved(0, size);
    }

    void addPayload(Payload payload) {
        payloads.add(0, payload);
        notifyItemInserted(0);
    }

    static class PingViewHolder extends RecyclerView.ViewHolder {

        static final int LAYOUT = R.layout.item_payload_ping;

        private TextView timestamp;

        PingViewHolder(View v) {
            super(v);
            timestamp = v.findViewById(R.id.item_timestamp);
        }

        void onBind(Payload payload) {
            timestamp.setText(payload.getFormattedTimestamp());
        }

    }

    static class TextViewHolder extends RecyclerView.ViewHolder {

        static final int LAYOUT = R.layout.item_payload_text;

        private Context context;

        private TextView timestamp, text;

        private Button button;

        TextViewHolder(View v) {
            super(v);
            context = v.getContext();
            timestamp = v.findViewById(R.id.item_timestamp);
            text = v.findViewById(R.id.item_text);
            button = v.findViewById(R.id.item_text_copy);
        }

        void onBind(final Payload payload) {
            timestamp.setText(payload.getFormattedTimestamp());
            text.setText(payload.getFormattedCharSequence(context));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Util.copyToClipboard(view.getContext(), ((TextPayload) payload).text);
                }
            });
        }
    }

    static class RawViewHolder extends RecyclerView.ViewHolder {

        static final int LAYOUT = R.layout.item_payload_raw;

        private Context context;

        private TextView timestamp, text;

        RawViewHolder(View v) {
            super(v);
            context = v.getContext();
            timestamp = v.findViewById(R.id.item_timestamp);
            text = v.findViewById(R.id.item_text);
        }

        void onBind(Payload payload) {
            timestamp.setText(payload.getFormattedTimestamp());
            text.setText(payload.getFormattedCharSequence(context));
        }
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {

        static final int LAYOUT = R.layout.item_payload_link;

        private Context context;

        private TextView timestamp, text;

        private Button button;

        LinkViewHolder(View v) {
            super(v);
            context = v.getContext();
            timestamp = v.findViewById(R.id.item_timestamp);
            text = v.findViewById(R.id.item_text);
            button = v.findViewById(R.id.item_link_open);
        }

        public Context getContext() {
            return context;
        }

        void onBind(final Payload payload) {
            timestamp.setText(payload.getFormattedTimestamp());
            text.setText(payload.getFormattedCharSequence(context));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        view.getContext().startActivity(((LinkPayload) payload).getIntent());
                    } catch (Exception e) {
                        Toast.makeText(view.getContext(), Util.printStackTrace(e), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {

        static final int LAYOUT = R.layout.item_payload_app;

        private Context context;

        private TextView timestamp, text;

        private Button buttonRemove;

        private Button buttonInstall;

        AppViewHolder(View v) {
            super(v);
            context = v.getContext();
            timestamp = v.findViewById(R.id.item_timestamp);
            text = v.findViewById(R.id.item_text);
            buttonRemove = v.findViewById(R.id.item_app_remove);
            buttonInstall = v.findViewById(R.id.item_app_install);
        }

        void onBind(final Payload payload) {
            timestamp.setText(payload.getFormattedTimestamp());
            text.setText(payload.getFormattedCharSequence(context));
            buttonInstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        view.getContext().startActivity(((AppPayload) payload).getInstallIntent());
                    } catch (Exception e) {
                        Toast.makeText(view.getContext(), Util.printStackTrace(e), Toast.LENGTH_LONG).show();
                    }
                }
            });
            buttonRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        view.getContext().startActivity(((AppPayload) payload).getRemoveIntent());
                    } catch (Exception e) {
                        Toast.makeText(view.getContext(), Util.printStackTrace(e), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
