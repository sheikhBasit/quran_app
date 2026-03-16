package com.quranapp.di

import com.quranapp.data.remote.ChatbotRemoteDataSource
import com.quranapp.data.repository.*
import com.quranapp.domain.repository.*
import com.quranapp.domain.usecase.chatbot.SendChatMessageUseCase
import com.quranapp.domain.usecase.hadith.*
import com.quranapp.domain.usecase.prayer.*
import com.quranapp.domain.usecase.qibla.GetQiblaDirectionUseCase
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.usecase.search.SearchUseCase
import com.quranapp.domain.usecase.userdata.*
import com.quranapp.viewmodel.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single { ChatbotRemoteDataSource(get()) }
}

val repositoryModule = module {
    single<ChatbotRepository> { ChatbotRepositoryImpl(get()) }
    // QuranRepository, HadithRepository etc. added in Phase 2 after SQLDelight setup
}

val useCaseModule = module {
    // Prayer & Qibla (no repo needed — pure calculation)
    factory { GetPrayerTimesUseCase() }
    factory { GetNextPrayerUseCase(get()) }
    factory { GetQiblaDirectionUseCase() }
    // Chatbot
    factory { SendChatMessageUseCase(get()) }
}

val viewModelModule = module {
    factory { ChatbotViewModel(get()) }
    // Other ViewModels added as each phase completes
}

val appModule = module {
    includes(networkModule, repositoryModule, useCaseModule, viewModelModule)
}
