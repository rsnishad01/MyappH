package com.example.data.network

import android.util.Log
import com.example.data.PostEntity
import com.example.data.ReelVideo
import com.example.data.StoryEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OnlineSyncService {
    companion object {
        private const val TAG = "OnlineSyncService"
    }

    suspend fun fetchLiveOnlinePosts(): List<PostEntity> = withContext(Dispatchers.IO) {
        val fetchedList = mutableListOf<PostEntity>()
        try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Fetching live posts from Firestore /posts (limit 50)...")
            val snapshot = firestore.collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            Log.d(TAG, "Received ${snapshot.documents.size} post document(s) from Firestore.")
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val id = doc.id
                val authorId = (data["authorId"] as? String) ?: ""
                val authorUsername = (data["authorUsername"] as? String) ?: "creator"
                val authorDisplayName = (data["authorDisplayName"] as? String) ?: authorUsername
                val authorAvatarUrl = (data["authorAvatarUrl"] as? String) ?: ""
                val imageUrl = (data["imageUrl"] as? String) ?: ""
                val mediaUrl = (data["mediaUrl"] as? String) ?: ""
                val type = (data["type"] as? String) ?: "image"
                val text = (data["text"] as? String) ?: ""
                val caption = (data["caption"] as? String) ?: ""
                val likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0
                val isLiked = (data["isLiked"] as? Boolean) ?: (data["liked"] as? Boolean) ?: false
                val commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0
                val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val location = (data["location"] as? String) ?: ""
                val isSaved = (data["isSaved"] as? Boolean) ?: (data["saved"] as? Boolean) ?: false
                @Suppress("UNCHECKED_CAST")
                val tags = (data["tags"] as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val likedByUserIds = (data["likedByUserIds"] as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val commentedByUserIds = (data["commentedByUserIds"] as? List<String>) ?: emptyList()

                val post = PostEntity(
                    id = id,
                    authorId = authorId,
                    authorUsername = authorUsername,
                    authorDisplayName = authorDisplayName,
                    authorAvatarUrl = authorAvatarUrl,
                    imageUrl = imageUrl,
                    mediaUrl = mediaUrl,
                    type = type,
                    text = text,
                    caption = caption,
                    likesCount = likesCount,
                    isLiked = isLiked,
                    commentsCount = commentsCount,
                    timestamp = timestamp,
                    location = location,
                    isSaved = isSaved,
                    tags = tags,
                    likedByUserIds = likedByUserIds,
                    commentedByUserIds = commentedByUserIds,
                    publicId = (data["publicId"] as? String) ?: "",
                    resourceType = (data["resourceType"] as? String) ?: "",
                    format = (data["format"] as? String) ?: "",
                    bytes = (data["bytes"] as? Number)?.toLong() ?: 0L,
                    width = (data["width"] as? Number)?.toInt() ?: 0,
                    height = (data["height"] as? Number)?.toInt() ?: 0,
                    folder = (data["folder"] as? String) ?: ""
                )
                fetchedList.add(post)
                Log.d(TAG, "Successfully parsed post id=$id, type=$type, imageUrl=$imageUrl")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online posts fetch skipped/restricted: ${e.message}")
        }
        fetchedList
    }

    suspend fun fetchLiveOnlineReels(): List<ReelVideo> = withContext(Dispatchers.IO) {
        val fetchedList = mutableListOf<ReelVideo>()
        try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Fetching live reels from Firestore /reels (limit 50)...")
            val snapshot = firestore.collection("reels")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            Log.d(TAG, "Received ${snapshot.documents.size} reel document(s) from Firestore.")
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val id = doc.id
                val authorId = (data["authorId"] as? String) ?: ""
                val authorUsername = (data["authorUsername"] as? String) ?: "creator"
                val authorDisplayName = (data["authorDisplayName"] as? String) ?: authorUsername
                val authorAvatarUrl = (data["authorAvatarUrl"] as? String) ?: ""
                val videoUrl = (data["videoUrl"] as? String) ?: ""
                val thumbnailUri = (data["thumbnailUri"] as? String) ?: videoUrl
                val caption = (data["caption"] as? String) ?: ""
                val audioTitle = (data["audioTitle"] as? String) ?: "Original Audio"
                val audioArtist = (data["audioArtist"] as? String) ?: authorUsername
                val likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0
                val isLiked = (data["isLiked"] as? Boolean) ?: (data["liked"] as? Boolean) ?: false
                val commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0
                val sharesCount = (data["sharesCount"] as? Number)?.toInt() ?: 0
                val isSaved = (data["isSaved"] as? Boolean) ?: (data["saved"] as? Boolean) ?: false
                val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                @Suppress("UNCHECKED_CAST")
                val tags = (data["tags"] as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val likedByUserIds = (data["likedByUserIds"] as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val commentedByUserIds = (data["commentedByUserIds"] as? List<String>) ?: emptyList()

                if (videoUrl.isNotBlank() || thumbnailUri.isNotBlank()) {
                    val reel = ReelVideo(
                        id = id,
                        authorId = authorId,
                        authorUsername = authorUsername,
                        authorDisplayName = authorDisplayName,
                        authorAvatarUrl = authorAvatarUrl,
                        videoUrl = videoUrl,
                        thumbnailUri = thumbnailUri,
                        caption = caption,
                        audioTitle = audioTitle,
                        audioArtist = audioArtist,
                        likesCount = likesCount,
                        isLiked = isLiked,
                        commentsCount = commentsCount,
                        sharesCount = sharesCount,
                        isSaved = isSaved,
                        tags = tags,
                        likedByUserIds = likedByUserIds,
                        commentedByUserIds = commentedByUserIds,
                        timestamp = timestamp,
                        viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: (data["views"] as? Number)?.toInt() ?: 0,
                        publicId = (data["publicId"] as? String) ?: "",
                        resourceType = (data["resourceType"] as? String) ?: "",
                        format = (data["format"] as? String) ?: "",
                        bytes = (data["bytes"] as? Number)?.toLong() ?: 0L,
                        width = (data["width"] as? Number)?.toInt() ?: 0,
                        height = (data["height"] as? Number)?.toInt() ?: 0,
                        folder = (data["folder"] as? String) ?: ""
                    )
                    fetchedList.add(reel)
                    Log.d(TAG, "Successfully parsed reel id=$id, videoUrl=$videoUrl")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online reels fetch skipped/restricted: ${e.message}")
        }
        fetchedList
    }

    suspend fun fetchLiveOnlineStories(): List<StoryEntity> = withContext(Dispatchers.IO) {
        val fetchedList = mutableListOf<StoryEntity>()
        try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Fetching live stories from Firestore /stories (limit 40)...")
            val snapshot = firestore.collection("stories")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(40)
                .get()
                .await()

            Log.d(TAG, "Received ${snapshot.documents.size} story document(s) from Firestore.")
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val id = doc.id
                val userId = (data["userId"] as? String) ?: ""
                val username = (data["username"] as? String) ?: "creator"
                val userAvatarUrl = (data["userAvatarUrl"] as? String) ?: ""
                val mediaUrl = (data["mediaUrl"] as? String) ?: ""
                val isVideo = (data["isVideo"] as? Boolean) ?: (data["video"] as? Boolean) ?: false
                val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isSeen = (data["isSeen"] as? Boolean) ?: (data["seen"] as? Boolean) ?: false
                val caption = (data["caption"] as? String) ?: ""

                if (mediaUrl.isNotBlank()) {
                    val story = StoryEntity(
                        id = id,
                        userId = userId,
                        username = username,
                        userAvatarUrl = userAvatarUrl,
                        mediaUrl = mediaUrl,
                        isVideo = isVideo,
                        timestamp = timestamp,
                        isSeen = isSeen,
                        caption = caption,
                        publicId = (data["publicId"] as? String) ?: "",
                        resourceType = (data["resourceType"] as? String) ?: "",
                        format = (data["format"] as? String) ?: "",
                        bytes = (data["bytes"] as? Number)?.toLong() ?: 0L,
                        width = (data["width"] as? Number)?.toInt() ?: 0,
                        height = (data["height"] as? Number)?.toInt() ?: 0,
                        folder = (data["folder"] as? String) ?: ""
                    )
                    fetchedList.add(story)
                    Log.d(TAG, "Successfully parsed story id=$id, mediaUrl=$mediaUrl")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online stories fetch skipped/restricted: ${e.message}")
        }
        fetchedList
    }
}
