package com.lzwy.myreply.data.remote

data class ResponseMessage<T>(
    val code: String,
    val message: String,
    val content: T? = null
)
