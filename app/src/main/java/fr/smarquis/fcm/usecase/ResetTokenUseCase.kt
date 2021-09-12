package fr.smarquis.fcm.usecase

import fr.smarquis.fcm.data.repository.TokenRepository

class ResetTokenUseCase(private val repo: TokenRepository) {
    suspend operator fun invoke(): Unit = repo.reset()
}