package com.lzwy.myreply.data.llm

import android.net.Uri
import android.util.Log
import com.lzwy.myreply.data.llm.zhipu.Content
import com.lzwy.myreply.data.llm.zhipu.ContentType
import com.lzwy.myreply.data.llm.zhipu.FileInfo
import com.lzwy.myreply.data.llm.zhipu.Query
import com.lzwy.myreply.data.llm.zhipu.Role
import com.lzwy.myreply.data.llm.zhipu.ZhipuQAMessage
import com.lzwy.myreply.data.llm.zhipu.ZhipuQARequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LlmRequestManagerImpl : ILlmRequestManager {

    private var currentFilePath: String? = null
    private var requestId: Long = -1

    override fun request(
        coroutineScope: CoroutineScope,
        listener: ILlmRequestManager.IListener?,
        question: String?,
        filePath: String?
    ) {
        Log.i(TAG, "request question:$question")
        var fileType: Int = FILE_TYPE
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                fileType = checkFileType(file)
            }
            if (fileType == 0) {
                listener?.onException("不支持当前文件格式")
            } else if (fileType != FILE_TYPE && !question.isNullOrEmpty()) {
/*                BitmapUtils.decodeFileToBase64(context, filePath, object :
                    BitmapUtils.DecodeListener {
                    override fun onSuccess(image: String) {
                        queryWithQuestion(listener, question, image)
                    }

                    override fun onFailed() {
                        queryWithQuestion(listener, question, null)
                    }
                })*/
            }
        } else {
            if (!question.isNullOrEmpty()) {
                coroutineScope.launch {
                    queryWithZhipu(listener, question, null)
                }
            }
        }
    }

    override fun request(
        coroutineScope: CoroutineScope,
        listener: ILlmRequestManager.IListener?,
        question: String?,
        imageUri: Uri
    ) {
        Log.i(TAG, "request question:$question, imageUri:$imageUri")
        // val filePath: String? = FileUtils.convertUriToPath(context, fileUri) TODO: convert it as absolute path
        val filePath = ""
        Log.i(TAG, "request filePath " + filePath)
        currentFilePath = filePath
        request(coroutineScope, listener, question, filePath)
    }

    override fun requestSyncWithPrompt(
        listener: ILlmRequestManager.IListener?,
        model: String,
        prompt: String,
        messageList: List<Map<String, String>>
    ) {
        TODO("Not yet implemented")
    }

    private fun reloadConversation(){
        // TODO: reload history list if needed
    }

    private suspend fun queryWithZhipu(
        listener: ILlmRequestManager.IListener?,
        question: String?,
        image64: String?
    ) {
        val requestId = System.currentTimeMillis()
        this.requestId = requestId
        withContext(Dispatchers.IO) {
            reloadConversation()

            val zhipuRequestData = if (image64 != null) {
                // TODO: only add current message won't support multi-turn conversation
                ZhipuQARequestManager.createRequestData(
                    "glm-4v",
                    true,
                    ZhipuQAMessage.multimediaMessage(
                        role = Role.user,
                        contentType = ContentType.image,
                        fileInfo = FileInfo.imageBase64(image64.orEmpty()),
                        query = Query.create(question.orEmpty())
                    ),
                    true)
            } else {
                ZhipuQARequestManager.addMessage(ZhipuQAMessage.plainMessage(
                    role = Role.user,
                    content = Content.create(question.orEmpty())
                ))
                ZhipuQARequestManager.createRequestData("glm-4", true)
            }
            val call = ZhipuQARequestManager.getZhipuRequestCall(zhipuRequestData)
            val response = call.execute()
            if (response.isSuccessful) {
                Log.i(TAG, "Response successful")
                response.body()?.let {
                    listener?.onServerResponse(ZhipuQARequestManager.getLlmResponseData(it))
                }
            } else {
                if (this@LlmRequestManagerImpl.requestId != requestId) {
                    Log.e(TAG, "onSuccess: requestId is wrong")
                }
                listener?.onException(response.message())
            }
        }
    }

    private fun checkFileType(file: File): Int {
        return when (file.extension) {
            "jpg" -> IMAGE_JPG
            "jpeg" -> IMAGE_JPG
            "png" -> IMAGE_PNG
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "txt" -> FILE_TYPE
            else -> 0
        }
    }

    companion object {
        const val TAG = "LlmRequestManager"
        const val IMAGE_JPG = 1
        const val IMAGE_PNG = 2
        const val FILE_TYPE = 3
    }
}