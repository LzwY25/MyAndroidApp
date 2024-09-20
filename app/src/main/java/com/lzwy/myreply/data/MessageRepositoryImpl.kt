package com.lzwy.myreply.data

import android.util.Log
import com.lzwy.myreply.data.remote.RetrofitManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MessageRepositoryImpl: MessageRepository {
    override fun getAllEmails(): Flow<List<Message>> = flow {
    }

    override fun getCategoryEmails(): Flow<List<Message>> = flow {

    }

    override fun getAllFolders(): List<String> {
        return emptyList()
    }

    override fun getEmailFromId(id: Long): Flow<Message?> = flow {  }
}