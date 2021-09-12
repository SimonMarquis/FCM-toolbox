package fr.smarquis.fcm.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import fr.smarquis.fcm.data.model.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InMemoryTokenRepository(
    private val fcm: FirebaseMessaging,
    scope: CoroutineScope,
) : TokenRepository, CoroutineScope by scope {

    private val state = MutableStateFlow<Token>(Token.Loading)

    init {
        launch {
            refresh()
        }
    }

    private suspend fun refresh() {
        val token = try {
            Token.Success(fcm.token.await())
        } catch (e: Exception) {
            e.printStackTrace()
            Token.Failure(e.rootCause())
        }
        update(token)
    }

    override fun get(): Flow<Token> = state

    override suspend fun update(token: Token) = state.emit(token)

    override suspend fun reset() {
        try {
            update(Token.Loading)
            fcm.deleteToken().await()
            Unit
        } catch (e: Exception) {
            e.printStackTrace()
            update(Token.Failure(e.rootCause()))
        }
        refresh()
    }

    private fun Throwable.rootCause(): Throwable {
        var root: Throwable? = this
        while (root!!.cause != null && root.cause !== root) {
            root = root.cause
        }
        return root
    }

}