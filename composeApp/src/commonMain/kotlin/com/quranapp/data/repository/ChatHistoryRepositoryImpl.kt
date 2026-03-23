package com.quranapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.ChatMessage
import com.quranapp.domain.model.ChatRole
import com.quranapp.domain.model.ChatSession
import com.quranapp.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatHistoryRepositoryImpl(
    private val db: QuranDatabase
) : ChatHistoryRepository {
    private val queries = db.chatSessionQueries

    override fun getAllSessions(): Flow<List<ChatSession>> {
        return queries.selectAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { 
                    ChatSession(it.id, it.title, it.created_at, it.last_message, it.last_updated) 
                }
            }
    }

    override suspend fun getMessagesBySession(sessionId: String): List<ChatMessage> {
        return queries.selectMessagesBySession(sessionId).executeAsList().map {
            ChatMessage(
                id = it.id,
                role = ChatRole.valueOf(it.role),
                content = it.content,
                timestamp = it.timestamp
            )
        }
    }

    override suspend fun saveSession(session: ChatSession) {
        queries.insertSession(
            session.id,
            session.title,
            session.createdAt,
            session.lastMessage,
            session.lastUpdated
        )
    }

    override suspend fun saveMessage(sessionId: String, message: ChatMessage) {
        queries.insertMessage(
            message.id,
            sessionId,
            message.role.name,
            message.content,
            message.timestamp
        )
        // Update session metadata as well
        queries.updateSessionMetadata(message.content, message.timestamp, sessionId)
    }

    override suspend fun deleteSession(sessionId: String) {
        queries.deleteSession(sessionId)
    }
}
