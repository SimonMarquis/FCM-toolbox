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
package fr.smarquis.fcm.view.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.Moshi
import fr.smarquis.fcm.R
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.model.Payload
import fr.smarquis.fcm.databinding.ItemPayloadBinding
import fr.smarquis.fcm.utils.copyToClipboard
import fr.smarquis.fcm.utils.safeStartActivity
import fr.smarquis.fcm.view.adapter.MessagesAdapter.Action.PRIMARY
import fr.smarquis.fcm.view.adapter.MessagesAdapter.Action.SECONDARY
import fr.smarquis.fcm.view.ui.TimeAgoTextView
import kotlin.math.min

class MessagesAdapter(private val moshi: Moshi) : ListAdapter<Message, MessagesAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.messageId == newItem.messageId
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
        }
        private val selection: androidx.collection.ArrayMap<String, Boolean> = androidx.collection.ArrayMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemPayloadBinding.inflate(LayoutInflater.from(parent.context), parent, false), ::toggle)

    private fun toggle(message: Message, viewHolder: ViewHolder) {
        selection[message.messageId] = !(selection[message.messageId] ?: false)
        notifyItemChanged(viewHolder.bindingAdapterPosition)
    }

    public override fun getItem(position: Int): Message = super.getItem(position)

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) = viewHolder.onBind(
        getItem(position),
        selection[getItem(position).messageId]
            ?: false,
    )

    override fun onViewRecycled(holder: ViewHolder) = holder.onUnbind()

    enum class Action {
        PRIMARY, SECONDARY
    }

    inner class ViewHolder(private val binding: ItemPayloadBinding, private val listener: (Message, ViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        private var message: Message? = null
        private var selected = false

        init {
            with(binding) {
                buttonPrimary.setOnClickListener { execute(PRIMARY, payload()) }
                buttonSecondary.setOnClickListener { execute(SECONDARY, payload()) }
                root.setOnClickListener { message?.let { listener(it, this@ViewHolder) } }
                root.setOnLongClickListener { message?.let { listener(it, this@ViewHolder) }.let { true } }
            }
        }

        private fun payload(): Payload? = message?.payload

        private fun execute(action: Action, payload: Payload?) {
            val context = itemView.context
            when (payload) {
                is Payload.App -> {
                    when (action) {
                        PRIMARY -> context.safeStartActivity(payload.playStore())
                        SECONDARY -> context.safeStartActivity(payload.uninstall())
                    }
                }

                is Payload.Link -> if (action == PRIMARY) context.safeStartActivity(payload.intent())
                is Payload.Text -> if (action == PRIMARY) context.copyToClipboard(payload.text)
                is Payload.Ping -> {
                }

                is Payload.Raw -> {
                }

                null -> {
                }
            }
        }

        fun onBind(message: Message, selected: Boolean) = with(binding) {
            this@ViewHolder.message = message
            this@ViewHolder.selected = selected
            icon.setImageResource(message.payload?.icon() ?: 0)
            timestamp.timestamp = min(message.sentTime, System.currentTimeMillis())
            renderContent()
            renderButtons()
        }

        private fun renderContent() = with(binding) {
            selector.isActivated = selected
            if (selected) {
                text.text = null
                text.visibility = GONE
                val data: Map<*, *>? = message?.data
                val display = if (data != null) moshi.adapter(Message::class.java).indent("  ").toJson(message) else null
                raw.text = display
                raw.visibility = if (TextUtils.isEmpty(display)) GONE else VISIBLE
            } else {
                val payload = payload()
                val display = payload?.display()
                text.text = display
                text.visibility = if (TextUtils.isEmpty(display)) GONE else VISIBLE
                raw.text = null
                raw.visibility = GONE
            }
        }

        private fun renderButtons() = with(binding) {
            mapOf(
                PRIMARY to buttonPrimary,
                SECONDARY to buttonSecondary,
            ).forEach { (action, button) ->
                render(action, button, message?.payload)
            }
        }

        private fun render(action: Action, button: Button, payload: Payload?) {
            if (selected) {
                button.visibility = GONE
                return
            }
            when (payload) {
                is Payload.App -> {
                    when (action) {
                        PRIMARY -> {
                            button.visibility = VISIBLE
                            button.setText(R.string.payload_app_store)
                            return
                        }

                        SECONDARY -> {
                            button.setText(R.string.payload_app_uninstall)
                            button.visibility = if (payload.isInstalled(itemView.context)) VISIBLE else GONE
                            return
                        }
                    }
                }

                is Payload.Link -> if (action == PRIMARY) {
                    button.visibility = VISIBLE
                    button.setText(R.string.payload_link_open)
                    return
                }

                is Payload.Text -> if (action == PRIMARY) {
                    button.visibility = VISIBLE
                    button.setText(R.string.payload_text_copy)
                    return
                }

                is Payload.Ping -> {}
                is Payload.Raw -> {}
                null -> {}
            }
            button.visibility = GONE
            button.text = null
        }

        fun onUnbind() {
            message = null
            binding.timestamp.timestamp = TimeAgoTextView.NO_TIMESTAMP
        }
    }

}
