package com.lzwy.myreply.data

data class Message(
    val id: Long,
    val title: String,
    val content: String,
    val author: Account,
    val createTime: Long
)