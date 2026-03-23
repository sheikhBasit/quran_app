package com.quranapp.domain.usecase.chatbot

import com.quranapp.domain.model.ChatMessage
import com.quranapp.domain.repository.ChatbotRepository
import kotlinx.coroutines.flow.Flow

class StreamChatMessageUseCase(private val repo: ChatbotRepository) {
    operator fun invoke(message: String, history: List<ChatMessage> = emptyList()): Flow<String> {
        return repo.streamMessage(message, history)
    }
}
