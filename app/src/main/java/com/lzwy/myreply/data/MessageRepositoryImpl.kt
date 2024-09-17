package com.lzwy.myreply.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MessageRepositoryImpl: MessageRepository {
    override fun getAllEmails(): Flow<List<Message>> = flow {
        for (i in 0..5) {
            emit(listOf(Message(id = i.toLong())))
            delay(200)
        }
    }

    override fun getCategoryEmails(): Flow<List<Message>> = flow {

    }

    override fun getAllFolders(): List<String> {
        return emptyList()
    }

    override fun getEmailFromId(id: Long): Flow<Message?> = flow {  }
}