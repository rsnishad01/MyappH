package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun getAllDraftsFlow(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE type = :type ORDER BY updatedAt DESC")
    fun getDraftsByTypeFlow(type: String): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: String): DraftEntity?

    @Query("SELECT COUNT(*) FROM drafts")
    fun getDraftsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Update
    suspend fun updateDraft(draft: DraftEntity)

    @Delete
    suspend fun deleteDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraftById(id: String)

    @Query("DELETE FROM drafts")
    suspend fun clearAllDrafts()
}
