package com.quranapp.data.remote

import com.quranapp.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

// ─── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class ChatMessageDto(val role: String, val content: String)

@Serializable
data class ChatRequestDto(
    val message: String,
    val history: List<ChatMessageDto> = emptyList()
)

@Serializable
data class AyahSourceDto(val surah: Int, val ayah: Int)

@Serializable
data class HadithSourceDto(val collection: String, val number: Int)

@Serializable
data class TafsirSourceDto(val surah: Int, val ayah: Int, val book: String)

@Serializable
data class ChatSourcesDto(
    val ayahs: List<AyahSourceDto> = emptyList(),
    val hadiths: List<HadithSourceDto> = emptyList(),
    val tafsir: List<TafsirSourceDto> = emptyList(),
)

@Serializable
data class ChatResponseDto(val answer: String, val sources: ChatSourcesDto)

// ─── Data Source ──────────────────────────────────────────────────────────────

class ChatbotRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun sendMessage(message: String, history: List<ChatMessageDto> = emptyList()): ChatResponse {
        val dto: ChatResponseDto = client.post("$baseUrl/chat/") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequestDto(message = message, history = history))
        }.body()

        return ChatResponse(
            answer = dto.answer,
            sources = ChatSources(
                ayahs = dto.sources.ayahs.map { AyahReference(it.surah, it.ayah) },
                hadiths = dto.sources.hadiths.map { HadithReference(it.collection, it.number) },
                tafsir = dto.sources.tafsir.map { TafsirReference(it.surah, it.ayah, it.book) },
            )
        )
    }

    fun streamMessage(message: String, history: List<ChatMessageDto> = emptyList()): Flow<String> = flow {
        println("CHATBOT_URL: Connecting to $baseUrl/chat/stream")
        try {
            client.preparePost("$baseUrl/chat/stream") {
                contentType(ContentType.Application.Json)
                setBody(ChatRequestDto(message = message, history = history))
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line()
                    if (line != null && line.startsWith("data: ")) {
                        val token = line.substring(6)
                        if (token.isNotEmpty()) {
                            emit(token)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("CHATBOT_REMOTE_ERROR: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
