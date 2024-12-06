package com.lzwy.myreply.data.account

interface AccountRepository {
    fun getAllAccount(): List<Account>
    fun getCurrentAccount(): List<Account>
}