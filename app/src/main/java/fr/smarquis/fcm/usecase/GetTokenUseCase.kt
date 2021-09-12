package fr.smarquis.fcm.usecase

import fr.smarquis.fcm.data.model.Token
import fr.smarquis.fcm.data.repository.TokenRepository
import kotlinx.coroutines.flow.Flow

class GetTokenUseCase(private val repo: TokenRepository) {
    operator fun invoke(): Flow<Token> = repo.get()
}