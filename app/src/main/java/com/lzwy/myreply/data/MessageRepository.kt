package com.lzwy.myreply.data

import kotlinx.coroutines.flow.Flow

/**
 * An Interface contract to get all emails info for a User.
 */
interface MessageRepository {
    fun getAllEmails(): Flow<List<Message>>
    fun getCategoryEmails(): Flow<List<Message>>
    fun getAllFolders(): List<String>
    fun getEmailFromId(id: Long): Flow<Message?>
}
