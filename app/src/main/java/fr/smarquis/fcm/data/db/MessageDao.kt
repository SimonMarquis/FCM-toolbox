package fr.smarquis.fcm.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.smarquis.fcm.data.model.Message

@Dao
interface MessageDao {

    @Query("SELECT * FROM message ORDER BY sentTime DESC")
    fun get(): LiveData<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg messages: Message)

    @Delete
    suspend fun delete(vararg messages: Message)

}
