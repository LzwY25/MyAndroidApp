package com.lzwy.myreply.data

import kotlinx.coroutines.flow.Flow

class MessageRepositoryImpl: MessageRepository {
    override fun getAllEmails(): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    override fun getCategoryEmails(): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    override fun getAllFolders(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getEmailFromId(id: Long): Flow<Message?> {
        TODO("Not yet implemented")
    }
}