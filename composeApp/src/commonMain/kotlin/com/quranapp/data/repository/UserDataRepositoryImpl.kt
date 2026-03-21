package com.quranapp.data.repository

import com.quranapp.db.QuranDatabase
import com.quranapp.domain.model.Bookmark
import com.quranapp.domain.model.Note
import com.quranapp.domain.repository.UserDataRepository

class UserDataRepositoryImpl(
    private val db: QuranDatabase
) : UserDataRepository {

    override suspend fun toggleBookmark(type: String, referenceId: Long): Boolean {
        val current = isBookmarked(type, referenceId)
        if (current) {
            db.userDataQueries.deleteBookmark(type, referenceId)
        } else {
            db.userDataQueries.insertBookmark(type, referenceId)
        }
        return !current
    }

    override suspend fun isBookmarked(type: String, referenceId: Long): Boolean {
        return db.userDataQueries.isBookmarked(type, referenceId).executeAsOne() > 0
    }

    override suspend fun getAllBookmarks(type: String): List<Bookmark> {
        return db.userDataQueries.selectAllBookmarks(type).executeAsList().map {
            Bookmark(
                id = it.id,
                type = it.type,
                referenceId = it.reference_id,
                createdAt = it.created_at
            )
        }
    }

    override suspend fun setHighlight(ayahId: Long, color: String) {
        db.userDataQueries.upsertHighlight(ayahId, color)
    }

    override suspend fun removeHighlight(ayahId: Long) {
        db.userDataQueries.deleteHighlight(ayahId)
    }

    override suspend fun getHighlightColor(ayahId: Long): String? {
        return db.userDataQueries.selectHighlightColor(ayahId).executeAsOneOrNull()
    }

    override suspend fun saveNote(type: String, referenceId: Long, content: String) {
        db.userDataQueries.upsertNote(type, referenceId, content)
    }

    override suspend fun getNote(type: String, referenceId: Long): Note? {
        return db.userDataQueries.selectNote(type, referenceId).executeAsOneOrNull()?.let {
            Note(
                id = it.id,
                type = it.type,
                referenceId = it.reference_id,
                content = it.content,
                updatedAt = it.updated_at
            )
        }
    }

    override suspend fun deleteNote(type: String, referenceId: Long) {
        db.userDataQueries.deleteNote(type, referenceId)
    }
}
