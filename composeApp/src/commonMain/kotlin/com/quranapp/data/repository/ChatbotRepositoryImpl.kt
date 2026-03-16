package com.quranapp.data.repository

import com.quranapp.data.remote.ChatbotRemoteDataSource
import com.quranapp.domain.model.ChatResponse
import com.quranapp.domain.repository.ChatbotRepository

class ChatbotRepositoryImpl(
    private val remote: ChatbotRemoteDataSource,
) : ChatbotRepository {
    override suspend fun sendMessage(message: String): ChatResponse =
        remote.sendMessage(message)
}
