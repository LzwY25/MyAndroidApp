package com.lzwy.myreply.data.llm.zhipu

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.lzwy.myreply.data.llm.ILlmRequestManager
import com.lzwy.myreply.data.llm.LlmRequestManagerImpl
import com.lzwy.myreply.data.llm.LlmRequestManagerImpl.Companion
import com.lzwy.myreply.data.llm.LlmResponseData
import com.lzwy.myreply.data.remote.RetrofitManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/*
* 文件问答："glm-4-long"，https://open.bigmodel.cn/dev/howuse/fileqa
* 普通问答： "glm-4v"，https://open.bigmodel.cn/dev/howuse/glm-4
* 访问视觉模型：https://open.bigmodel.cn/dev/howuse/glm-4v
*/

private const val API_KEY = "e0cd41dc7d320defa114f27ab03b2b3e.vK9tM5DvrhoCyM7j"

private const val TAG = "ZhipuQARequest"

private const val SEARCH_PROMPT = "\"\"\"\n" +
        "\n" +
        "# 以下是来自互联网的信息：\n" +
        "{search_result}\n" +
        "# 要求：\n" +
        "根据最新发布的信息回答用户问题，当回答引用了参考信息时，必须在句末使用对应的[ref_序号]来标明参考信息来源。\n" +
        "\n" +
        "\"\"\""

object ZhipuQARequestManager {

    private var historyList: ArrayList<ZhipuQAMessage<*>> = ArrayList()
    private val systemMsgList = ArrayList<ZhipuQAMessage<*>>()
    var conversationId: String? = null

    private var assistantTextBuilder: StringBuilder = StringBuilder()

    fun getZhipuRequestCall(requestData: ZhipuApiRequestData): Call<ResponseBody?> {
        return RetrofitManager.getZhipuApiService().requestZhipuAi(
            generateClientToken(API_KEY),
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
            "application/json; charset=utf-8",
            requestData
        )
    }

    fun addMessage(message: ZhipuQAMessage<*>) {
        historyList.add(message)
    }

    fun createRequestData(
        modelName: String,
        supportStream: Boolean,
        curMessage: ZhipuQAMessage<*>? = null,
        isImageRequest: Boolean = false): ZhipuApiRequestData {
        Log.i(TAG, "createRequestData: $modelName, $supportStream")
        return ZhipuApiRequestData().apply {
            model = modelName
            systemMsgList.apply {
                clear()
                setupSystemList()
                if (isImageRequest && curMessage != null) {
                    add(curMessage)
                } else {
                    addAll(historyList)
                }
            }
            messages = systemMsgList
            tools = addWebSearch()
            stream = supportStream
        }
    }

    fun getLlmResponseData(response: ResponseBody): LlmResponseData {
        toZhiPuResponseData(response)?.apply {
            return handleZhipuResponseData(this)
        }
        return LlmResponseData()
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

    private fun toZhiPuResponseData(
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
            Log.i(LlmRequestManagerImpl.TAG, "line length : " + line!!.length)
            if (line!!.length < 20) continue  //data: [DONE]

            val jsonBody = if(line!!.startsWith("data:")) line!!.substring(5) else line!!
            val gson = Gson()
            Log.i(
                LlmRequestManagerImpl.TAG,
                "ai_search_result Success, data = $line"
            )
            try {
                data =
                    gson.fromJson(jsonBody, ZhipuResponseData::class.java)
            } catch (e: Exception) {
                Log.e(LlmRequestManagerImpl.TAG, "Response Json error : ${e.stackTrace}")
            }
        }
        reader.close()
        return data
    }

    // TODO: modify the prompt
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
        Log.d(TAG, "token = $token ")
        return token
    }

    private fun printOnResponseLog(
        call: Call<ResponseBody?>,
        response: Response<ResponseBody?>
    ) {
        Log.d(TAG, "success = " + response.isSuccessful)
        Log.d(TAG, "responsemessage = ${response.message()}")
        Log.d(TAG, "responseraw = ${response.raw()}")
        Log.d(TAG, "responsecode = ${response.code()}")
        Log.d(TAG, "responseerrorBody = ${response.errorBody()}")
        Log.d(TAG, "response = $response")
        Log.d(TAG, "responseBody = ${response.body()}")
    }

    private fun printFailLog(call: Call<ResponseBody?>, t: Throwable) {
        Log.d(TAG, "onFailure: msg = " + t.message)
    }


    interface ZhipuAiCallBack {
        @Throws(IOException::class)
        fun onSuccess(response: ResponseBody?)

        fun onFail(errorMessage: String?)
    }

}