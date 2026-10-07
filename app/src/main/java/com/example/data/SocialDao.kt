package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM reels ORDER BY timestamp DESC")
    fun getAllReelsFlow(): Flow<List<ReelVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelVideo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: ReelVideo)

    @Query("DELETE FROM reels WHERE id = :reelId")
    suspend fun deleteReel(reelId: String)

    @Query("SELECT * FROM stories WHERE timestamp > :sinceTimestamp ORDER BY timestamp DESC")
    fun getActiveStoriesFlow(sinceTimestamp: Long): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("SELECT * FROM chat_threads ORDER BY lastMessageTimestamp DESC")
    fun getAllChatThreadsFlow(): Flow<List<ChatThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatThread(thread: ChatThreadEntity)

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThreadFlow(threadId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserData)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserData?

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM posts WHERE id LIKE 'sample_%' OR authorId LIKE 'u_%'")
    suspend fun deleteAllSamplePosts()

    @Query("DELETE FROM reels WHERE id LIKE 'sample_%' OR authorId LIKE 'u_%'")
    suspend fun deleteAllSampleReels()

    @Query("DELETE FROM stories WHERE id LIKE 'sample_%' OR userId LIKE 'u_%'")
    suspend fun deleteAllSampleStories()
}
