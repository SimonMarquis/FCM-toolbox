package fr.smarquis.fcm.usecase

import fr.smarquis.fcm.data.model.Token
import fr.smarquis.fcm.data.repository.TokenRepository

class UpdateTokenUseCase(private val repo: TokenRepository) {
    suspend operator fun invoke(token: String): Unit = repo.update(Token.Success(token))
}