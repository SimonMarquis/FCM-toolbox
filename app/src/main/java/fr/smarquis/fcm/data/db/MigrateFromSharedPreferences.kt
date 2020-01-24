package fr.smarquis.fcm.data.db

import android.app.Application
import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get


class MigrateFromSharedPreferences(private val application: Application, private val moshi: Moshi) : RoomDatabase.Callback(), KoinComponent {

    override fun onCreate(db: SupportSQLiteDatabase) {
        GlobalScope.launch(Dispatchers.IO) {
            val sharedPreferences = application.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            val json = sharedPreferences.getString(KEY, null) ?: return@launch
            val type = Types.newParameterizedType(MutableList::class.java, Message::class.java)
            val adapter = moshi.adapter<List<Message>>(type)
            try {
                adapter.fromJson(json)?.let {
                    get<MessageRepository>().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sharedPreferences.edit().remove(KEY).apply()
        }
    }

    companion object {
        private const val NAME = "messages"
        private const val KEY = "data"
    }
}