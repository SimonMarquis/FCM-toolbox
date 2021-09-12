package fr.smarquis.fcm.data.repository

import fr.smarquis.fcm.data.model.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {

    fun get(): Flow<Token>

    suspend fun update(token: Token)

    suspend fun reset()

}