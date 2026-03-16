package com.quranapp.domain.usecase.userdata

import com.quranapp.domain.repository.UserDataRepository

class ToggleBookmarkUseCase(private val repo: UserDataRepository) {
    suspend operator fun invoke(type: String, referenceId: Long): Result<Boolean> =
        runCatching { repo.toggleBookmark(type, referenceId) }
}

class SetHighlightUseCase(private val repo: UserDataRepository) {
    suspend operator fun invoke(ayahId: Long, color: String?): Result<Unit> =
        runCatching {
            if (color == null) repo.removeHighlight(ayahId)
            else repo.setHighlight(ayahId, color)
        }
}

class SaveNoteUseCase(private val repo: UserDataRepository) {
    suspend operator fun invoke(type: String, referenceId: Long, content: String): Result<Unit> {
        if (content.isBlank())
            return Result.failure(IllegalArgumentException("Note content cannot be empty"))
        return runCatching { repo.saveNote(type, referenceId, content) }
    }
}
