package com.quranapp.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
data class UnderstandRequestDto(
    val surah: Int,
    val ayah: Int,
    val arabic_text: String,
    val translation: String,
)

class UnderstandRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    fun streamUnderstand(
        surah: Int,
        ayah: Int,
        arabicText: String,
        translation: String,
    ): Flow<String> = flow {
        client.preparePost("$baseUrl/chat/understand-ayah") {
            contentType(ContentType.Application.Json)
            setBody(UnderstandRequestDto(surah, ayah, arabicText, translation))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line()
                if (line != null && line.startsWith("data: ")) {
                    val token = line.substring(6)
                    if (token.isNotEmpty() && token != "[DONE]") emit(token)
                }
            }
        }
    }
}
