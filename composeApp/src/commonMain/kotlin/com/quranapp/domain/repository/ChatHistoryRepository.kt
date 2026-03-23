package com.quranapp.domain.repository

import com.quranapp.domain.model.ChatMessage
import com.quranapp.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getMessagesBySession(sessionId: String): List<ChatMessage>
    suspend fun saveSession(session: ChatSession)
    suspend fun saveMessage(sessionId: String, message: ChatMessage)
    suspend fun deleteSession(sessionId: String)
}
