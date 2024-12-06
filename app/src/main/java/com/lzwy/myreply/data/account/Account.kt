package com.lzwy.myreply.data.account

data class Account(
    val id: Long,
    val accountName: String,
    val displayName: String,
    val email: String,
    val avatar: String
)