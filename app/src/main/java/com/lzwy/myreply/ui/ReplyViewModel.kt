package com.lzwy.myreply.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzwy.myreply.data.Message
import com.lzwy.myreply.data.MessageRepository
import com.lzwy.myreply.data.MessageRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReplyViewModel(private val emailsRepository: MessageRepository = MessageRepositoryImpl()) :
    ViewModel() {

    // UI state exposed to the UI
    private val _uiState = MutableStateFlow(ReplyHomeUIState(loading = true))
    val uiState: StateFlow<ReplyHomeUIState> = _uiState

    init {
        observeEmails()
    }

    private fun observeEmails() {
        viewModelScope.launch {
            emailsRepository.getAllEmails()
                .catch { ex ->
                    _uiState.value = ReplyHomeUIState(error = ex.message)
                }
                .collect { messages ->
                    /**
                     * We set first email selected by default for first App launch in large-screens
                     */
                    _uiState.value = ReplyHomeUIState(
                        messages = messages,
                        openedMessage = messages.first()
                    )
                }
        }
    }

    fun setOpenedEmail(emailId: Long) {
        /**
         * We only set isDetailOnlyOpen to true when it's only single pane layout
         */
        val message = uiState.value.messages.find { it.id == emailId }
        _uiState.value = _uiState.value.copy(
            openedMessage = message,
        )
    }

    fun toggleSelectedEmail(emailId: Long) {
        val currentSelection = uiState.value.selectedMessages
        _uiState.value = _uiState.value.copy(
            selectedMessages = if (currentSelection.contains(emailId))
                currentSelection.minus(emailId) else currentSelection.plus(emailId)
        )
    }

    fun closeDetailScreen() {
        _uiState.value = _uiState
            .value.copy(
                openedMessage = _uiState.value.messages.first()
            )
    }
}

data class ReplyHomeUIState(
    val messages: List<Message> = emptyList(),
    val selectedMessages: Set<Long> = emptySet(),
    val openedMessage: Message? = null,
    val loading: Boolean = false,
    val error: String? = null
)
