package fr.smarquis.fcm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fr.smarquis.fcm.data.db.AppDatabase.MapConverter
import fr.smarquis.fcm.data.db.AppDatabase.PayloadConverter
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.model.Payload
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Database(entities = [Message::class], version = 1)
@TypeConverters(value = [MapConverter::class, PayloadConverter::class])
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): MessageDao

    object PayloadConverter : KoinComponent {

        private val moshi by inject<Moshi>()
        private val adapter = moshi.adapter(Payload::class.java)

        @TypeConverter
        @JvmStatic
        fun fromJson(data: String): Payload? = adapter.fromJson(data)

        @TypeConverter
        @JvmStatic
        fun toJson(payload: Payload?): String = adapter.toJson(payload)

    }

    object MapConverter : KoinComponent {

        private val moshi by inject<Moshi>()
        private val adapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))

        @TypeConverter
        @JvmStatic
        fun stringToMap(data: String): Map<String, String> = adapter.fromJson(data).orEmpty()

        @TypeConverter
        @JvmStatic
        fun mapToString(map: Map<String, String>?): String = adapter.toJson(map)

    }

}



