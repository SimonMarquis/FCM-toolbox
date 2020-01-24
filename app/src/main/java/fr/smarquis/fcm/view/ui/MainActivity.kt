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
package fr.smarquis.fcm.view.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import fr.smarquis.fcm.R
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.model.Presence
import fr.smarquis.fcm.utils.Notifications.removeAll
import fr.smarquis.fcm.utils.asString
import fr.smarquis.fcm.utils.safeStartActivity
import fr.smarquis.fcm.view.adapter.MessagesAdapter
import fr.smarquis.fcm.viewmodel.MessagesViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.get
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private val viewModel: MessagesViewModel by viewModel()

    private val messagesAdapter: MessagesAdapter = MessagesAdapter(get())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.presence.observe(this, Observer { updatePresence(it) })
        viewModel.messages.observe(this, Observer { updateMessages(it) })

        messagesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart != 0 || itemCount > 1) return
                recycler_view.post { recycler_view.smoothScrollToPosition(0) }
            }
        })
        recycler_view.apply {
            setHasFixedSize(true)
            val horizontal = resources.getDimensionPixelSize(R.dimen.unit_4)
            val vertical = resources.getDimensionPixelSize(R.dimen.unit_1)
            addItemDecoration(SpacingItemDecoration(horizontal, vertical))
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val message = messagesAdapter.getItem(viewHolder.adapterPosition)
                    viewModel.delete(message)
                    Snackbar.make(recycler_view, getString(R.string.snackbar_item_deleted, 1), Snackbar.LENGTH_LONG).setAction(R.string.snackbar_item_undo) {
                        viewModel.insert(message)
                    }.show()
                }
            }).attachToRecyclerView(this)
            adapter = messagesAdapter
        }

    }

    private fun updateMessages(messages: List<Message>) {
        messagesAdapter.submitList(messages) {
            empty_view.visibility = if (messagesAdapter.itemCount > 0) INVISIBLE else VISIBLE
            invalidateOptionsMenu()
        }
    }

    private fun updatePresence(presence: Presence) {
        supportActionBar?.subtitle = when (presence.token) {
            null -> getString(R.string.fetching)
            else -> presence.token
        }
        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        removeAll(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_presence).setIcon(if (viewModel.presence.value.connected) android.R.drawable.presence_online else android.R.drawable.presence_invisible)
        menu.findItem(R.id.action_share_token).isVisible = !viewModel.presence.value.token.isNullOrEmpty()
        menu.findItem(R.id.action_delete_all).isVisible = messagesAdapter.itemCount > 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share_token -> shareToken()
            R.id.action_invalidate_token -> viewModel.presence.resetToken()
            R.id.action_topics -> showTopicsDialog()
            R.id.action_delete_all -> showDeleteDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteDialog() {
        val messages = messagesAdapter.currentList.toTypedArray<Message>()
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.dialog_delete_all_title, messages.size))
                .setPositiveButton(R.string.dialog_delete_all_positive) { _: DialogInterface?, _: Int ->
                    viewModel.delete(*messages)
                    removeAll(this)
                    Snackbar.make(recycler_view, getString(R.string.snackbar_item_deleted, messages.size), TimeUnit.SECONDS.toMillis(10).toInt())
                            .setAction(R.string.snackbar_item_undo) { viewModel.insert(*messages) }
                            .show()
                }
                .setNegativeButton(R.string.dialog_delete_all_negative) { _: DialogInterface?, _: Int -> }
                .show()
    }

    private fun shareToken() {
        val token = viewModel.presence.value.token ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, token)
        }
        safeStartActivity(Intent.createChooser(intent, getString(R.string.menu_share_token)))
    }

    private fun showTopicsDialog() {
        // Extracted from com.google.firebase.messaging.FirebaseMessaging
        val pattern = Pattern.compile("[a-zA-Z0-9-_.~%]{1,900}")
        @SuppressLint("InflateParams") val view = LayoutInflater.from(this).inflate(R.layout.topics_dialog, null, false)
        val input = view.findViewById<EditText>(R.id.dialog_input_value)
        val messaging = FirebaseMessaging.getInstance()
        val dialog = AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.topics_subscribe) { _: DialogInterface?, _: Int ->
                    val topic = input.text.toString()
                    messaging.subscribeToTopic(topic)
                            .addOnSuccessListener(this) { Toast.makeText(this, getString(R.string.topics_subscribed, topic), LENGTH_LONG).show() }
                            .addOnFailureListener(this) { Toast.makeText(this, it.asString(), LENGTH_LONG).show() }
                }
                .setNegativeButton(R.string.topics_unsubscribe) { _: DialogInterface?, _: Int ->
                    val topic = input.text.toString()
                    messaging.unsubscribeFromTopic(topic)
                            .addOnSuccessListener(this) { Toast.makeText(this, getString(R.string.topics_unsubscribed, topic), LENGTH_LONG).show() }
                            .addOnFailureListener(this) { Toast.makeText(this, it.asString(), LENGTH_LONG).show() }
                }.show()
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val matches = pattern.matcher(s).matches()
                dialog.getButton(BUTTON_POSITIVE).isEnabled = matches
                dialog.getButton(BUTTON_NEGATIVE).isEnabled = matches
            }
        })
        // Trigger afterTextChanged()
        input.text = null
        input.requestFocus()
    }

}