package com.lzwy.myreply.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitManager {

    private const val REPLY_API_URL = ""
    private const val ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/"

    private var replyApiService: ReplyApiService? = null
    private var zhipuApiService: ZhipuApiService? = null

    fun getZhipuApiService(): ZhipuApiService {
        if (zhipuApiService == null) {
            synchronized(this) {
                if (zhipuApiService == null) {
                    zhipuApiService =
                        buildRetrofit(
                            ZHIPU_API_URL
                        ).create(ZhipuApiService::class.java)
                }
            }
        }
        return zhipuApiService!!
    }

    fun getReplyApiService(): ReplyApiService {
        if (replyApiService == null) {
            synchronized(this) {
                if (replyApiService == null) {
                    replyApiService =
                        buildRetrofit(
                            REPLY_API_URL
                        ).create(ReplyApiService::class.java)
                }
            }
        }

        return replyApiService!!
    }

    private fun buildRetrofit(url: String): Retrofit {

        // we extend the time to wait for the LLM request. can adjust the timeout accordingly.
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
/*            .addInterceptor(Interceptor { chain ->
                val response = chain.proceed(chain.request())
                val content = response.body?.string() ?: ""
                val type = response.body?.contentType()
                Log.i("LZWY", "response: isSuccessful: ${response.isSuccessful},  ${response.body?.string()}")
                return@Interceptor response.newBuilder()
                    .body(ResponseBody.create(type, content))
                    .build()
            })*/
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}