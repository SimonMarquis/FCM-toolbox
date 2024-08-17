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

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ComponentName
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.isInvisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import fr.smarquis.fcm.R
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.model.Presence
import fr.smarquis.fcm.data.model.Token
import fr.smarquis.fcm.databinding.ActivityMainBinding
import fr.smarquis.fcm.databinding.TopicsDialogBinding
import fr.smarquis.fcm.utils.Notifications
import fr.smarquis.fcm.utils.asString
import fr.smarquis.fcm.utils.safeStartActivity
import fr.smarquis.fcm.view.adapter.MessagesAdapter
import fr.smarquis.fcm.viewmodel.MainViewModel
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModel()

    private val messagesAdapter: MessagesAdapter = MessagesAdapter(get())

    @RequiresApi(O)
    private val requestNotificationsPermission = registerForActivityResult(RequestPermission(), ::onNotificationsPermissionResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)

        viewModel.token.observe(this, ::updateToken)
        viewModel.messages.observe(this, ::updateMessages)
        viewModel.presence.observe(this, ::updatePresence)

        messagesAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    if (positionStart != 0 || itemCount > 1) return
                    binding.recyclerView.post { binding.recyclerView.smoothScrollToPosition(0) }
                }
            },
        )
        binding.recyclerView.apply {
            setHasFixedSize(true)
            val horizontal = resources.getDimensionPixelSize(R.dimen.unit_4)
            val vertical = resources.getDimensionPixelSize(R.dimen.unit_1)
            addItemDecoration(SpacingItemDecoration(horizontal, vertical))
            ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                        val message = messagesAdapter.getItem(viewHolder.bindingAdapterPosition)
                        viewModel.delete(message)
                        Snackbar.make(binding.recyclerView, getString(R.string.snackbar_item_deleted, 1), Snackbar.LENGTH_LONG).setAction(R.string.snackbar_item_undo) {
                            viewModel.insert(message)
                        }.show()
                    }
                },
            ).attachToRecyclerView(this)
            adapter = messagesAdapter
        }
    }

    private fun updateMessages(messages: List<Message>) {
        messagesAdapter.submitList(messages) {
            binding.emptyView.isInvisible = messagesAdapter.itemCount > 0
            invalidateOptionsMenu()
        }
    }

    private fun updatePresence(presence: Presence) {
        if (presence is Presence.Error) {
            Toast.makeText(this, presence.message(), LENGTH_LONG).show()
        }
        invalidateOptionsMenu()
    }

    private fun updateToken(token: Token) {
        supportActionBar?.subtitle = when (token) {
            Token.Loading -> getString(R.string.fetching)
            is Token.Success -> token.value
            is Token.Failure -> token.exception.message?.also {
                Toast.makeText(this, it, LENGTH_LONG).show()
            }
        }
        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        Notifications.removeAll(this)
    }

    override fun onStart() {
        super.onStart()
        checkNotificationsPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_presence).apply {
            title = viewModel.presence.value.message()
            setIcon((viewModel.presence.value ?: Presence.Offline).icon)
        }
        menu.findItem(R.id.action_share_token).isEnabled = !(viewModel.token.value as? Token.Success)?.value.isNullOrEmpty()
        menu.findItem(R.id.action_delete_all).isVisible = messagesAdapter.itemCount > 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_presence -> Toast.makeText(this, viewModel.presence.value.message(), LENGTH_LONG).show()
            R.id.action_share_token -> (viewModel.token.value as? Token.Success)?.let { shareToken(it) }
            R.id.action_invalidate_token -> viewModel.resetToken()
            R.id.action_topics -> showTopicsDialog()
            R.id.action_delete_all -> showDeleteDialog()
            R.id.action_diagnostics -> openDiagnostics()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteDialog() {
        val messages = messagesAdapter.currentList.toTypedArray<Message>()
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.dialog_delete_all_title, messages.size))
            .setPositiveButton(R.string.dialog_delete_all_positive) { _: DialogInterface?, _: Int ->
                viewModel.delete(*messages)
                Notifications.removeAll(this)
                Snackbar.make(binding.recyclerView, getString(R.string.snackbar_item_deleted, messages.size), TimeUnit.SECONDS.toMillis(10).toInt())
                    .setAction(R.string.snackbar_item_undo) { viewModel.insert(*messages) }
                    .show()
            }
            .setNegativeButton(R.string.dialog_delete_all_negative) { _: DialogInterface?, _: Int -> }
            .show()
    }

    private fun shareToken(token: Token.Success) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, token.value)
        }
        safeStartActivity(Intent.createChooser(intent, getString(R.string.menu_share_token)))
    }

    private fun showTopicsDialog() {
        // Extracted from com.google.firebase.messaging.FirebaseMessaging
        val pattern = Pattern.compile("[a-zA-Z0-9-_.~%]{1,900}")
        val messaging = FirebaseMessaging.getInstance()
        val binding = TopicsDialogBinding.inflate(LayoutInflater.from(this), null, false)
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .setPositiveButton(R.string.topics_subscribe) { _: DialogInterface?, _: Int ->
                val topic = binding.inputText.text.toString()
                messaging.subscribeToTopic(topic)
                    .addOnSuccessListener(this) { Toast.makeText(this, getString(R.string.topics_subscribed, topic), LENGTH_LONG).show() }
                    .addOnFailureListener(this) { Toast.makeText(this, it.asString(), LENGTH_LONG).show() }
            }
            .setNegativeButton(R.string.topics_unsubscribe) { _: DialogInterface?, _: Int ->
                val topic = binding.inputText.text.toString()
                messaging.unsubscribeFromTopic(topic)
                    .addOnSuccessListener(this) { Toast.makeText(this, getString(R.string.topics_unsubscribed, topic), LENGTH_LONG).show() }
                    .addOnFailureListener(this) { Toast.makeText(this, it.asString(), LENGTH_LONG).show() }
            }.show()
        binding.inputText.doAfterTextChanged { editable ->
            val matches = editable?.let(pattern::matcher)?.matches() ?: false
            dialog.getButton(BUTTON_POSITIVE).isEnabled = matches
            dialog.getButton(BUTTON_NEGATIVE).isEnabled = matches
        }
        // Trigger afterTextChanged()
        binding.inputText.text = null
        binding.inputText.requestFocus()
    }

    private fun openDiagnostics() {
        val componentName = ComponentName("com.google.android.gms", "com.google.android.gms.gcm.GcmDiagnostics")
        val intent = Intent().setComponent(componentName)
        safeStartActivity(intent)
    }

    private fun Presence?.message() = when (this) {
        Presence.Offline, null -> getString(R.string.presence_offline)
        Presence.Online -> getString(R.string.presence_online)
        is Presence.Error -> error.message
    }

    private fun checkNotificationsPermission() {
        if (SDK_INT < O) return
        val notificationManager = NotificationManagerCompat.from(this)
        if (notificationManager.areNotificationsEnabled()) return
        if (SDK_INT < TIRAMISU) return permissionSnackBar { startActivity(Notifications.settingsIntent(this)) }
        if (checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) return
        if (shouldShowRequestPermissionRationale(this, POST_NOTIFICATIONS)) return permissionSnackBar { requestNotificationsPermission.launch(POST_NOTIFICATIONS) }
        requestNotificationsPermission.launch(POST_NOTIFICATIONS)
    }

    @RequiresApi(O)
    private fun onNotificationsPermissionResult(isGranted: Boolean) {
        if (isGranted) return
        permissionSnackBar { startActivity(Notifications.settingsIntent(this)) }
    }

    private fun permissionSnackBar(action: () -> Unit) = Snackbar
        .make(binding.root, getString(R.string.snackbar_notification_permission_required), Snackbar.LENGTH_INDEFINITE)
        .setAction(R.string.snackbar_notification_permission_grant) { action() }
        .show()

}
