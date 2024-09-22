package com.lzwy.myreply.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzwy.myreply.data.ChatItem
import com.lzwy.myreply.data.Message
import com.lzwy.myreply.data.MessageRepository
import com.lzwy.myreply.data.MessageRepositoryImpl
import com.lzwy.myreply.data.llm.ILlmRequestManager
import com.lzwy.myreply.data.llm.LlmRequestManagerImpl
import com.lzwy.myreply.data.llm.LlmResponseData
import com.lzwy.myreply.data.llm.Model
import com.lzwy.myreply.data.remote.RetrofitManager
import com.lzwy.myreply.ui.utils.createAndCompressImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private const val TAG = "ReplyViewModel"

class ReplyViewModel(private val emailsRepository: MessageRepository = MessageRepositoryImpl()) :
    ViewModel() {

    // UI state exposed to the UI
    private val _uiState = MutableStateFlow(ReplyHomeUIState(loading = true))
    private val _conversationState = MutableStateFlow(ConversationState())
    private val _llmLastReply = MutableStateFlow("")
    private val requestManager: ILlmRequestManager by lazy { LlmRequestManagerImpl() }
    val uiState: StateFlow<ReplyHomeUIState> = _uiState
    val conversationState: StateFlow<ConversationState> = _conversationState
    val llmLastReply: StateFlow<String> = _llmLastReply
    val historyList: MutableList<ChatItem> = mutableListOf()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        Log.i(TAG, "observeEmails")
        viewModelScope.launch {
            val response = RetrofitManager.getReplyApiService().getAllMessages()
            Log.i(TAG, "observeMessages: code: ${response.code}, size: ${response.content?.size}")

            _uiState.value = _uiState.value.copy(
                messages = response.content ?: emptyList()
            )
        }
    }

    fun setOpenedEmail(emailId: Long) {
        val message = uiState.value.messages.find { it.id == emailId }
        _uiState.value = _uiState.value.copy(
            openedMessage = message,
        )
    }

    fun setWriting(isWriting: Boolean = true) {
        // TODO: clear relative data
        _uiState.value = _uiState.value.copy(
            isWriting = isWriting
        )
    }

    private fun updateHistoryList(content: String, isUser: Boolean) {
        historyList.add(ChatItem(isUser, content))
        _conversationState.value =
            _conversationState.value.copy(
                historyList = historyList.toList()
            )
    }

    fun setLlmModel(displayName: String) {
        val model = Model.entries.find { it.displayName == displayName }
        _conversationState.value =
            _conversationState.value.copy(
                model = model ?: Model.BearOne
            )
    }

    fun chatWithLLM(question: String = "你好") {
        Log.i(TAG, "chatWithLLM, question: $question, model: ${_conversationState.value.model}")
        updateHistoryList(question, true)
        _llmLastReply.value = ""
        _conversationState.value =
            _conversationState.value.copy(
                isAsking = true
            )
        requestManager.request(
            viewModelScope,
            object : ILlmRequestManager.IListener {
                private var answer = ""
                override fun onServerResponse(data: LlmResponseData?) {
                    data?.let {
                        if (it.isIncremental) {
                            answer += it.content
                        } else {
                            answer = it.content
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            _llmLastReply.value = answer
                        }
                        if (it.responseFinish) {
                            _conversationState.value =
                                _conversationState.value.copy(
                                    isAsking = false,
                                )
                            updateHistoryList(answer, false)
                            Log.i(TAG, "llm response finish, update status: ${_conversationState.value.historyList.size}")
                        }
                        // todo: exception handle
                        Log.d(TAG, "get content: $answer")
                    }
                }

                override fun onException(errorMessage: String?) {
                    Log.e(TAG, "Access LLM error: $errorMessage")
                    CoroutineScope(Dispatchers.Main).launch {
                        _llmLastReply.value = "error"
                    }
                }
            },
            question
        )
    }

    fun finishWriting(context: Context, title: String = "", body: String = "", uris: List<Uri>?, record: String?) {
        Log.i(TAG, "finishWriting: title: $title, body: $body, hasImages: ${!uris.isNullOrEmpty()}, hasRecord: ${!record.isNullOrEmpty()}")
        viewModelScope.launch {
            var imagesList: MutableList<MultipartBody.Part>? = null
            if (!uris.isNullOrEmpty()) {
                imagesList = mutableListOf()
                for(uri in uris) {
                    val file = createAndCompressImage(context, uri)
                    val requestFile = file?.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    if (requestFile != null) {
                        // images might mean API's name. it need to store in a val
                        imagesList.add(MultipartBody.Part.createFormData("images", file.name, requestFile))
                    }
                }
            }
            val recordFile: File?
            var filePart: MultipartBody.Part? = null
            if(!record.isNullOrEmpty()) {
                recordFile = File(record)
                val requestBody: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), recordFile)
                filePart = MultipartBody.Part.createFormData("record", recordFile.name, requestBody)
            }
            Log.i(TAG, "record: $record")
            async {
                RetrofitManager.getReplyApiService().uploadMessage(4L, title, body,
                    System.currentTimeMillis(), imagesList, filePart)
            }.await().apply {
                observeMessages()
                setWriting(false)
            }
        }
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
                openedMessage = null
            )
    }
}

data class ReplyHomeUIState(
    val messages: List<Message> = emptyList(),
    val selectedMessages: Set<Long> = emptySet(),
    val openedMessage: Message? = null,
    val loading: Boolean = false,
    val isWriting: Boolean = false,
    val error: String? = null,
    val lastReply: String = "12345"
)

data class ConversationState(
    var isAsking: Boolean = false,
    var model: Model = Model.BearOne,
    var historyList: List<ChatItem> = emptyList()
)