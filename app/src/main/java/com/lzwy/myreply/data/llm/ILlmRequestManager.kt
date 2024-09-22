package com.lzwy.myreply.data.llm

import android.net.Uri
import kotlinx.coroutines.CoroutineScope

interface ILlmRequestManager {
    // TODO: i do really think it's weird to pass a scope so deeply//
    fun request(
        coroutineScope: CoroutineScope,
        listener: IListener?,
        question: String?,
        filePath: String? = null
    )

    fun request(
        coroutineScope: CoroutineScope,
        listener: IListener?,
        question: String?,
        imageUri: Uri,
    )

    fun requestSyncWithPrompt(listener: IListener?, model: String, prompt: String, messageList: List<Map<String, String>>)

    interface IListener {
        fun onServerResponse(data: LlmResponseData?)
        fun onException(errorMessage: String?)
    }
}

data class LlmResponseData(
    var id: String = "",
    var content: String = "",
    var imageUri: String = "",
    var finishReason: String = "",
    var responseFinish: Boolean = false,
    var isIncremental: Boolean = false,
) {
    override fun toString(): String {
        return "LlmResponseData(id='$id', content='$content', imageUri='$imageUri', finishReason='$finishReason', responseFinish=$responseFinish, isIncremental=$isIncremental)"
    }
}