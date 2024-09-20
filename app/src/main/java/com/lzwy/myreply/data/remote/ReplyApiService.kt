package com.lzwy.myreply.data.remote

import com.lzwy.myreply.data.Account
import com.lzwy.myreply.data.Message
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ReplyApiService {

    @GET("/message/getAllMessages")
    suspend fun getAllMessages(): ResponseMessage<List<Message>>

    @Multipart
    @POST("/message/uploadMessage")
    suspend fun uploadMessage(
        @Part("sender") sender: Long,
        @Part("title") title: String,
        @Part("body") body: String,
        @Part("time") time: Long,
        @Part images: List<MultipartBody.Part>?,
        @Part record: MultipartBody.Part?
    ): ResponseMessage<Void>

    // for record
    @GET
    @Streaming
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody?

    @GET("/account/getAllAccounts")
    suspend fun getAllAccounts(): Response<List<Account>>

    @GET("/account/getAccountById")
    suspend fun getAccountById(id: Long): Response<Account?>
}