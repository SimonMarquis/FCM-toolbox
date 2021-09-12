package fr.smarquis.fcm.viewmodel

import android.app.Application
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import fr.smarquis.fcm.BuildConfig
import fr.smarquis.fcm.data.model.Presence
import fr.smarquis.fcm.data.model.Presence.*
import fr.smarquis.fcm.data.model.Token
import fr.smarquis.fcm.usecase.GetTokenUseCase
import fr.smarquis.fcm.utils.uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PresenceLiveData(
    application: Application,
    database: FirebaseDatabase,
    getTokenUseCase: GetTokenUseCase,
    scope: CoroutineScope,
) : LiveData<Presence>(Offline), ValueEventListener, CoroutineScope by scope {

    private var token: Token? = null
    private val presenceRef: DatabaseReference = database.getReference(".info/connected")
    private val connectionRef: DatabaseReference = database.getReference("devices/${uuid(application)}").apply {
        onDisconnect().removeValue()
    }

    init {
        launch {
            getTokenUseCase().collectLatest {
                token = it
                updateMetadata()
            }
        }
    }

    override fun onActive() {
        presenceRef.addValueEventListener(this)
        DatabaseReference.goOnline()
        updateMetadata()
    }

    override fun onInactive() {
        clearMetadata()
        DatabaseReference.goOffline()
        presenceRef.removeEventListener(this)
        value = Offline
    }

    override fun onCancelled(error: DatabaseError) {
        value = Error(error)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        value = when (snapshot.getValue(Boolean::class.java)) {
            true -> Online
            false, null -> Offline
        }
        updateMetadata()
    }

    private fun updateMetadata() = connectionRef.setValue(metadata(token))

    private fun clearMetadata() = connectionRef.removeValue()

    private fun metadata(token: Token?) = mapOf(
        "name" to if (MODEL.lowercase().startsWith(MANUFACTURER.lowercase())) MODEL else MANUFACTURER.lowercase() + " " + MODEL,
        "token" to when (token) {
            is Token.Success -> token.value
            Token.Loading, is Token.Failure, null -> null
        },
        "version" to BuildConfig.VERSION_CODE,
        "timestamp" to ServerValue.TIMESTAMP
    )

}