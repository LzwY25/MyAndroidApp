package com.lzwy.myreply.data

data class ChatItem(
    val isUser: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
