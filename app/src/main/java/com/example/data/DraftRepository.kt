package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DraftRepository(context: Context) {
    private val draftDao: DraftDao = AppDatabase.getDatabase(context).draftDao()

    val allDrafts: Flow<List<DraftEntity>> = draftDao.getAllDraftsFlow()
    val draftsCount: Flow<Int> = draftDao.getDraftsCountFlow()

    fun getDraftsByType(type: DraftType): Flow<List<DraftEntity>> {
        return draftDao.getDraftsByTypeFlow(type.name)
    }

    suspend fun getDraftById(id: String): DraftEntity? {
        return draftDao.getDraftById(id)
    }

    suspend fun savePostDraft(
        id: String? = null,
        mediaUri: String,
        caption: String,
        location: String = "",
        filterId: String = "normal"
    ): String {
        val draftId = id?.ifBlank { null } ?: "draft_post_${UUID.randomUUID()}"
        val draft = DraftEntity(
            id = draftId,
            type = DraftType.POST.name,
            mediaUri = mediaUri,
            caption = caption,
            location = location,
            filterId = filterId,
            thumbnailUri = mediaUri,
            updatedAt = System.currentTimeMillis()
        )
        draftDao.insertDraft(draft)
        return draftId
    }

    suspend fun saveReelDraft(
        id: String? = null,
        mediaUri: String,
        caption: String,
        audioTitle: String = "",
        audioArtist: String = "",
        thumbnailUri: String = ""
    ): String {
        val draftId = id?.ifBlank { null } ?: "draft_reel_${UUID.randomUUID()}"
        val draft = DraftEntity(
            id = draftId,
            type = DraftType.REEL.name,
            mediaUri = mediaUri,
            caption = caption,
            audioTitle = audioTitle,
            audioArtist = audioArtist,
            thumbnailUri = thumbnailUri.ifBlank { mediaUri },
            updatedAt = System.currentTimeMillis()
        )
        draftDao.insertDraft(draft)
        return draftId
    }

    suspend fun deleteDraft(id: String) {
        draftDao.deleteDraftById(id)
    }

    suspend fun clearAll() {
        draftDao.clearAllDrafts()
    }
}
