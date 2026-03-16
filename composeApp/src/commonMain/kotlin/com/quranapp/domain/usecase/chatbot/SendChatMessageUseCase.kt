package com.quranapp.domain.usecase.chatbot

import com.quranapp.domain.model.ChatResponse
import com.quranapp.domain.repository.ChatbotRepository

class SendChatMessageUseCase(private val repo: ChatbotRepository) {
    suspend operator fun invoke(message: String): Result<ChatResponse> {
        if (message.isBlank())
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        if (message.length > 1000)
            return Result.failure(IllegalArgumentException("Message too long (max 1000 characters)"))
        return runCatching { repo.sendMessage(message) }
    }
}
