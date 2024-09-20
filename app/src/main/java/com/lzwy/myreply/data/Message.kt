package com.lzwy.myreply.data

data class TextMessage(
    val title: String,
    val content: String,

)

data class Message(
    val id: Long = 0L,
    val title: String = "Test: ${System.currentTimeMillis()}",
    val content: String = "TestContent: ${System.currentTimeMillis()}",
    val author: Account = Account(0L, "lzwy", "LzwY", "", ""),
    val createTime: Long = System.currentTimeMillis(),
    val images: String? = null,
    val record: String? = null
)