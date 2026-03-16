package com.quranapp.data.remote

import com.quranapp.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

// ─── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class ChatRequestDto(val message: String)

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

private const val BASE_URL = "https://your-domain.com/api"   // set from BuildConfig

class ChatbotRemoteDataSource(private val client: HttpClient) {
    suspend fun sendMessage(message: String): ChatResponse {
        val dto: ChatResponseDto = client.post("$BASE_URL/chat/") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequestDto(message = message))
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
}
