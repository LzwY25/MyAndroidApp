package com.lzwy.myreply.data.llm

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import android.util.Log
import com.lzwy.myreply.data.remote.RetrofitManager
import com.lzwy.myreply.data.remote.ZhipuApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

private const val API_KEY = "e0cd41dc7d320defa114f27ab03b2b3e.vK9tM5DvrhoCyM7j"

class ZhipuQARequest {

    private val systemMsgList = ArrayList<ZhipuQAMessage<*>>()

    /*
    * 文件问答："glm-4-long"，https://open.bigmodel.cn/dev/howuse/fileqa
    * 普通问答： "glm-4v"，https://open.bigmodel.cn/dev/howuse/glm-4
    */
    fun requestZhipuOpenModel(
        modelName: String,
        messages: ArrayList<ZhipuQAMessage<*>>,
        callBack: ZhipuAiCallBack
    ) {
        Log.i(TAG, "requestZhipuOpenModel request start")
        val requestData = ZhipuApiRequestData()
        val api = RetrofitManager.getZhipuApiService()

        requestData.model = modelName
        Log.i(TAG, "model is $modelName")

        systemMsgList.clear()
        if (systemMsgList.size <= 0) {
            setupSystemList()
        }
        systemMsgList.addAll(messages)
        requestData.messages = systemMsgList

        requestData.tools = addWebSearch()
        requestData.stream = true
        api.requestZhipuAi(
            generateClientToken(API_KEY),
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
            "application/json; charset=utf-8",
            requestData
        ).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>, response: Response<ResponseBody?>
            ) {
                printOnResponseLog(call, response)
                if (response.body() != null) {
                    callBack.onSuccess(response.body())
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                printFailLog(call, t)
                callBack.onFail(t.message)
            }
        })
    }

    fun requestZhipuOpenModelWithCoroutine(
        coroutineScope: CoroutineScope,
        modelName: String,
        messages: ArrayList<ZhipuQAMessage<*>>,
        callBack: ZhipuAiCallBack
    ) {
        coroutineScope.launch {
            Log.i(TAG, "requestZhipuOpenModel request start")
            val requestData = ZhipuApiRequestData()
            val api = RetrofitManager.getZhipuApiService()

            requestData.model = modelName
            Log.i(TAG, "model is $modelName")

            systemMsgList.clear()
            if (systemMsgList.size <= 0) {
                setupSystemList()
            }
            systemMsgList.addAll(messages)
            requestData.messages = systemMsgList

            requestData.tools = addWebSearch()
            requestData.stream = true
            val call = api.requestZhipuAiWithCoroutine(
                generateClientToken(API_KEY),
                "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
                "application/json; charset=utf-8",
                requestData
            )
            val response = call.execute()
            if (response.isSuccessful) {
                Log.i(TAG, "Response successful")
                withContext(Dispatchers.IO) {
                    printOnResponseLog(call, response)
                    if (response.body() != null) {
                        callBack.onSuccess(response.body())
                    }
                }
            }
        }
    }

    /*
    * 访问视觉模型：https://open.bigmodel.cn/dev/howuse/glm-4v
    */
    fun requestZhipuImage(
        messages: ArrayList<ZhipuQAMessage<*>>,
        callBack: ZhipuAiCallBack
    ) {
        Log.i(TAG, "requestZhipuImage start")
        val requestData = ZhipuApiMediaRequestData()
        val api = RetrofitManager.getZhipuApiService()
        requestData.model = "glm-4v"
        requestData.messages = messages
        requestData.tools = addWebSearch()
        requestData.stream = true

        Log.d(TAG, "request data :$requestData")

        api.requestZhipuAi(
            generateClientToken(API_KEY),
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
            "application/json; charset=utf-8",
            requestData
        ).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                printOnResponseLog(call, response)

                if (response.body() != null) {
                    callBack.onSuccess(response.body())
                } else {
                    callBack.onFail("response body is null")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                printFailLog(call, t)
                callBack.onFail(t.message)
            }
        })
    }

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

    fun requestPromptQuestion(
        model: String,
        prompt: String,
        zhipuMessageList: ArrayList<ZhipuQAMessage<*>>,
        callBack: ZhipuAiCallBack
    ) {
        Log.i(TAG, "requestAdditionalQuestion start")

        val messagesList = ArrayList<ZhipuQAMessage<*>>()
        messagesList.add(
            ZhipuQAMessage.plainMessage(
                role = Role.system,
                content = Content.create(prompt)
            )
        )
        messagesList.addAll(zhipuMessageList)

        val requestData = ZhipuApiRequestData()
        val api = RetrofitManager.getZhipuApiService()
        requestData.model = model
        requestData.messages = messagesList
        requestData.stream = false
        api.requestZhipuAi(
            generateClientToken(API_KEY),
            "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt",
            "application/json; charset=utf-8",
            requestData
        ).enqueue((object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                printOnResponseLog(call, response)

                if (response.body() != null) {
                    callBack.onSuccess(response.body())
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                printFailLog(call, t)
                callBack.onFail(t.message)
            }
        }))
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
        Log.d(TAG, "response = ${response.toString()}")
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

    companion object {
        const val TAG = "ZhipuQARequest"

        private const val SEARCH_PROMPT = "\"\"\"\n" +
                "\n" +
                "# 以下是来自互联网的信息：\n" +
                "{search_result}\n" +
                "# 要求：\n" +
                "根据最新发布的信息回答用户问题，当回答引用了参考信息时，必须在句末使用对应的[ref_序号]来标明参考信息来源。\n" +
                "\n" +
                "\"\"\""

        private const val ADDITIONAL_QUESTION_PROMPT = "\"\"\"\n" +
                "你的任务是 根据用户的提问和你的回复，使用[q_序号]给出用户可能继续提问的三个问题，\n" +
                " \"\"\""
    }
}