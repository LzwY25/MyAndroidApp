package com.lzwy.myreply.data.remote

import com.lzwy.myreply.data.llm.zhipu.ZhipuApiAssistantRequestData
import com.lzwy.myreply.data.llm.zhipu.ZhipuApiMediaRequestData
import com.lzwy.myreply.data.llm.zhipu.ZhipuApiRequestData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ZhipuApiService {
    @Streaming
    @POST("chat/completions")
    fun requestZhipuAi(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Header("Content-Type") contentType: String?,
        @Body data: ZhipuApiMediaRequestData?
    ): Call<ResponseBody?>

    @Streaming
    @POST("chat/completions")
    fun requestZhipuAi(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Header("Content-Type") contentType: String?,
        @Body data: ZhipuApiRequestData?
    ): Call<ResponseBody?>

    @Multipart
    @POST("files")
    fun requestZhipuUploadFile(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Part file: MultipartBody.Part?,
        @Part("purpose") purpose: RequestBody?
    ): Call<ResponseBody?>

    @Streaming
    @DELETE("files/{fileID}")
    fun requestZhipuRemoveFile(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Path("fileID") fileID: String?,
    ): Call<ResponseBody?>

    @GET("files/{file_id}/content")
    fun requestZhipuFileContent(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Path("file_id") fileID: String?
    ): Call<ResponseBody?>

    @Streaming
    @GET("files")
    fun requestZhipuAiGetFileList(
        @Header("Authorization") jwt: String?,
        @Header("User-Agent") ua: String?,
        @Header("Content-Type") contentType: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("purpose") purpose: String = "file-extract",
    ): Call<ResponseBody?>

    @Streaming
    @POST("assistant")
    fun requestZhipuAiAssistant(
        @Header("Authorization") jwt: String?,
        //@Header("User-Agent") ua: String?,
        @Header("Content-Type") contentType: String?,
        @Body data: ZhipuApiAssistantRequestData?
    ): Call<ResponseBody?>

}