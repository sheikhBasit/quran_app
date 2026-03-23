package com.quranapp.data.repository

import com.quranapp.data.remote.ChatbotRemoteDataSource
import com.quranapp.data.remote.ChatMessageDto
import com.quranapp.domain.model.ChatMessage
import com.quranapp.domain.model.ChatResponse
import com.quranapp.domain.repository.ChatbotRepository
import kotlinx.coroutines.flow.Flow

class ChatbotRepositoryImpl(
    private val remoteDataSource: ChatbotRemoteDataSource,
) : ChatbotRepository {
    override suspend fun sendMessage(message: String, history: List<ChatMessage>): ChatResponse {
        val historyDto = history.map {
            ChatMessageDto(role = it.role.name.lowercase(), content = it.content)
        }
        return remoteDataSource.sendMessage(message, historyDto)
    }

    override fun streamMessage(message: String, history: List<ChatMessage>): Flow<String> {
        val historyDto = history.map {
            ChatMessageDto(role = it.role.name.lowercase(), content = it.content)
        }
        return remoteDataSource.streamMessage(message, historyDto)
    }
}
