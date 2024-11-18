package fr.smarquis.fcm.data.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.messaging.RemoteMessage.Notification

@Entity
@Keep
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "messageId") val messageId: String,
    @ColumnInfo(name = "notification") val notification: Notification?,
    @ColumnInfo(name = "from") val from: String?,
    @ColumnInfo(name = "to") val to: String?,
    @ColumnInfo(name = "data") val data: Map<String, String>,
    @ColumnInfo(name = "collapseKey") val collapseKey: String?,
    @ColumnInfo(name = "messageType") val messageType: String?,
    @ColumnInfo(name = "sentTime") val sentTime: Long,
    @ColumnInfo(name = "ttl") val ttl: Int,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "originalPriority") val originalPriority: Int,
    @ColumnInfo(name = "payload") val payload: Payload? = null,

    )
