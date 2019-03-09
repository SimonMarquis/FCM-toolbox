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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import fr.smarquis.fcm.payloads.Payload;
import fr.smarquis.fcm.payloads.ViewHolder;

class MessageAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final Map<String, Long> stableIds = new HashMap<>();

    private static final Map<Message, Boolean> selection = new HashMap<>();

    @NonNull
    private final List<Message<Payload>> messages = new ArrayList<>();

    MessageAdapter(@NonNull List<Message<Payload>> messages) {
        this.messages.addAll(messages);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payload, parent, false);
        return new ViewHolder(view, this::onClick);
    }

    private void onClick(@Nullable Message<Payload> message) {
        if (message == null) {
            return;
        }
        Boolean value = selection.get(message);
        selection.put(message, value != null ? Boolean.valueOf(!value) : Boolean.TRUE);
        notifyItemChanged(messages.indexOf(message));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Message<Payload> message = messages.get(position);
        Boolean selected = selection.get(message);
        viewHolder.onBind(message, selected != null ? selected : Boolean.FALSE);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.onUnbind();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public long getItemId(int position) {
        String key = messages.get(position).id();
        Long value = stableIds.get(key);
        if (value == null) {
            value = (long) stableIds.size();
            stableIds.put(key, value);
        }
        return value;
    }

    Message<Payload> removeItemAtPosition(int position) {
        Message<Payload> message = messages.remove(position);
        notifyItemRemoved(position);
        return message;
    }

    void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    int add(@NonNull Message<Payload> message) {
        messages.add(0, message);
        Collections.sort(messages);
        int indexOf = messages.indexOf(message);
        notifyItemInserted(indexOf);
        return indexOf;
    }

}
