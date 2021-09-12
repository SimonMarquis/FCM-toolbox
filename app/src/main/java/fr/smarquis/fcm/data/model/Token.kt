package fr.smarquis.fcm.data.model

sealed class Token {
    object Loading : Token()
    data class Success(val value: String) : Token()
    data class Failure(val exception: Throwable) : Token()
}