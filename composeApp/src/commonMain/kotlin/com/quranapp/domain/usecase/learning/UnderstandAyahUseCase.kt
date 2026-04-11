package com.quranapp.domain.usecase.learning

import com.quranapp.data.remote.UnderstandRemoteDataSource
import kotlinx.coroutines.flow.Flow

open class UnderstandAyahUseCase(private val remote: UnderstandRemoteDataSource) {
    open operator fun invoke(
        surah: Int,
        ayah: Int,
        arabicText: String,
        translation: String,
    ): Flow<String> = remote.streamUnderstand(surah, ayah, arabicText, translation)
}
