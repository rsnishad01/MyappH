package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.ChatMessageEntity
import com.example.data.CommentEntity
import com.example.data.PostEntity
import com.example.data.ReelVideo
import com.example.data.StoryEntity
import com.example.data.UserData
import com.example.data.CallEntity
import com.example.data.CallStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreRepository(private val context: Context) {
    companion object {
        private const val TAG = "FirestoreRepository"
    }

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }

    private val _realtimeMessages = MutableStateFlow<Map<String, List<ChatMessageEntity>>>(emptyMap())
    val realtimeMessages: Flow<Map<String, List<ChatMessageEntity>>> = _realtimeMessages.asStateFlow()
    private val activeListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    fun getCanonicalThreadId(threadId: String, currentUserId: String): String {
        if (threadId.isBlank()) return "general"
        if (threadId.startsWith("thread_")) return threadId
        if (currentUserId.isNotBlank() && threadId != currentUserId) {
            val list = listOf(currentUserId, threadId).sorted()
            return "thread_${list[0]}_${list[1]}"
        }
        return threadId
    }

    fun startListeningToThread(threadId: String, currentUserId: String, onMessagesReceived: (List<ChatMessageEntity>) -> Unit) {
        val db = firestore ?: return
        val canonicalId = getCanonicalThreadId(threadId, currentUserId)

        // Remove existing listener for this thread if any
        activeListeners[threadId]?.remove()

        try {
            val listener = db.collection("chats")
                .document(canonicalId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to messages failed for $canonicalId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            val senderId = data["senderId"] as? String ?: ""
                            ChatMessageEntity(
                                messageId = doc.id,
                                threadId = threadId,
                                senderId = senderId,
                                senderUsername = data["senderUsername"] as? String ?: "",
                                text = data["text"] as? String ?: "",
                                mediaUrl = data["mediaUrl"] as? String ?: "",
                                isMedia = data["isMedia"] as? Boolean ?: false,
                                isVoiceMessage = data["isVoiceMessage"] as? Boolean ?: false,
                                voiceDurationSeconds = (data["voiceDurationSeconds"] as? Number)?.toInt() ?: 0,
                                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isOutgoing = (senderId == currentUserId && currentUserId.isNotBlank()),
                                isRead = true,
                                reaction = data["reaction"] as? String ?: "",
                                sendStatus = "SENT"
                            )
                        }
                        val currentMap = _realtimeMessages.value.toMutableMap()
                        currentMap[threadId] = messages
                        _realtimeMessages.value = currentMap
                        onMessagesReceived(messages)
                    }
                }
            activeListeners[threadId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Error starting message listener: ${e.message}")
        }
    }

    fun stopListeningToThread(threadId: String) {
        activeListeners[threadId]?.remove()
        activeListeners.remove(threadId)
    }

    /**
     * Saves user login profile to Firebase Firestore at path: /users/{userId}
     */
    suspend fun syncUserToFirestore(user: UserData) = withContext(Dispatchers.IO) {
        if (user.userId.isBlank()) return@withContext
        try {
            val userMap = hashMapOf<String, Any>(
                "userId" to user.userId,
                "username" to user.username,
                "displayName" to user.displayName,
                "bio" to user.bio,
                "avatarUrl" to user.avatarUrl,
                "followersCount" to user.followersCount,
                "followingCount" to user.followingCount,
                "postsCount" to user.postsCount,
                "isVerified" to user.isVerified,
                "website" to user.website,
                "location" to user.location,
                "email" to user.email,
                "isDeactivated" to user.isDeactivated,
                "deactivationTimestamp" to user.deactivationTimestamp,
                "lastLoginAt" to System.currentTimeMillis()
            )
            Log.d(TAG, "Writing user profile to Firestore /users/${user.userId}, avatarUrl=${user.avatarUrl}")
            firestore?.collection("users")?.document(user.userId)?.set(userMap, SetOptions.merge())?.await()
            Log.d(TAG, "User profile successfully saved to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile: ${e.message}")
        }
    }

    suspend fun deleteUserFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            firestore?.collection("users")?.document(userId)?.delete()?.await()
            Log.d(TAG, "User $userId deleted permanently from Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user from Firestore: ${e.message}")
        }
    }

    /**
     * Retrieves saved user from Firebase Firestore at path: /users/{userId}
     */
    suspend fun getUserFromFirestore(userId: String): UserData? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        try {
            val doc = firestore?.collection("users")?.document(userId)?.get()?.await()
            if (doc != null && doc.exists()) {
                val data = doc.data ?: return@withContext null
                return@withContext UserData(
                    userId = doc.id,
                    username = (data["username"] as? String) ?: "user",
                    displayName = (data["displayName"] as? String) ?: "Creator",
                    bio = (data["bio"] as? String) ?: "",
                    avatarUrl = (data["avatarUrl"] as? String) ?: "",
                    followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                    isVerified = (data["isVerified"] as? Boolean) ?: false,
                    website = (data["website"] as? String) ?: "",
                    location = (data["location"] as? String) ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user $userId from Firestore: ${e.message}")
        }
        null
    }

    suspend fun findUserBySearchQuery(query: String): UserData? = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext null
            val snapshot = db.collection("users").get().await()
            val cleanQuery = query.trim().lowercase()
            if (cleanQuery.isBlank()) return@withContext null

            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val uId = doc.id.lowercase()
                val uname = (data["username"] as? String)?.lowercase() ?: ""
                val dname = (data["displayName"] as? String)?.lowercase() ?: ""
                val fName = (data["fullName"] as? String)?.lowercase() ?: ""

                if (uId == cleanQuery || 
                    uname == cleanQuery || 
                    uname.contains(cleanQuery) || 
                    dname == cleanQuery ||
                    dname.contains(cleanQuery) ||
                    fName == cleanQuery ||
                    fName.contains(cleanQuery)) {
                    
                    return@withContext UserData(
                        userId = doc.id,
                        username = (data["username"] as? String) ?: "user",
                        displayName = (data["displayName"] as? String) ?: "Creator",
                        bio = (data["bio"] as? String) ?: "",
                        avatarUrl = (data["avatarUrl"] as? String) ?: "",
                        followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                        followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                        postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                        isVerified = (data["isVerified"] as? Boolean) ?: false,
                        website = (data["website"] as? String) ?: "",
                        location = (data["location"] as? String) ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search query error: ${e.message}")
        }
        null
    }

    /**
     * Publishes Post to Firebase Firestore at:
     * 1. /posts/{post.id}
     * 2. /users/{authorId}/posts/{post.id}
     * And increments postsCount on /users/{authorId}
     */
    suspend fun publishPost(post: PostEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: run {
                Log.e(TAG, "Firestore is not available for publishing post.")
                return@withContext false
            }
            Log.d(TAG, "Publishing post to Firestore: id=${post.id}, imageUrl=${post.imageUrl}")
            
            val postMap = hashMapOf<String, Any>(
                "id" to post.id,
                "authorId" to post.authorId,
                "authorUsername" to post.authorUsername,
                "authorDisplayName" to post.authorDisplayName,
                "authorAvatarUrl" to post.authorAvatarUrl,
                "imageUrl" to post.imageUrl,
                "mediaUrl" to post.mediaUrl,
                "type" to post.type,
                "text" to post.text,
                "caption" to post.caption,
                "likesCount" to post.likesCount,
                "isLiked" to post.isLiked,
                "commentsCount" to post.commentsCount,
                "timestamp" to post.timestamp,
                "location" to post.location,
                "isSaved" to post.isSaved,
                "tags" to post.tags,
                "likedByUserIds" to post.likedByUserIds,
                "commentedByUserIds" to post.commentedByUserIds,
                "isTextOnly" to post.isTextOnly,
                // Media Metadata
                "publicId" to post.publicId,
                "resourceType" to post.resourceType,
                "format" to post.format,
                "bytes" to post.bytes,
                "width" to post.width,
                "height" to post.height,
                "folder" to post.folder
            )

            // Global posts collection
            db.collection("posts").document(post.id).set(postMap).await()
            Log.d(TAG, "Successfully saved post to /posts/${post.id}")

            // User subcollection for posts
            if (post.authorId.isNotBlank()) {
                db.collection("users")
                    .document(post.authorId)
                    .collection("posts")
                    .document(post.id)
                    .set(postMap)
                    .await()

                // Increment user postsCount
                db.collection("users")
                    .document(post.authorId)
                    .update("postsCount", FieldValue.increment(1))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed publishing post to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Publishes Story to Firebase Firestore at:
     * 1. /stories/{story.id}
     * 2. /users/{userId}/stories/{story.id}
     */
    suspend fun publishStory(story: StoryEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext false
            Log.d(TAG, "Publishing story to Firestore: id=${story.id}, mediaUrl=${story.mediaUrl}")

            val storyMap = hashMapOf<String, Any>(
                "id" to story.id,
                "userId" to story.userId,
                "username" to story.username,
                "userAvatarUrl" to story.userAvatarUrl,
                "mediaUrl" to story.mediaUrl,
                "isVideo" to story.isVideo,
                "timestamp" to story.timestamp,
                "isSeen" to story.isSeen,
                "caption" to story.caption,
                "audioTitle" to story.audioTitle,
                "audioArtist" to story.audioArtist,
                "audioUrl" to story.audioUrl,
                // Media Metadata
                "publicId" to story.publicId,
                "resourceType" to story.resourceType,
                "format" to story.format,
                "bytes" to story.bytes,
                "width" to story.width,
                "height" to story.height,
                "folder" to story.folder
            )

            db.collection("stories").document(story.id).set(storyMap).await()
            Log.d(TAG, "Successfully saved story to /stories/${story.id}")

            if (story.userId.isNotBlank()) {
                db.collection("users")
                    .document(story.userId)
                    .collection("stories")
                    .document(story.id)
                    .set(storyMap)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed publishing story to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteStoryFromFirestore(storyId: String, userId: String = "") = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext
            db.collection("stories").document(storyId).delete().await()
            if (userId.isNotBlank()) {
                db.collection("users").document(userId).collection("stories").document(storyId).delete().await()
            }
            Log.d(TAG, "Successfully deleted story from Firestore: $storyId")
        } catch (e: Exception) {
            Log.w(TAG, "Deleting story skipped/restricted: ${e.message}")
        }
    }

    /**
     * Publishes Reel to Firebase Firestore at:
     * 1. /reels/{reel.id}
     * 2. /users/{authorId}/reels/{reel.id}
     */
    suspend fun publishReel(reel: ReelVideo): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext false
            Log.d(TAG, "Publishing reel to Firestore: id=${reel.id}, videoUrl=${reel.videoUrl}")

            val reelMap = hashMapOf<String, Any>(
                "id" to reel.id,
                "authorId" to reel.authorId,
                "authorUsername" to reel.authorUsername,
                "authorDisplayName" to reel.authorDisplayName,
                "authorAvatarUrl" to reel.authorAvatarUrl,
                "videoUrl" to reel.videoUrl,
                "thumbnailUri" to reel.thumbnailUri,
                "caption" to reel.caption,
                "audioTitle" to reel.audioTitle,
                "audioArtist" to reel.audioArtist,
                "likesCount" to reel.likesCount,
                "isLiked" to reel.isLiked,
                "commentsCount" to reel.commentsCount,
                "sharesCount" to reel.sharesCount,
                "isSaved" to reel.isSaved,
                "tags" to reel.tags,
                "likedByUserIds" to reel.likedByUserIds,
                "commentedByUserIds" to reel.commentedByUserIds,
                "timestamp" to reel.timestamp,
                "viewsCount" to reel.viewsCount,
                // Media Metadata
                "publicId" to reel.publicId,
                "resourceType" to reel.resourceType,
                "format" to reel.format,
                "bytes" to reel.bytes,
                "width" to reel.width,
                "height" to reel.height,
                "folder" to reel.folder
            )

            db.collection("reels").document(reel.id).set(reelMap).await()
            Log.d(TAG, "Successfully saved reel to /reels/${reel.id}")

            if (reel.authorId.isNotBlank()) {
                db.collection("users")
                    .document(reel.authorId)
                    .collection("reels")
                    .document(reel.id)
                    .set(reelMap)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed publishing reel to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun sendChatMessage(message: ChatMessageEntity, currentUserId: String = ""): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: run {
            Log.e(TAG, "Firestore not initialized for sending chat message")
            return@withContext false
        }
        val canonicalId = getCanonicalThreadId(message.threadId, currentUserId.ifBlank { message.senderId })

        val messageMap = hashMapOf<String, Any>(
            "messageId" to message.messageId,
            "threadId" to message.threadId,
            "canonicalThreadId" to canonicalId,
            "senderId" to message.senderId,
            "senderUsername" to message.senderUsername,
            "text" to message.text,
            "mediaUrl" to message.mediaUrl,
            "isMedia" to message.isMedia,
            "isVoiceMessage" to message.isVoiceMessage,
            "voiceDurationSeconds" to message.voiceDurationSeconds,
            "timestamp" to message.timestamp,
            "reaction" to message.reaction
        )

        try {
            // Save in canonical chat thread collection
            db.collection("chats")
                .document(canonicalId)
                .collection("messages")
                .document(message.messageId)
                .set(messageMap)
                .await()

            // Also save in direct threadId document if different
            if (canonicalId != message.threadId) {
                db.collection("chats")
                    .document(message.threadId)
                    .collection("messages")
                    .document(message.messageId)
                    .set(messageMap)
                    .await()
            }

            // Update thread metadata in Firestore
            val threadMeta = hashMapOf<String, Any>(
                "threadId" to message.threadId,
                "canonicalThreadId" to canonicalId,
                "lastMessageText" to message.text.ifBlank { if (message.isVoiceMessage) "🎤 Voice message" else "📷 Media" },
                "lastMessageTimestamp" to message.timestamp,
                "lastSenderId" to message.senderId,
                "lastSenderUsername" to message.senderUsername
            )
            db.collection("chats").document(canonicalId).set(threadMeta, SetOptions.merge())

            Log.d(TAG, "Message ${message.messageId} sent successfully to $canonicalId")
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error sending chat message: ${e.message}")
            false
        }
    }

    suspend fun getRecommendedCreators(): List<UserData> = withContext(Dispatchers.IO) {
        val list = mutableListOf<UserData>()
        try {
            val db = firestore ?: return@withContext emptyList()
            val snapshot = db.collection("users").limit(10).get().await()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                list.add(UserData(
                    userId = doc.id,
                    username = (data["username"] as? String) ?: "user",
                    displayName = (data["displayName"] as? String) ?: "Creator",
                    bio = (data["bio"] as? String) ?: "",
                    avatarUrl = (data["avatarUrl"] as? String) ?: "",
                    followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                    isVerified = (data["isVerified"] as? Boolean) ?: false,
                    website = (data["website"] as? String) ?: "",
                    location = (data["location"] as? String) ?: ""
                ))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetching recommended creators skipped/restricted: ${e.message}")
        }
        list
    }

    suspend fun addCommentToFirestore(targetId: String, comment: CommentEntity) = withContext(Dispatchers.IO) {
        if (targetId.isBlank() || comment.id.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val commentMap = hashMapOf<String, Any>(
                "id" to comment.id,
                "postId" to comment.postId,
                "authorId" to comment.authorId,
                "authorUsername" to comment.authorUsername,
                "authorAvatarUrl" to comment.authorAvatarUrl,
                "text" to comment.text,
                "timestamp" to comment.timestamp
            )
            db.collection("posts").document(targetId).collection("comments").document(comment.id).set(commentMap).await()
            db.collection("posts").document(targetId).update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
            db.collection("reels").document(targetId).update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing comment to Firestore: ${e.message}")
        }
    }

    suspend fun isUsernameAvailable(username: String, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext true
            val snapshot = db.collection("users").get().await()
            val clean = username.trim().lowercase()
            val taken = snapshot.documents.any { doc ->
                doc.id != currentUserId && (doc.getString("username") ?: "").trim().lowercase() == clean
            }
            !taken
        } catch (e: Exception) {
            true
        }
    }

    suspend fun searchUsersByQuery(query: String): List<UserData> = withContext(Dispatchers.IO) {
        val list = mutableListOf<UserData>()
        try {
            val db = firestore ?: return@withContext emptyList()
            val snapshot = db.collection("users").get().await()
            val cleanQuery = query.trim().lowercase()
            if (cleanQuery.isBlank()) return@withContext emptyList()

            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val uId = doc.id.lowercase()
                val uname = (data["username"] as? String)?.lowercase() ?: ""
                val dname = (data["displayName"] as? String)?.lowercase() ?: ""
                val fName = (data["fullName"] as? String)?.lowercase() ?: ""

                if (uId.contains(cleanQuery) || 
                    uname.contains(cleanQuery) || 
                    dname.contains(cleanQuery) || 
                    fName.contains(cleanQuery)) {
                    
                    list.add(UserData(
                        userId = doc.id,
                        username = (data["username"] as? String) ?: "user",
                        displayName = (data["displayName"] as? String) ?: "Creator",
                        bio = (data["bio"] as? String) ?: "",
                        avatarUrl = (data["avatarUrl"] as? String) ?: "",
                        followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                        followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                        postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                        isVerified = (data["isVerified"] as? Boolean) ?: false,
                        website = (data["website"] as? String) ?: "",
                        location = (data["location"] as? String) ?: ""
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}")
        }
        list
    }

    suspend fun toggleFollowUserInFirestore(targetUserId: String, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank() || currentUserId.isBlank() || targetUserId == currentUserId) return@withContext false
        try {
            val db = firestore ?: return@withContext false
            val userRef = db.collection("users").document(targetUserId)
            val doc = userRef.get().await()
            if (doc.exists()) {
                val data = doc.data ?: return@withContext false
                val followers = (data["followersCount"] as? Number)?.toInt() ?: 0
                val followingList = doc.get("followedBy") as? List<String> ?: emptyList()
                
                val isFollowingNow = if (followingList.contains(currentUserId)) {
                    userRef.update(
                        "followersCount", (followers - 1).coerceAtLeast(0),
                        "followedBy", followingList - currentUserId
                    ).await()
                    false
                } else {
                    userRef.update(
                        "followersCount", followers + 1,
                        "followedBy", followingList + currentUserId
                    ).await()
                    true
                }
                
                val currentUserRef = db.collection("users").document(currentUserId)
                val currDoc = currentUserRef.get().await()
                if (currDoc.exists()) {
                    val currFollowing = (currDoc.data?.get("followingCount") as? Number)?.toInt() ?: 0
                    val currFollowingList = currDoc.get("followingList") as? List<String> ?: emptyList()
                    val newFollowingList = if (isFollowingNow) (currFollowingList + targetUserId).distinct() else currFollowingList - targetUserId
                    currentUserRef.update(
                        "followingCount", if (isFollowingNow) currFollowing + 1 else (currFollowing - 1).coerceAtLeast(0),
                        "followingList", newFollowingList
                    ).await()
                }
                
                return@withContext isFollowingNow
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling follow: ${e.message}")
        }
        false
    }

    suspend fun getFollowersUserIdsOf(targetUserId: String): List<String> = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank()) return@withContext emptyList()
        try {
            val db = firestore ?: return@withContext emptyList()
            val doc = db.collection("users").document(targetUserId).get().await()
            if (doc.exists()) {
                return@withContext (doc.get("followedBy") as? List<String>) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting followers: ${e.message}")
        }
        emptyList()
    }

    suspend fun getFollowingUserIdsOf(targetUserId: String): List<String> = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank()) return@withContext emptyList()
        try {
            val db = firestore ?: return@withContext emptyList()
            val doc = db.collection("users").document(targetUserId).get().await()
            if (doc.exists()) {
                return@withContext (doc.get("followingList") as? List<String>) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting following: ${e.message}")
        }
        emptyList()
    }

    suspend fun isUserFollowing(targetUserId: String, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank() || currentUserId.isBlank()) return@withContext false
        try {
            val db = firestore ?: return@withContext false
            val doc = db.collection("users").document(targetUserId).get().await()
            if (doc.exists()) {
                val followingList = doc.get("followedBy") as? List<String> ?: emptyList()
                return@withContext followingList.contains(currentUserId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking follow: ${e.message}")
        }
        false
    }

    suspend fun updatePostLikeStatusInFirestore(postId: String, isLiked: Boolean, likesCount: Int, userId: String = "") = withContext(Dispatchers.IO) {
        if (postId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val updates = mutableMapOf<String, Any>(
                "isLiked" to isLiked,
                "likesCount" to likesCount
            )
            if (userId.isNotBlank()) {
                if (isLiked) {
                    updates["likedByUserIds"] = FieldValue.arrayUnion(userId)
                } else {
                    updates["likedByUserIds"] = FieldValue.arrayRemove(userId)
                }
            }
            db.collection("posts").document(postId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating post like: ${e.message}")
        }
    }

    suspend fun updateReelLikeStatusInFirestore(reelId: String, isLiked: Boolean, likesCount: Int, userId: String = "") = withContext(Dispatchers.IO) {
        if (reelId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val updates = mutableMapOf<String, Any>(
                "isLiked" to isLiked,
                "likesCount" to likesCount
            )
            if (userId.isNotBlank()) {
                if (isLiked) {
                    updates["likedByUserIds"] = FieldValue.arrayUnion(userId)
                } else {
                    updates["likedByUserIds"] = FieldValue.arrayRemove(userId)
                }
            }
            db.collection("reels").document(reelId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reel like: ${e.message}")
        }
    }

    suspend fun editPostInFirestore(postId: String, newCaption: String, newLocation: String, newPrivacy: String = "PUBLIC", tags: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        if (postId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val updates = mutableMapOf<String, Any>(
                "caption" to newCaption,
                "location" to newLocation,
                "privacy" to newPrivacy,
                "tags" to tags
            )
            db.collection("posts").document(postId).update(updates).await()
            Log.d(TAG, "Post $postId updated in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error editing post in Firestore: ${e.message}")
        }
    }

    suspend fun deletePostFromFirestore(postId: String, authorId: String) = withContext(Dispatchers.IO) {
        if (postId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            db.collection("posts").document(postId).delete().await()
            if (authorId.isNotBlank()) {
                db.collection("users").document(authorId)
                    .collection("posts").document(postId).delete().await()
                db.collection("users").document(authorId)
                    .update("postsCount", FieldValue.increment(-1))
            }
            Log.d(TAG, "Post $postId deleted from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post in Firestore: ${e.message}")
        }
    }

    suspend fun editReelInFirestore(reelId: String, newCaption: String, newAudioTitle: String, newAudioArtist: String, newPrivacy: String = com.example.data.PrivacyLevel.PUBLIC.name, tags: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        if (reelId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val updates = mutableMapOf<String, Any>(
                "caption" to newCaption,
                "privacy" to newPrivacy,
                "tags" to tags
            )
            if (newAudioTitle.isNotBlank()) updates["audioTitle"] = newAudioTitle
            if (newAudioArtist.isNotBlank()) updates["audioArtist"] = newAudioArtist
            db.collection("reels").document(reelId).update(updates).await()
            Log.d(TAG, "Reel $reelId updated in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error editing reel in Firestore: ${e.message}")
        }
    }

    suspend fun recordReelView(reelId: String) = withContext(Dispatchers.IO) {
        if (reelId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            db.collection("reels").document(reelId).update("viewsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed incrementing reel view: ${e.message}")
        }
    }

    suspend fun deleteReelFromFirestore(reelId: String, authorId: String) = withContext(Dispatchers.IO) {
        if (reelId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            db.collection("reels").document(reelId).delete().await()
            if (authorId.isNotBlank()) {
                db.collection("users").document(authorId)
                    .collection("reels").document(reelId).delete().await()
            }
            Log.d(TAG, "Reel $reelId deleted from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reel in Firestore: ${e.message}")
        }
    }

    private var notificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    suspend fun sendNotificationToUser(recipientUserId: String, notification: com.example.data.NotificationEntity) = withContext(Dispatchers.IO) {
        if (recipientUserId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val notifMap = hashMapOf<String, Any>(
                "id" to notification.id,
                "recipientId" to recipientUserId,
                "type" to notification.type.name,
                "title" to notification.title,
                "message" to notification.message,
                "fromUserId" to notification.fromUserId,
                "fromUser" to notification.fromUser,
                "fromDisplayName" to notification.fromDisplayName,
                "fromUserAvatar" to notification.fromUserAvatar,
                "timestamp" to notification.timestamp,
                "isRead" to notification.isRead,
                "relatedId" to notification.relatedId,
                "relatedThumbnail" to notification.relatedThumbnail
            )
            db.collection("users")
                .document(recipientUserId)
                .collection("notifications")
                .document(notification.id)
                .set(notifMap)
                .await()
            Log.d(TAG, "Notification ${notification.id} sent to user $recipientUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification to user $recipientUserId: ${e.message}")
        }
    }

    fun startListeningToNotifications(userId: String, onNotificationsReceived: (List<com.example.data.NotificationEntity>) -> Unit) {
        if (userId.isBlank()) return
        val db = firestore ?: return
        notificationsListener?.remove()
        try {
            notificationsListener = db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to notifications failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            val typeStr = data["type"] as? String ?: "SYSTEM_ALERT"
                            val type = try {
                                com.example.data.NotificationType.valueOf(typeStr)
                            } catch (_: Exception) {
                                com.example.data.NotificationType.SYSTEM_ALERT
                            }
                            com.example.data.NotificationEntity(
                                id = doc.id,
                                recipientId = data["recipientId"] as? String ?: userId,
                                type = type,
                                title = data["title"] as? String ?: "",
                                message = data["message"] as? String ?: "",
                                fromUserId = data["fromUserId"] as? String ?: "",
                                fromUser = data["fromUser"] as? String ?: "",
                                fromDisplayName = data["fromDisplayName"] as? String ?: "",
                                fromUserAvatar = data["fromUserAvatar"] as? String ?: "",
                                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isRead = data["isRead"] as? Boolean ?: false,
                                relatedId = data["relatedId"] as? String ?: "",
                                relatedThumbnail = data["relatedThumbnail"] as? String ?: ""
                            )
                        }
                        onNotificationsReceived(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting notifications listener: ${e.message}")
        }
    }

    fun stopListeningToNotifications() {
        notificationsListener?.remove()
        notificationsListener = null
    }

    suspend fun clearUserNotifications(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val snap = db.collection("users").document(userId).collection("notifications").get().await()
            val batch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notifications: ${e.message}")
        }
    }

    suspend fun getAllUsers(): List<UserData> = withContext(Dispatchers.IO) {
        val list = mutableListOf<UserData>()
        try {
            val db = firestore ?: return@withContext emptyList()
            val snapshot = db.collection("users").get().await()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                list.add(UserData(
                    userId = doc.id,
                    username = (data["username"] as? String) ?: "",
                    displayName = (data["displayName"] as? String) ?: "User",
                    bio = (data["bio"] as? String) ?: "",
                    avatarUrl = (data["avatarUrl"] as? String) ?: "",
                    followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    isVerified = (data["isVerified"] as? Boolean) ?: false,
                    isOnline = (data["isOnline"] as? Boolean) ?: true,
                    showOnlineStatus = (data["showOnlineStatus"] as? Boolean) ?: true
                ))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error fetching all users: ${e.message}")
        }
        list
    }

    suspend fun initiateCall(call: CallEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext false
            val callMap = hashMapOf<String, Any>(
                "callId" to call.callId,
                "callerId" to call.callerId,
                "callerName" to call.callerName,
                "callerAvatar" to call.callerAvatar,
                "receiverId" to call.receiverId,
                "isVideo" to call.isVideo,
                "status" to CallStatus.RINGING.name,
                "timestamp" to call.timestamp
            )
            db.collection("calls").document(call.callId).set(callMap).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating call: ${e.message}")
            false
        }
    }

    suspend fun answerCall(callId: String) = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext
            db.collection("calls").document(callId).update("status", CallStatus.CONNECTED.name).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call: ${e.message}")
        }
    }

    suspend fun endCall(callId: String, status: CallStatus) = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext
            db.collection("calls").document(callId).update("status", status.name).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call: ${e.message}")
        }
    }

    private var callListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningToCall(callId: String, onStatusChanged: (CallStatus) -> Unit) {
        val db = firestore ?: return
        callListener = db.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to call failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val statusStr = snapshot.getString("status") ?: CallStatus.IDLE.name
                    val status = try {
                        CallStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        CallStatus.IDLE
                    }
                    onStatusChanged(status)
                }
            }
    }

    fun stopListeningToCall() {
        callListener?.remove()
        callListener = null
    }

    private var incomingCallsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningForIncomingCalls(currentUserId: String, onIncomingCall: (CallEntity?) -> Unit) {
        val db = firestore ?: return
        if (currentUserId.isBlank()) return
        incomingCallsListener?.remove()

        incomingCallsListener = db.collection("calls")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", CallStatus.RINGING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val firstDoc = snapshot.documents.firstOrNull()
                if (firstDoc != null) {
                    val data = firstDoc.data ?: return@addSnapshotListener
                    val call = CallEntity(
                        callId = firstDoc.id,
                        callerId = data["callerId"] as? String ?: "",
                        callerName = data["callerName"] as? String ?: "HundredGram User",
                        callerAvatar = data["callerAvatar"] as? String ?: "",
                        receiverId = data["receiverId"] as? String ?: currentUserId,
                        isVideo = data["isVideo"] as? Boolean ?: false,
                        status = CallStatus.RINGING,
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    onIncomingCall(call)
                } else {
                    onIncomingCall(null)
                }
            }
    }

    fun stopListeningForIncomingCalls() {
        incomingCallsListener?.remove()
        incomingCallsListener = null
    }

    // Real-time Chat Typing Status
    private val typingListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    fun setTypingStatus(threadId: String, currentUserId: String, isTyping: Boolean) {
        val db = firestore ?: return
        if (currentUserId.isBlank()) return
        val canonicalId = getCanonicalThreadId(threadId, currentUserId)
        try {
            db.collection("chats").document(canonicalId).set(
                mapOf(
                    "typingUsers" to mapOf(currentUserId to isTyping),
                    "lastTypingUpdate" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        } catch (_: Exception) {}
    }

    fun startListeningToTyping(threadId: String, currentUserId: String, onTypingChanged: (Boolean) -> Unit) {
        val db = firestore ?: return
        val canonicalId = getCanonicalThreadId(threadId, currentUserId)
        typingListeners[threadId]?.remove()

        val listener = db.collection("chats").document(canonicalId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    onTypingChanged(false)
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                val typingUsers = snapshot.get("typingUsers") as? Map<String, Any> ?: emptyMap()
                val isOtherUserTyping = typingUsers.entries.any { (uid, typing) ->
                    uid != currentUserId && (typing == true || typing.toString().toBooleanStrictOrNull() == true)
                }
                onTypingChanged(isOtherUserTyping)
            }
        typingListeners[threadId] = listener
    }

    fun stopListeningToTyping(threadId: String) {
        typingListeners[threadId]?.remove()
        typingListeners.remove(threadId)
    }
}
