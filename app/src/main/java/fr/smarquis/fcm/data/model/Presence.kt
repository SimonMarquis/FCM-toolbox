package fr.smarquis.fcm.data.model

import androidx.annotation.DrawableRes
import com.google.firebase.database.DatabaseError

sealed class Presence(@DrawableRes val icon: Int) {
    object Online : Presence(android.R.drawable.presence_online)
    object Offline : Presence(android.R.drawable.presence_invisible)
    data class Error(val error: DatabaseError) : Presence(android.R.drawable.presence_busy)
}
