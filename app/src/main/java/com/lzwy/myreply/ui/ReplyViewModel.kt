package com.lzwy.myreply.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzwy.myreply.data.Message
import com.lzwy.myreply.data.MessageRepository
import com.lzwy.myreply.data.MessageRepositoryImpl
import com.lzwy.myreply.data.llm.ILlmRequestManager
import com.lzwy.myreply.data.llm.LlmRequestManagerImpl
import com.lzwy.myreply.data.llm.LlmResponseData
import com.lzwy.myreply.data.remote.RetrofitManager
import com.lzwy.myreply.ui.utils.createAndCompressImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

private const val TAG = "ReplyViewModel"

class ReplyViewModel(private val emailsRepository: MessageRepository = MessageRepositoryImpl()) :
    ViewModel() {

    // UI state exposed to the UI
    private val _uiState = MutableStateFlow(ReplyHomeUIState(loading = true))
    private val requestManager: ILlmRequestManager by lazy { LlmRequestManagerImpl() }
    val uiState: StateFlow<ReplyHomeUIState> = _uiState
    private val _llmState = MutableStateFlow("")
    val llmState: StateFlow<String> = _llmState

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
                     * We set first message selected by default for first App launch in large-screens
                     */
                    val newList = _uiState.value.messages.toMutableList()
                    newList.addAll(messages)
                    _uiState.value = ReplyHomeUIState(
                        messages = newList.toList(),
                        openedMessage = null
                    )
                }
        }
    }

    fun setOpenedEmail(emailId: Long) {
        val message = uiState.value.messages.find { it.id == emailId }
        _uiState.value = _uiState.value.copy(
            openedMessage = message,
        )
    }

    fun setWriting() {
        _uiState.value = _uiState.value.copy(
            isWriting = true
        )
    }

    fun accessLLM() {
        Log.i(TAG, "accessLLM")
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
                            _llmState.value = answer
                        }
                        Log.i(TAG, "get content: $answer")
                    }
                }

                override fun onException(errorMessage: String?) {
                    Log.e(TAG, "Access LLM error: $errorMessage")
                    CoroutineScope(Dispatchers.Main).launch {
                        _llmState.value = "error"
                    }
                }
            },
            "介绍一下你自己"
        )
    }

    fun finishWriting(context: Context, title: String = "", body: String = "", uris: List<Uri>, record: String) {
        viewModelScope.launch {
            val imagesList: MutableList<MultipartBody.Part> = mutableListOf()
            for(uri in uris) {

                val file = createAndCompressImage(context, uri)
                val requestFile = file?.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                if (requestFile != null) {
                    // images might mean API's name. it need to store in a val
                    imagesList.add(MultipartBody.Part.createFormData("images", file.name, requestFile))
                }
            }
            // TODO: delegate as 3 func, with both record and image, with only record, with only image
            if(imagesList.isEmpty()) {
                val emptyFile: RequestBody = "".toRequestBody("image/*".toMediaTypeOrNull())
                val emptyPart: MultipartBody.Part = MultipartBody.Part.createFormData("images", "empty.jpg", emptyFile)
                imagesList.add(emptyPart)
            }
            val recordFile: File?
            val filePart: MultipartBody.Part?
            if(record.isNotEmpty()) {
                recordFile = File(record)
                val requestBody: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), recordFile)
                filePart = MultipartBody.Part.createFormData("record", recordFile.name, requestBody)
            }
            else {
                val emptyFile: RequestBody = "".toRequestBody("image/*".toMediaTypeOrNull())
                filePart = MultipartBody.Part.createFormData("images", "empty.jpg", emptyFile)
            }
            Log.i(TAG, "record: $record")
            RetrofitManager.getReplyApiService().uploadMessage(4L, title, body!!,
                System.currentTimeMillis(), imagesList, filePart)

            delay(2000)
            // TODO: modify as async & wait for the response
            observeEmails()
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
