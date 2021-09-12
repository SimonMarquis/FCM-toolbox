package fr.smarquis.fcm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import fr.smarquis.fcm.usecase.GetTokenUseCase
import fr.smarquis.fcm.usecase.ResetTokenUseCase
import fr.smarquis.fcm.data.model.Token
import fr.smarquis.fcm.data.model.Message
import fr.smarquis.fcm.data.repository.MessageRepository
import kotlinx.coroutines.launch

class MainViewModel(
    val presence: PresenceLiveData,
    getTokenUseCase: GetTokenUseCase,
    private val resetTokenUseCase: ResetTokenUseCase,
    private val repository: MessageRepository,
) : ViewModel() {

    val messages = repository.get()

    val token: LiveData<Token> = getTokenUseCase().asLiveData()

    fun insert(vararg messages: Message) = viewModelScope.launch { repository.insert(*messages) }

    fun delete(vararg messages: Message) = viewModelScope.launch { repository.delete(*messages) }

    fun resetToken() = viewModelScope.launch { resetTokenUseCase() }

}