package com.lzwy.myreply.data.llm

import android.net.Uri
import android.os.Looper
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.lzwy.myreply.data.remote.RetrofitManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class LlmRequestManagerImpl : ILlmRequestManager {

    private var historyList: ArrayList<ZhipuQAMessage<*>> = ArrayList()
    private var currentFilePath: String? = null
    private var qaRequest: ZhipuQARequest? = null
    private var requestId: Long = -1
    private var conversationId: String? = null

    override fun request(
        coroutineScope: CoroutineScope,
        listener: ILlmRequestManager.IListener?,
        question: String?,
        filePath: String?
    ) {
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
            question?.let {
                if (question.isNotEmpty()) {
                    coroutineScope.launch {
                        queryWithQuestion(listener, question, null)
                    }
                }
            }
        }
    }

    override fun request(
        coroutineScope: CoroutineScope,
        listener: ILlmRequestManager.IListener?,
        question: String?,
        fileUri: Uri
    ) {
        Log.i(TAG, "request question:$question, imageUri:$fileUri")
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

    private suspend fun queryWithQuestion(
        listener: ILlmRequestManager.IListener?,
        question: String?,
        image64: String?
    ) {
        val message = ZhipuQAMessage.multimediaMessage(
            role = Role.user,
            contentType = ContentType.image,
            fileInfo = FileInfo.imageBase64(image64.orEmpty()),
            query = Query.create(question.orEmpty())
        )

        reloadConversation()
        historyList.add(
            ZhipuQAMessage.plainMessage(
                role = Role.user,
                content = Content.create(question.orEmpty())
            )
        )

        val requestId = System.currentTimeMillis()
        this.requestId = requestId

        if (image64 != null) {
            requestZhipuAiImage(listener, arrayListOf(message), requestId)
        } else {
            requestZhipuAi(listener, requestId)
        }
    }

    private val systemMsgList = ArrayList<ZhipuQAMessage<*>>()

    private fun setupSystemList() {
        systemMsgList.clear()
        systemMsgList.add(
            ZhipuQAMessage.plainMessage(
                content = Content.create("你是一个专业的搜索引擎，你的任务是帮助用户搜索相关信息。你需要提供准确、有用的信息，并引用相关来源。"),
                role = Role.system
            )
        )
    }

    private fun addWebSearch(): ArrayList<Tools> {
        val toolsList = ArrayList<Tools>()
        val webSearch = WebSearch(true, true, SEARCH_PROMPT)
        val requestMessage = Tools("web_search", webSearch)
        toolsList.add(requestMessage)
        return toolsList
    }

    private fun generateClientToken(apikey: String): String? {
        val apiKeyParts = apikey.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val apiKey = apiKeyParts[0]
        val secret = apiKeyParts[1]
        val header: MutableMap<String, Any> = java.util.HashMap()
        header["alg"] = "HS256"
        header["sign_type"] = "SIGN"
        val payload: MutableMap<String, Any> = java.util.HashMap()
        payload["api_key"] = apiKey
        payload["exp"] = System.currentTimeMillis() + 5 * 600 * 1000
        payload["timestamp"] = System.currentTimeMillis()
        var token: String? = null
        try {
            token = JWT.create()
                .withHeader(header)
                .withPayload(payload)
                .sign(Algorithm.HMAC256(secret))
        } catch (e: java.lang.Exception) {
            println()
        }
        Log.d(ZhipuQARequest.TAG, "token = $token ")
        return token
    }

    private suspend fun requestZhipuAi(
        listener: ILlmRequestManager.IListener?,
        requestId: Long
    ) {
        Log.i(TAG, "requestZhipuAi start... at: ${Looper.getMainLooper().isCurrentThread}")
        withContext(Dispatchers.IO) {
            Log.d(TAG, "request data :$historyList")
            val requestData = ZhipuApiRequestData()
            val api = RetrofitManager.getZhipuApiService()

            requestData.model = "glm-4v"
            Log.i(TAG, "model is ${requestData.model}")

            systemMsgList.clear()
            if (systemMsgList.size <= 0) {
                setupSystemList()
            }
            systemMsgList.addAll(historyList)
            requestData.messages = systemMsgList

            requestData.tools = addWebSearch()
            requestData.stream = true
            val call = api.requestZhipuAi(
                generateClientToken(API_KEY),
                "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
                "application/json; charset=utf-8",
                requestData
            )

            val response = call.execute()
            if (response.isSuccessful) {
                Log.i(TAG, "Response successful")
                withContext(Dispatchers.IO) {
                    response.body()?.let { toZhiPuResponseData(listener, it) }
                }
            } else {
                if (this@LlmRequestManagerImpl.requestId != requestId) {
                    Log.i(TAG, "onSuccess: requestId is wrong")
                    return@withContext
                }
                listener?.onException(response.message())
            }
        }
    }
    
    private fun requestZhipuAiImage(
        listener: ILlmRequestManager.IListener?,
        messages: ArrayList<ZhipuQAMessage<*>>,
        requestId: Long
    ) {
        if (qaRequest == null) {
            qaRequest = ZhipuQARequest()
        }

        Log.e(TAG, "request data :$messages")
        qaRequest?.requestZhipuImage(messages, object : ZhipuQARequest.ZhipuAiCallBack {
            @Throws(IOException::class)
            override fun onSuccess(response: ResponseBody?) {
                if (this@LlmRequestManagerImpl.requestId != requestId) {
                    Log.i(TAG, "onSuccess: requestId is wrong")
                    return
                }

                Log.d(TAG, "onSuccess: requestZhipu success")
                response?.let {
                    toZhiPuResponseData(listener, it)
                }
            }

            override fun onFail(errorMessage: String?) {
                if (this@LlmRequestManagerImpl.requestId != requestId) {
                    Log.i(TAG, "onSuccess: requestId is wrong")
                    return
                }
                Log.d(TAG, "onFail: requestZhipu fail")
                listener?.onException(errorMessage)
            }
        })
    }

    private fun toZhiPuResponseData(
        listener: ILlmRequestManager.IListener?,
        response: ResponseBody
    ): ZhipuResponseData? {
        var data: ZhipuResponseData? = null
        val reader = BufferedReader(
            InputStreamReader(
                response.byteStream(), StandardCharsets.UTF_8
            )
        )
        var line: String? = null
        while ((reader.readLine()?.also { line = it }) != null) {
            Log.i(TAG, "line length : " + line!!.length)
            if (line!!.length < 20) continue  //data: [DONE]

            val jsonBody = if(line!!.startsWith("data:")) line!!.substring(5) else line!!
            val gson = Gson()
            Log.i(
                TAG,
                "ai_search_result Success, data = $line"
            )
            try {
                data =
                    gson.fromJson(jsonBody, ZhipuResponseData::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Response Json error : ${e.stackTrace}")
            }

            data?.let {
                listener?.onServerResponse(handleZhipuResponseData(it))
            }
        }
        reader.close()
        return data
    }

    private var assistantTextBuilder: StringBuilder = StringBuilder()
    private fun updateAssistantData(content: String, finish: Boolean) {
        assistantTextBuilder.append(content)
        if (finish) {
            historyList.add(
                ZhipuQAMessage.plainMessage(
                    content = Content.create(assistantTextBuilder.toString()),
                    role = Role.assistant
                )
            )
            assistantTextBuilder.clear()
        }
    }

    private fun handleZhipuResponseData(data: ZhipuResponseData): LlmResponseData {
        val llmResponseData = LlmResponseData()
        val delta: Delta = data.choices[0].delta
        llmResponseData.isIncremental = true
        llmResponseData.id = data.id

        llmResponseData.responseFinish = false
        data.choices[0].finishReason?.let {
            llmResponseData.finishReason = it
            llmResponseData.responseFinish = true
            updateAssistantData(delta.content, true)
        }

        if (delta.content != null) {
            llmResponseData.content = delta.content
            updateAssistantData(delta.content, false)
        }
        conversationId = data.conversationId

        return llmResponseData
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

        private const val API_KEY = "e0cd41dc7d320defa114f27ab03b2b3e.vK9tM5DvrhoCyM7j"
        private const val SEARCH_PROMPT = "\"\"\"\n" +
                "\n" +
                "# 以下是来自互联网的信息：\n" +
                "{search_result}\n" +
                "# 要求：\n" +
                "根据最新发布的信息回答用户问题，当回答引用了参考信息时，必须在句末使用对应的[ref_序号]来标明参考信息来源。\n" +
                "\n" +
                "\"\"\""
    }
}