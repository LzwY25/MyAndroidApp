package com.lzwy.myreply.data.remote

// TODO: is it necessary?
data class ServerResponse<T>(
    val resultCode: Int,
    val data: T
)
