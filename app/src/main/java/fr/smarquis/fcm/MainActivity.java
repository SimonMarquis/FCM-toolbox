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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import fr.smarquis.fcm.payloads.Payload;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity implements Messages.Listener {

    private PresenceEventListener presence;

    private MessageAdapter adapter;

    private final BroadcastReceiver tokenReceiver = Token.create(this::applyToken);

    private final BroadcastReceiver presenceReceiver = Presence.create(presence -> supportInvalidateOptionsMenu());

    private View emptyView;

    private RecyclerView recyclerView;

    private Messages messages;

    @Nullable
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presence = PresenceEventListener.instance(this);
        messages = Messages.instance(this);
        initRecyclerView();
        registerReceivers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        presence.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        presence.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Notifications.removeAll(this);
        fetchToken();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    private void fetchToken() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(R.string.fetching);
        }
        Task<InstanceIdResult> instanceId = FirebaseInstanceId.getInstance().getInstanceId();
        instanceId.addOnSuccessListener(this, instanceIdResult -> applyToken(instanceIdResult.getToken()));
    }

    private void applyToken(String token) {
        this.token = token;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(token);
        }
        supportInvalidateOptionsMenu();
    }

    private void registerReceivers() {
        Token.register(this, tokenReceiver);
        Presence.register(this, presenceReceiver);
        messages.register(this);
    }

    private void unregisterReceivers() {
        Token.unregister(this, tokenReceiver);
        Presence.unregister(this, presenceReceiver);
        messages.unregister(this);
    }

    private void initRecyclerView() {
        emptyView = findViewById(R.id.empty_view);
        recyclerView = findViewById(R.id.recycler_view);
        Resources resources = getResources();
        int horizontal = resources.getDimensionPixelSize(R.dimen.unit_4);
        int vertical = resources.getDimensionPixelSize(R.dimen.unit_1);
        recyclerView.addItemDecoration(new SpacingItemDecoration(horizontal, vertical));
        recyclerView.setHasFixedSize(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messages.get());
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int position = viewHolder.getAdapterPosition();
                Message<Payload> removed = adapter.removeItemAtPosition(position);
                messages.remove(removed);
                onAdapterCountMightHaveChanged();
                String message = getString(R.string.snackbar_item_deleted, 1);
                Snackbar snackbar = Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.snackbar_item_undo, v -> messages.add(removed)).show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        onAdapterCountMightHaveChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_presence).setIcon(presence.isConnected() ? android.R.drawable.presence_online : android.R.drawable.presence_invisible);
        menu.findItem(R.id.action_share_token).setVisible(!TextUtils.isEmpty(token));
        menu.findItem(R.id.action_delete_all).setVisible(adapter != null && adapter.getItemCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_token:
                if (token != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, token);
                    startActivity(Intent.createChooser(intent, getString(R.string.menu_share_token)));
                }
                return true;
            case R.id.action_invalidate_token:
                AsyncTask.execute(() -> {
                    try {
                        FirebaseInstanceId.getInstance().deleteInstanceId();
                        runOnUiThread(() -> {
                            presence.reset();
                            finish();
                            startActivity(getIntent());
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case R.id.action_topics:
                // Extracted from com.google.firebase.messaging.FirebaseMessaging
                Pattern pattern = Pattern.compile("[a-zA-Z0-9-_.~%]{1,900}");
                @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.topics_dialog, null, false);
                EditText input = view.findViewById(R.id.input);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.menu_topics)
                        .setView(view)
                        .setPositiveButton(R.string.topics_subscribe, (d, which) -> {
                            String topic = input.getText().toString();
                            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                    .addOnSuccessListener(this, success -> Toast.makeText(this, getString(R.string.topics_subscribed, topic), LENGTH_LONG).show())
                                    .addOnFailureListener(this, error -> Toast.makeText(this, Util.printStackTrace(error), LENGTH_LONG).show());
                        })
                        .setNegativeButton(R.string.topics_unsubscribe, (d, which) -> {
                            String topic = input.getText().toString();
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                                    .addOnSuccessListener(this, success -> Toast.makeText(this, getString(R.string.topics_unsubscribed, topic), LENGTH_LONG).show())
                                    .addOnFailureListener(this, error -> Toast.makeText(this, Util.printStackTrace(error), LENGTH_LONG).show());
                        }).show();
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        boolean matches = pattern.matcher(s).matches();
                        dialog.getButton(BUTTON_POSITIVE).setEnabled(matches);
                        dialog.getButton(BUTTON_NEGATIVE).setEnabled(matches);
                    }
                });
                // Trigger afterTextChanged()
                input.setText(null);
                return true;
            case R.id.action_delete_all:
                Notifications.removeAll(this);
                List<Message<Payload>> removedMessages = messages.get();
                messages.clear();
                adapter.clear();
                String message = getString(R.string.snackbar_item_deleted, removedMessages.size());
                Snackbar.make(recyclerView, message, (int) TimeUnit.SECONDS.toMillis(10))
                        .setAction(R.string.snackbar_item_undo, v -> messages.add(removedMessages))
                        .show();
                onAdapterCountMightHaveChanged();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewMessage(@NonNull Message<Payload> message) {
        int index = adapter.add(message);
        recyclerView.smoothScrollToPosition(index);
        onAdapterCountMightHaveChanged();
    }

    private void onAdapterCountMightHaveChanged() {
        int count = adapter != null ? adapter.getItemCount() : 0;
        emptyView.setVisibility(count > 0 ? View.INVISIBLE : View.VISIBLE);
        supportInvalidateOptionsMenu();
    }
}
