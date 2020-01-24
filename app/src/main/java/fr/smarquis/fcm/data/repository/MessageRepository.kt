package fr.smarquis.fcm.data.repository

import fr.smarquis.fcm.data.db.MessageDao
import fr.smarquis.fcm.data.model.Message

class MessageRepository(private val dao: MessageDao) {

    fun get() = dao.get()

    suspend fun insert(vararg messages: Message) = dao.insert(*messages)

    suspend fun delete(vararg messages: Message) = dao.delete(*messages)

}