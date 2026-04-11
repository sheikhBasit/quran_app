package com.quranapp.di

import com.quranapp.data.remote.ChatbotRemoteDataSource
import com.quranapp.data.remote.UnderstandRemoteDataSource
import com.quranapp.data.repository.*
import com.quranapp.data.repository.LearningRepositoryImpl
import com.quranapp.domain.repository.LearningRepository
import com.quranapp.domain.usecase.learning.*
import com.quranapp.viewmodel.LearningViewModel
import com.quranapp.db.QuranDatabase
import com.quranapp.domain.repository.*
import com.quranapp.domain.usecase.chatbot.SendChatMessageUseCase
import com.quranapp.domain.usecase.chatbot.StreamChatMessageUseCase
import com.quranapp.domain.usecase.hadith.*
import com.quranapp.domain.usecase.prayer.*
import com.quranapp.domain.usecase.qibla.GetQiblaDirectionUseCase
import com.quranapp.domain.usecase.quran.*
import com.quranapp.domain.usecase.search.SearchUseCase
import com.quranapp.domain.usecase.userdata.*
import com.quranapp.util.DatabaseDriverFactory
import com.quranapp.util.SettingsStore
import com.quranapp.viewmodel.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import com.russhwolf.settings.ObservableSettings

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
    }
    single { ChatbotRemoteDataSource(get(), get(org.koin.core.qualifier.named("baseUrl"))) }
    single { UnderstandRemoteDataSource(get(), get(org.koin.core.qualifier.named("baseUrl"))) }
}

val settingsModule = module {
    single<ObservableSettings> { get<SettingsStore>().createSettings() }
}

val databaseModule = module {
    single { QuranDatabase(get<DatabaseDriverFactory>().createDriver()) }
}

val repositoryModule = module {
    single<ChatbotRepository> { ChatbotRepositoryImpl(get()) }
    single<ChatHistoryRepository> { ChatHistoryRepositoryImpl(get()) }
    single<QuranRepository> { QuranRepositoryImpl(get()) }
    single<HadithRepository> { HadithRepositoryImpl(get()) }
    single<UserDataRepository> { UserDataRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get<ObservableSettings>()) }
    single<LearningRepository> { LearningRepositoryImpl(get()) }
}

val useCaseModule = module {
    // Prayer & Qibla
    factory { GetPrayerTimesUseCase() }
    factory { GetNextPrayerUseCase(get()) }
    factory { GetQiblaDirectionUseCase() }
    
    // Chatbot
    factory { SendChatMessageUseCase(get()) }
    factory { StreamChatMessageUseCase(get()) }
    
    // Quran
    factory { GetSurahListUseCase(get()) }
    factory { GetAyahsBySurahUseCase(get()) }
    factory { GetAyahsForPageUseCase(get()) }
    factory { GetTafsirUseCase(get()) }
    
    // Hadith
    factory { GetCollectionsUseCase(get()) }
    factory { GetHadithByChapterUseCase(get()) }
    factory { GetHadithChaptersUseCase(get()) }
    
    // User Data
    factory { ToggleBookmarkUseCase(get()) }
    factory { SetHighlightUseCase(get()) }
    factory { SaveNoteUseCase(get()) }

    // Search
    factory { SearchUseCase(get()) }

    // Learning
    factory { GetWordMeaningsUseCase(get()) }
    factory { AddToWordBankUseCase(get()) }
    factory { GetDueFlashcardsUseCase(get()) }
    factory { RecordReviewUseCase(get()) }
    factory { MarkAyahStudiedUseCase(get()) }
    factory { GetProgressUseCase(get()) }
    factory { UnderstandAyahUseCase(get()) }
}

val viewModelModule = module {
    factory { ChatbotViewModel(get(), get()) }
    factory { QuranViewModel(get(), get(), get(), get(), get()) }
    factory { HadithViewModel(get(), get(), get()) }
    factory { SearchViewModel(get()) }
    factory { SettingsViewModel(get()) }
    factory { PrayerViewModel(get(), get(), get()) }
    factory { QiblaViewModel(get(), get(), get()) }
    factory { LearningViewModel(get(), get(), get(), get(), get(), get(), get()) }
}

val appModule = module {
    includes(networkModule, databaseModule, settingsModule, repositoryModule, useCaseModule, viewModelModule, platformModule)
}

expect val platformModule: org.koin.core.module.Module
