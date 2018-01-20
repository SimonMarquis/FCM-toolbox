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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.iid.FirebaseInstanceId;

import fr.smarquis.fcm.payload.Payload;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private PayloadAdapter adapter;

    private PresenceEventListener presenceEventListener;

    private BroadcastReceiver tokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSubtitle();
        }
    };

    private BroadcastReceiver presenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSubtitle();
            supportInvalidateOptionsMenu();
        }
    };
    private View emptyView;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
        registerReceivers();
        updateSubtitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSubtitle();
    }

    private void registerReceivers() {
        Payload.registerOnSharedPreferenceChanges(this, this);
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(this);
        instance.registerReceiver(tokenReceiver, TokenFetcher.INTENT_FILTER);
        instance.registerReceiver(presenceReceiver, PresenceEventListener.INTENT_FILTER);
        presenceEventListener = new PresenceEventListener(this);
        presenceEventListener.register();
    }

    private void unregisterReceivers() {
        Payload.unregisterOnSharedPreferenceChanges(this, this);
        LocalBroadcastManager instance = LocalBroadcastManager.getInstance(this);
        instance.unregisterReceiver(tokenReceiver);
        instance.unregisterReceiver(presenceReceiver);
        presenceEventListener.unregister();
    }

    private void initRecyclerView() {
        emptyView = findViewById(R.id.empty_view);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new PayloadAdapter(Payload.fetchPayloads(this));
        recyclerView.setAdapter(adapter);
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int position = viewHolder.getAdapterPosition();
                Payload.remove(MainActivity.this, adapter.getItem(position));
                adapter.removeItemAtPosition(position);
                onAdapterCountMightHaveChanged();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        onAdapterCountMightHaveChanged();
    }

    private void updateSubtitle() {
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(FirebaseInstanceId.getInstance().getToken());
    }

    @Override
    protected void onDestroy() {
        unregisterReceivers();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_presence).setIcon(presenceEventListener.isConnected() ? android.R.drawable.presence_online : android.R.drawable.presence_invisible);
        menu.findItem(R.id.action_share_token).setVisible(!TextUtils.isEmpty(FirebaseInstanceId.getInstance().getToken()));
        menu.findItem(R.id.action_delete_all).setVisible(adapter != null && adapter.getItemCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_token:
                final String token = FirebaseInstanceId.getInstance().getToken();
                if (token != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, token);
                    startActivity(Intent.createChooser(intent, getString(R.string.menu_share_token)));
                }
                return true;
            case R.id.action_delete_all:
                Payload.removeAll(this);
                adapter.removeAll();
                onAdapterCountMightHaveChanged();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences.contains(key)) {
            final Payload payload = Payload.with(key, sharedPreferences.getString(key, ""));
            adapter.addPayload(payload);
            recyclerView.smoothScrollToPosition(0);
            onAdapterCountMightHaveChanged();
        }
    }

    void onAdapterCountMightHaveChanged() {
        int count = adapter != null ? adapter.getItemCount() : 0;
        emptyView.setVisibility(count > 0 ? View.INVISIBLE : View.VISIBLE);
        supportInvalidateOptionsMenu();
    }

}
