package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.data.audio.ReelAudioService
import com.example.data.audio.YouTubeSongSearchService
import com.example.data.auth.DeviceAccountsHelper
import com.example.data.auth.FacebookAuthManager
import com.example.data.auth.FirebasePhoneAuthManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.firestore.FirestoreRepository
import com.example.data.location.AppLocationManager
import com.example.data.network.NetworkConnectivityObserver
import com.example.data.network.OnlineSyncService
import com.example.data.network.SmsOtpService
import com.example.data.storage.MediaStorageManager
import com.example.data.storage.MediaStorageHelper
import com.example.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SocialRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val googleAuthManager = GoogleAuthManager(context)
    val facebookAuthManager = FacebookAuthManager(context)
    val phoneAuthManager = FirebasePhoneAuthManager(context)
    val deviceAccountsHelper = DeviceAccountsHelper(context)
    val smsOtpService = SmsOtpService(context)
    val onlineSyncService = OnlineSyncService()
    val firestoreRepository = FirestoreRepository(context)
    val locationManager = AppLocationManager(context)
    val mediaStorageHelper = MediaStorageHelper(context)
    val storageManager = MediaStorageManager(context)
    val reelAudioService = ReelAudioService(context)
    val voiceMessageManager = com.example.data.audio.VoiceMessageManager(context)
    val youtubeSearchService = YouTubeSongSearchService(context)
    val nearbyService = com.example.data.nearby.NearbyService(context)
    val connectivityObserver = NetworkConnectivityObserver(context)
    val draftRepository = DraftRepository(context)
    private val socialDao = AppDatabase.getDatabase(context).socialDao()

    private val _isLoggedIn = MutableStateFlow(checkInitialLoginState())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(createInitialUserData())
    val currentUser: StateFlow<UserData> = _currentUser.asStateFlow()

    private val _posts = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<StoryEntity>>(emptyList())
    val stories: StateFlow<List<StoryEntity>> = _stories.asStateFlow()

    private val _reels = MutableStateFlow<List<ReelVideo>>(emptyList())
    val reels: StateFlow<List<ReelVideo>> = _reels.asStateFlow()

    private val _comments = MutableStateFlow<Map<String, List<CommentEntity>>>(emptyMap())
    val comments: StateFlow<Map<String, List<CommentEntity>>> = _comments.asStateFlow()

    private val _chatThreads = MutableStateFlow<List<ChatThreadEntity>>(emptyList())
    val chatThreads: StateFlow<List<ChatThreadEntity>> = _chatThreads.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessageEntity>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessageEntity>>> = _chatMessages.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLogItem>>(emptyList())
    val callLogs: StateFlow<List<CallLogItem>> = _callLogs.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _blockedList = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedList: StateFlow<List<BlockedUser>> = _blockedList.asStateFlow()

    private val _reportedList = MutableStateFlow<List<ReportedItem>>(emptyList())
    val reportedList: StateFlow<List<ReportedItem>> = _reportedList.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _typingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStatus: StateFlow<Map<String, Boolean>> = _typingStatus.asStateFlow()

    private val _incomingCall = MutableStateFlow<CallEntity?>(null)
    val incomingCall: StateFlow<CallEntity?> = _incomingCall.asStateFlow()

    private val _followedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followedUserIds: StateFlow<Set<String>> = _followedUserIds.asStateFlow()

    init {
        // Purge any legacy sample data from Room DB
        scope.launch {
            try {
                socialDao.deleteAllSamplePosts()
                socialDao.deleteAllSampleReels()
                socialDao.deleteAllSampleStories()
            } catch (_: Exception) {}
        }

        // 1. Observe and stream cached Posts from Room DB for instant offline loading
        scope.launch {
            try {
                socialDao.getAllPostsFlow().collect { cachedPosts ->
                    val realPosts = cachedPosts.filterNot { it.id.startsWith("sample_") || it.authorId.startsWith("u_") }
                    _posts.value = realPosts
                }
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error observing cached posts: ${e.message}")
            }
        }

        // 2. Observe and stream cached Reels from Room DB for instant offline loading
        scope.launch {
            try {
                socialDao.getAllReelsFlow().collect { cachedReels ->
                    val realReels = cachedReels.filterNot { it.id.startsWith("sample_") || it.authorId.startsWith("u_") }
                    _reels.value = realReels
                }
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error observing cached reels: ${e.message}")
            }
        }

        // Sync online posts, reels, and stories from live Firestore backend
        scope.launch {
            try {
                syncOnlineData()
                performAutoCleanupOfExpiredReelsAndPosts()
            } catch (e: Exception) {
                // Ignore
            }

            val current = _currentUser.value
            if (_isLoggedIn.value && current.userId.isNotBlank()) {
                enableFcmDynamically()
                val savedProfile = firestoreRepository.getUserFromFirestore(current.userId)
                if (savedProfile != null) {
                    _currentUser.value = savedProfile
                } else {
                    firestoreRepository.syncUserToFirestore(current)
                }
                startListeningToNotificationsForUser(current.userId)
            }
        }
    }

    private fun enableFcmDynamically() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Log.d("SocialRepository", "FCM successfully enabled dynamically for logged-in user.")
        } catch (e: Exception) {
            Log.e("SocialRepository", "FCM Dynamic Activation failed safely (usually due to TOO_MANY_REGISTRATIONS or missing play services): ${e.message}")
        }
    }

    private val seenNotificationIds = mutableSetOf<String>()
    private var initialNotificationsLoaded = false

    fun startListeningToNotificationsForUser(userId: String) {
        if (userId.isBlank()) return
        seenNotificationIds.clear()
        initialNotificationsLoaded = false
        NotificationHelper.initNotificationChannels(context)

        firestoreRepository.startListeningToNotifications(userId) { list ->
            _notifications.value = list

            if (!initialNotificationsLoaded) {
                // First snapshot on login / launch: record existing IDs without buzzing
                list.forEach { seenNotificationIds.add(it.id) }
                initialNotificationsLoaded = true
            } else {
                // Subsequent real-time snapshot: trigger system push notification with alert sound & vibration
                list.forEach { notif ->
                    if (!seenNotificationIds.contains(notif.id) && !notif.isRead && notif.fromUserId != userId) {
                        seenNotificationIds.add(notif.id)
                        NotificationHelper.showPushNotification(
                            context = context,
                            title = notif.title.ifBlank { "HundredGram Alert" },
                            message = if (notif.fromDisplayName.isNotBlank()) "${notif.fromDisplayName}: ${notif.message}" else notif.message,
                            type = notif.type,
                            relatedId = notif.relatedId,
                            fromUsername = notif.fromUser
                        )
                    }
                }
            }
        }
    }

    fun startListeningForIncomingCalls(userId: String) {
        if (userId.isBlank()) return
        firestoreRepository.startListeningForIncomingCalls(userId) { call ->
            val prev = _incomingCall.value
            _incomingCall.value = call
            if (call != null && (prev == null || prev.callId != call.callId)) {
                NotificationHelper.showPushNotification(
                    context = context,
                    title = "📞 Incoming ${if (call.isVideo) "Video" else "Voice"} Call",
                    message = "${call.callerName} is calling you...",
                    type = NotificationType.SYSTEM_ALERT,
                    relatedId = call.callId,
                    fromUsername = call.callerName
                )
            }
        }
    }

    fun stopListeningForIncomingCalls() {
        firestoreRepository.stopListeningForIncomingCalls()
        _incomingCall.value = null
    }

    private fun checkInitialLoginState(): Boolean {
        return try {
            val fbUser = FirebaseAuth.getInstance().currentUser
            fbUser != null && !fbUser.isAnonymous
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(user: UserData) {
        _currentUser.value = user
        _isLoggedIn.value = true
        enableFcmDynamically()
        scope.launch {
            firestoreRepository.syncUserToFirestore(user)
        }
        startListeningToNotificationsForUser(user.userId)
        startListeningForIncomingCalls(user.userId)
    }

    fun logoutUser() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        firestoreRepository.stopListeningToNotifications()
        stopListeningForIncomingCalls()
        _notifications.value = emptyList()
        _currentUser.value = createGuestUserData()
        _isLoggedIn.value = false
    }

    /**
     * Requests account deactivation. Marks user as deactivated with deactivation timestamp,
     * posts system notification, triggers email intent chooser, and logs out user.
     */
    fun deactivateAccount(context: Context, reason: String = ""): String {
        val user = _currentUser.value
        if (user.userId.isBlank()) return "कोई सक्रिय खाता नहीं मिला (No active account found)"

        val deactivationTime = System.currentTimeMillis()
        val deactivatedUser = user.copy(
            isDeactivated = true,
            deactivationTimestamp = deactivationTime
        )

        _currentUser.value = deactivatedUser
        scope.launch {
            firestoreRepository.syncUserToFirestore(deactivatedUser)
            try {
                socialDao.insertUser(deactivatedUser)
            } catch (_: Exception) {}
        }

        val fbEmail = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email } catch (_: Exception) { null }
        val emailToSend = user.email.ifBlank { fbEmail.orEmpty() }.ifBlank { "ssir6921@gmail.com" }
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(deactivationTime))
        val expiryTime = deactivationTime + (15L * 24 * 60 * 60 * 1000)
        val expiryStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expiryTime))

        com.example.util.NotificationHelper.showPushNotification(
            context = context,
            title = "⚠️ Account Deactivated (खाता निष्क्रिय)",
            message = "खाता $dateStr पर निष्क्रिय किया गया। 15 दिनों ($expiryStr) में लॉगिन न करने पर डिलीट हो जाएगा। सूचना $emailToSend पर भेजी गई।",
            type = NotificationType.SYSTEM_ALERT
        )

        // Launch email chooser to send deactivation notice to user's email
        try {
            val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(emailToSend))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "HundredGram - Account Deactivation & Deletion Notice")
                putExtra(android.content.Intent.EXTRA_TEXT, """
                    नमस्ते ${user.displayName.ifBlank { user.username }},

                    आपका HundredGram खाता (@${user.username}) निष्क्रय (Deactivate) कर दिया गया है।

                    --------------------------------------------------
                    महत्वपूर्ण जानकारी (15-Day Grace Period)
                    --------------------------------------------------
                    • निष्क्रय तिथि: $dateStr
                    • अंतिम तिथि (Expiry Date): $expiryStr (15 दिन)

                    1. यदि आप 15 दिनों के भीतर ($expiryStr तक) पुनः लॉगिन करते हैं, तो आपका खाता स्वतः पुनः सक्रिय (Reactivate) हो जाएगा और कोई डेटा डिलीट नहीं होगा।
                    2. यदि आप 15 दिनों तक लॉगिन नहीं करते हैं, तो 15 दिन पूरे होने पर आपका खाता एवं सभी डेटा स्थायी रूप से डिलीट कर दिए जाएंगे।

                    सपोर्ट ईमेल: ssir6921@gmail.com
                    टीम HundredGram
                """.trimIndent())
            }
            emailIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Deactivation Email Confirmation"))
        } catch (_: Exception) {}

        logoutUser()
        return "खाता सफलतापूर्वक निष्क्रिय कर दिया गया है। 15 दिनों के भीतर लॉगिन न करने पर खाता स्थायी रूप से डिलीट कर दिया जाएगा।"
    }

    /**
     * Checks if a logging in user was previously deactivated.
     * If < 15 days, reactivates account.
     * If >= 15 days, permanently deletes user data.
     */
    suspend fun checkAndProcessAccountDeactivationOnLogin(context: Context, user: UserData): Pair<Boolean, String> {
        if (!user.isDeactivated || user.deactivationTimestamp <= 0L) {
            return Pair(false, "OK")
        }

        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - user.deactivationTimestamp
        val daysElapsed = diffMillis / (1000L * 60 * 60 * 24)

        return if (daysElapsed >= 15) {
            try {
                socialDao.deleteUser(user.userId)
            } catch (_: Exception) {}
            try {
                firestoreRepository.deleteUserFromFirestore(user.userId)
            } catch (_: Exception) {}

            Pair(
                true,
                "15 दिन समाप्त हो चुके हैं। आपका खाता स्थायी रूप से डिलीट कर दिया गया है। नया खाता बनाने के लिए साइन अप करें।"
            )
        } else {
            val reactivatedUser = user.copy(
                isDeactivated = false,
                deactivationTimestamp = 0L
            )
            _currentUser.value = reactivatedUser
            try {
                socialDao.insertUser(reactivatedUser)
                firestoreRepository.syncUserToFirestore(reactivatedUser)
            } catch (_: Exception) {}

            com.example.util.NotificationHelper.showPushNotification(
                context = context,
                title = "🎉 Account Reactivated (खाता पुनः सक्रिय)",
                message = "स्वागत है! 15 दिनों के भीतर लॉगिन करने पर आपकी खाता निष्क्रियता रद्द कर दी गई है।",
                type = NotificationType.SYSTEM_ALERT
            )

            Pair(
                false,
                "स्वागत है! 15 दिनों के भीतर लॉगिन करने पर आपका खाता पुनः सक्रिय (Reactivated) कर दिया गया है। खाता डिलीट नहीं होगा।"
            )
        }
    }

    fun setCurrentUser(user: UserData) {
        loginUser(user)
    }

    private fun createGuestUserData(): UserData {
        return UserData(
            userId = "",
            username = "guest",
            displayName = "Guest User",
            bio = "Please log in to manage your account",
            avatarUrl = "",
            followersCount = 0,
            followingCount = 0,
            postsCount = 0,
            isVerified = false,
            website = "",
            location = ""
        )
    }

    private fun createInitialUserData(): UserData {
        val fbUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }

        return if (fbUser != null && !fbUser.isAnonymous) {
            UserData(
                userId = fbUser.uid,
                username = fbUser.email?.substringBefore("@") ?: "user_${fbUser.uid.take(6)}",
                displayName = fbUser.displayName ?: "HundredGram Member",
                bio = "HundredGram Creator ✨",
                avatarUrl = fbUser.photoUrl?.toString() ?: "",
                followersCount = 0,
                followingCount = 0,
                postsCount = 0,
                isVerified = false,
                website = "",
                location = ""
            )
        } else {
            createGuestUserData()
        }
    }

    fun addCallLog(
        callerName: String,
        callerAvatar: String,
        isVideo: Boolean,
        durationSeconds: Int,
        isOutgoing: Boolean = true,
        isMissed: Boolean = false
    ) {
        val direction = when {
            isMissed || durationSeconds == 0 -> CallDirection.MISSED
            isOutgoing -> CallDirection.OUTGOING
            else -> CallDirection.INCOMING
        }
        val newCall = CallLogItem(
            id = "cl_${UUID.randomUUID()}",
            callerId = "caller_${callerName.lowercase().replace(" ", "_")}",
            callerName = callerName,
            callerAvatar = callerAvatar,
            type = if (isVideo) CallType.VIDEO else CallType.VOICE,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        _callLogs.value = listOf(newCall) + _callLogs.value
    }

    fun clearCallLogs() {
        _callLogs.value = emptyList()
    }

    fun toggleLikePost(postId: String) {
        val currentUserId = _currentUser.value.userId.ifBlank { "usr_me" }
        val updated = _posts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                val newLikedUsers = if (newLiked) {
                    if (post.likedByUserIds.contains(currentUserId)) post.likedByUserIds else post.likedByUserIds + currentUserId
                } else {
                    post.likedByUserIds.filterNot { it == currentUserId }
                }
                scope.launch {
                    firestoreRepository.updatePostLikeStatusInFirestore(postId, newLiked, newCount, currentUserId)
                }
                post.copy(isLiked = newLiked, likesCount = newCount, likedByUserIds = newLikedUsers)
            } else post
        }
        _posts.value = updated
    }

    fun toggleBookmarkPost(postId: String) {
        val updated = _posts.value.map { post ->
            if (post.id == postId) {
                val newSaved = !post.isSaved
                post.copy(isSaved = newSaved)
            } else post
        }
        _posts.value = updated
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val author = _currentUser.value
        val newComment = CommentEntity(
            id = "c_${UUID.randomUUID()}",
            postId = postId,
            authorId = author.userId,
            authorUsername = author.username,
            authorAvatarUrl = author.avatarUrl,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        val currentComments = _comments.value[postId]?.toMutableList() ?: mutableListOf()
        currentComments.add(newComment)
        _comments.value = _comments.value + (postId to currentComments)

        // Increment comments count on post and reel, and add commented user ID
        _posts.value = _posts.value.map {
            if (it.id == postId) {
                val newCommented = if (it.commentedByUserIds.contains(author.userId)) it.commentedByUserIds else it.commentedByUserIds + author.userId
                it.copy(commentsCount = it.commentsCount + 1, commentedByUserIds = newCommented)
            } else it
        }
        _reels.value = _reels.value.map {
            if (it.id == postId) {
                val newCommented = if (it.commentedByUserIds.contains(author.userId)) it.commentedByUserIds else it.commentedByUserIds + author.userId
                it.copy(commentsCount = it.commentsCount + 1, commentedByUserIds = newCommented)
            } else it
        }

        scope.launch {
            firestoreRepository.addCommentToFirestore(postId, newComment)
        }
    }

    fun toggleLikeReel(reelId: String) {
        val currentUserId = _currentUser.value.userId.ifBlank { "usr_me" }
        val updated = _reels.value.map { reel ->
            if (reel.id == reelId) {
                val newLiked = !reel.isLiked
                val newCount = if (newLiked) reel.likesCount + 1 else (reel.likesCount - 1).coerceAtLeast(0)
                val newLikedUsers = if (newLiked) {
                    if (reel.likedByUserIds.contains(currentUserId)) reel.likedByUserIds else reel.likedByUserIds + currentUserId
                } else {
                    reel.likedByUserIds.filterNot { it == currentUserId }
                }
                scope.launch {
                    firestoreRepository.updateReelLikeStatusInFirestore(reelId, newLiked, newCount, currentUserId)
                }
                reel.copy(isLiked = newLiked, likesCount = newCount, likedByUserIds = newLikedUsers)
            } else reel
        }
        _reels.value = updated
    }

    fun toggleBookmarkReel(reelId: String) {
        val updated = _reels.value.map { reel ->
            if (reel.id == reelId) reel.copy(isSaved = !reel.isSaved) else reel
        }
        _reels.value = updated
    }

    suspend fun toggleFollowUser(targetUserId: String): Boolean {
        val currentUserId = _currentUser.value.userId.ifBlank { "usr_me" }
        if (targetUserId.isBlank() || targetUserId == currentUserId) return false

        // 1. Optimistic UI update so follow button instantly turns to "Following"
        val currentSet = _followedUserIds.value.toMutableSet()
        val willFollow = !currentSet.contains(targetUserId)
        if (willFollow) {
            currentSet.add(targetUserId)
        } else {
            currentSet.remove(targetUserId)
        }
        _followedUserIds.value = currentSet

        val localUser = _currentUser.value
        val newFollowingCount = if (willFollow) localUser.followingCount + 1 else (localUser.followingCount - 1).coerceAtLeast(0)
        _currentUser.value = localUser.copy(
            userId = if (localUser.userId.isBlank()) currentUserId else localUser.userId,
            followingCount = newFollowingCount
        )

        // 2. Sync to Firestore in background
        try {
            val isFollowingNow = firestoreRepository.toggleFollowUserInFirestore(targetUserId, currentUserId)
            if (isFollowingNow || willFollow) {
                return true
            }
        } catch (_: Exception) {}
        return willFollow
    }

    suspend fun isFollowingUser(targetUserId: String): Boolean {
        if (_followedUserIds.value.contains(targetUserId)) return true
        val currentUserId = _currentUser.value.userId
        if (currentUserId.isBlank() || targetUserId.isBlank()) return false
        return firestoreRepository.isUserFollowing(targetUserId, currentUserId)
    }

    suspend fun updateFollowedUserIds() {
        val currentUserId = _currentUser.value.userId
        if (currentUserId.isBlank()) return
        try {
            val allUsers = getAllUsers()
            val followed = allUsers.filter { user ->
                user.userId != currentUserId && isUserFollowing(user.userId, currentUserId)
            }.map { it.userId }.toSet()
            _followedUserIds.value = followed
        } catch (_: Exception) {}
    }

    suspend fun getAllUsers(): List<UserData> {
        val remote = firestoreRepository.getAllUsers()
        return remote
    }

    suspend fun getFollowersOrFollowing(targetUserId: String, isFollowingTab: Boolean): List<UserData> {
        val allUsers = getAllUsers()
        val currentUserId = _currentUser.value.userId.ifBlank { "usr_me" }
        val myFollowedUserIds = _followedUserIds.value

        val isSelf = targetUserId.isBlank() || targetUserId == currentUserId

        if (isFollowingTab) {
            // FOLLOWING TAB
            if (isSelf) {
                return allUsers.filter { user -> myFollowedUserIds.contains(user.userId) && user.userId != currentUserId }
            } else {
                val remoteFollowingIds = firestoreRepository.getFollowingUserIdsOf(targetUserId).toSet()
                return allUsers.filter { user -> remoteFollowingIds.contains(user.userId) && user.userId != targetUserId }
            }
        } else {
            // FOLLOWERS TAB
            if (isSelf) {
                val remoteFollowerIds = firestoreRepository.getFollowersUserIdsOf(currentUserId).toSet()
                return allUsers.filter { user -> remoteFollowerIds.contains(user.userId) && user.userId != currentUserId }
            } else {
                val remoteFollowerIds = firestoreRepository.getFollowersUserIdsOf(targetUserId).toSet()
                var targetFollowerIds = remoteFollowerIds

                if (myFollowedUserIds.contains(targetUserId)) {
                    targetFollowerIds = targetFollowerIds + currentUserId
                } else {
                    targetFollowerIds = targetFollowerIds - currentUserId
                }
                return allUsers.filter { user -> targetFollowerIds.contains(user.userId) && user.userId != targetUserId }
            }
        }
    }

    suspend fun isUserFollowing(targetUserId: String, currentUserId: String): Boolean {
        return firestoreRepository.isUserFollowing(targetUserId, currentUserId)
    }

    suspend fun publishPostAfterUpload(
        mediaUrl: String,
        caption: String,
        location: String = "",
        type: String = "image",
        text: String = "",
        publicId: String = "",
        resourceType: String = "",
        format: String = "",
        bytes: Long = 0L,
        width: Int = 0,
        height: Int = 0,
        folder: String = "",
        privacy: PrivacyLevel = PrivacyLevel.PUBLIC,
        tags: List<String> = emptyList()
    ) {
        val author = _currentUser.value
        val finalLocation = if (location.isNotBlank()) location else locationManager.getCurrentLocationName()

        val newPost = PostEntity(
            id = "p_${UUID.randomUUID()}",
            authorId = author.userId,
            authorUsername = author.username,
            authorDisplayName = author.displayName.ifBlank { author.username },
            authorAvatarUrl = author.avatarUrl,
            mediaUrl = mediaUrl,
            imageUrl = mediaUrl,
            type = type,
            text = text,
            caption = caption,
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            timestamp = System.currentTimeMillis(),
            location = if (finalLocation != "Location unavailable") finalLocation else "",
            privacy = privacy.name,
            tags = tags,
            // Media Metadata Fields
            publicId = publicId,
            resourceType = resourceType,
            format = format,
            bytes = bytes,
            width = width,
            height = height,
            folder = folder
        )
        _posts.value = listOf(newPost) + _posts.value
        _currentUser.value = author.copy(postsCount = author.postsCount + 1)
        firestoreRepository.publishPost(newPost)
        socialDao.insertPost(newPost)
    }

    suspend fun publishStoryAfterUpload(
        mediaUrl: String,
        caption: String = "",
        audioTitle: String = "",
        audioArtist: String = "",
        audioUrl: String = "",
        publicId: String = "",
        resourceType: String = "",
        format: String = "",
        bytes: Long = 0L,
        width: Int = 0,
        height: Int = 0,
        folder: String = ""
    ) {
        val author = _currentUser.value
        val newStory = StoryEntity(
            id = "s_${UUID.randomUUID()}",
            userId = author.userId,
            username = author.username,
            userAvatarUrl = author.avatarUrl,
            mediaUrl = mediaUrl,
            caption = caption,
            audioTitle = audioTitle,
            audioArtist = audioArtist,
            audioUrl = audioUrl,
            timestamp = System.currentTimeMillis(),
            // Media Metadata Fields
            publicId = publicId,
            resourceType = resourceType,
            format = format,
            bytes = bytes,
            width = width,
            height = height,
            folder = folder
        )
        _stories.value = listOf(newStory) + _stories.value
        firestoreRepository.publishStory(newStory)
    }

    suspend fun deleteStory(storyId: String) {
        val storyToDelete = _stories.value.find { it.id == storyId }
        _stories.value = _stories.value.filterNot { it.id == storyId }
        try {
            firestoreRepository.deleteStoryFromFirestore(storyId, storyToDelete?.userId ?: "")
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error deleting story: ${e.message}")
        }
    }

    suspend fun publishReelAfterUpload(
        mediaUrl: String,
        caption: String,
        audioTitle: String,
        audioArtist: String,
        thumbnailUri: String = "",
        publicId: String = "",
        resourceType: String = "",
        format: String = "",
        bytes: Long = 0L,
        width: Int = 0,
        height: Int = 0,
        folder: String = "",
        privacy: PrivacyLevel = PrivacyLevel.PUBLIC,
        tags: List<String> = emptyList()
    ) {
        val author = _currentUser.value
        val finalThumb = if (thumbnailUri.isNotBlank()) thumbnailUri else mediaUrl
        val newReel = ReelVideo(
            id = "r_${UUID.randomUUID()}",
            authorId = author.userId,
            authorUsername = author.username,
            authorDisplayName = author.displayName.ifBlank { author.username },
            authorAvatarUrl = author.avatarUrl,
            videoUrl = mediaUrl,
            thumbnailUri = finalThumb,
            caption = caption,
            audioTitle = audioTitle.ifBlank { "Original Audio" },
            audioArtist = audioArtist.ifBlank { author.displayName },
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            timestamp = System.currentTimeMillis(),
            tags = tags,
            // Media Metadata Fields
            publicId = publicId,
            resourceType = resourceType,
            format = format,
            bytes = bytes,
            width = width,
            height = height,
            folder = folder,
            privacy = privacy.name
        )
        _reels.value = listOf(newReel) + _reels.value
        firestoreRepository.publishReel(newReel)
        socialDao.insertReel(newReel)
    }

    fun editPost(postId: String, newCaption: String, newLocation: String, newPrivacy: PrivacyLevel = PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        var editedPost: PostEntity? = null
        val updated = _posts.value.map { post ->
            if (post.id == postId) {
                val updatedPost = post.copy(
                    caption = newCaption,
                    location = newLocation.ifBlank { post.location },
                    privacy = newPrivacy.name,
                    tags = if (tags.isNotEmpty() || post.tags.isNotEmpty()) tags else post.tags
                )
                editedPost = updatedPost
                updatedPost
            } else post
        }
        _posts.value = updated
        scope.launch {
            editedPost?.let {
                firestoreRepository.editPostInFirestore(postId, newCaption, newLocation, newPrivacy.name, it.tags)
                socialDao.insertPost(it)
            }
        }
    }

    fun deletePost(postId: String) {
        val targetPost = _posts.value.find { it.id == postId }
        _posts.value = _posts.value.filterNot { it.id == postId }
        val author = _currentUser.value
        _currentUser.value = author.copy(postsCount = (author.postsCount - 1).coerceAtLeast(0))
        scope.launch {
            try {
                firestoreRepository.deletePostFromFirestore(postId, targetPost?.authorId ?: author.userId)
                socialDao.deletePost(postId)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Failed to delete post: ${e.message}", e)
            }
        }
    }

    fun editReel(reelId: String, newCaption: String, newAudioTitle: String = "", newAudioArtist: String = "", privacy: PrivacyLevel = PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        var editedReel: ReelVideo? = null
        val updated = _reels.value.map { reel ->
            if (reel.id == reelId) {
                val updatedR = reel.copy(
                    caption = newCaption,
                    audioTitle = if (newAudioTitle.isNotBlank()) newAudioTitle else reel.audioTitle,
                    audioArtist = if (newAudioArtist.isNotBlank()) newAudioArtist else reel.audioArtist,
                    privacy = privacy.name,
                    tags = if (tags.isNotEmpty() || reel.tags.isNotEmpty()) tags else reel.tags
                )
                editedReel = updatedR
                updatedR
            } else reel
        }
        _reels.value = updated
        scope.launch {
            editedReel?.let {
                firestoreRepository.editReelInFirestore(reelId, newCaption, newAudioTitle, newAudioArtist, privacy.name, it.tags)
                try { socialDao.insertReel(it) } catch (_: Exception) {}
            }
        }
    }

    private val viewedReelIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun recordReelView(reelId: String) {
        if (reelId.isBlank()) return
        val currentUser = _currentUser.value
        val targetReel = _reels.value.find { it.id == reelId }

        // 1. Do NOT count view if creator views their own reel
        if (targetReel != null) {
            val isAuthor = targetReel.authorId == currentUser.userId ||
                           (targetReel.authorUsername.isNotBlank() && targetReel.authorUsername.equals(currentUser.username, ignoreCase = true))
            if (isAuthor) return
        }

        // 2. Count view only ONCE per device/session
        if (viewedReelIds.contains(reelId)) return
        viewedReelIds.add(reelId)

        _reels.value = _reels.value.map { r ->
            if (r.id == reelId) r.copy(viewsCount = r.viewsCount + 1) else r
        }
        scope.launch {
            try {
                firestoreRepository.recordReelView(reelId)
                val current = _reels.value.find { it.id == reelId }
                if (current != null) {
                    socialDao.insertReel(current)
                }
            } catch (e: Exception) {
                Log.w("SocialRepository", "Error updating reel view: ${e.message}")
            }
        }
    }

    fun deleteReel(reelId: String) {
        val targetReel = _reels.value.find { it.id == reelId }
        _reels.value = _reels.value.filterNot { it.id == reelId }
        scope.launch {
            try {
                firestoreRepository.deleteReelFromFirestore(reelId, targetReel?.authorId ?: _currentUser.value.userId)
                socialDao.deleteReel(reelId)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Failed to delete reel: ${e.message}", e)
            }
        }
    }

    fun sendNotification(
        recipientUserId: String,
        type: NotificationType,
        title: String,
        message: String,
        relatedId: String = "",
        relatedThumbnail: String = ""
    ) {
        val author = _currentUser.value
        // CRITICAL CHECK: NEVER send notification to oneself!
        if (recipientUserId.isBlank() || recipientUserId == author.userId) {
            return
        }

        val notif = NotificationEntity(
            id = "notif_${UUID.randomUUID()}",
            recipientId = recipientUserId,
            type = type,
            title = title,
            message = message,
            fromUserId = author.userId,
            fromUser = author.username,
            fromDisplayName = author.displayName.ifBlank { author.username },
            fromUserAvatar = author.avatarUrl,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = relatedId,
            relatedThumbnail = relatedThumbnail
        )

        scope.launch {
            firestoreRepository.sendNotificationToUser(recipientUserId, notif)
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
        val currentUserId = _currentUser.value.userId
        if (currentUserId.isNotBlank()) {
            scope.launch {
                firestoreRepository.clearUserNotifications(currentUserId)
            }
        }
    }

    fun startListeningToChatThread(threadId: String) {
        val currentUserId = _currentUser.value.userId
        firestoreRepository.startListeningToTyping(threadId, currentUserId) { isTyping ->
            _typingStatus.value = _typingStatus.value + (threadId to isTyping)
        }

        firestoreRepository.startListeningToThread(threadId, currentUserId) { firestoreMessages ->
            if (firestoreMessages.isNotEmpty()) {
                val currentLocalMessages = _chatMessages.value[threadId] ?: emptyList()
                val firestoreIds = firestoreMessages.map { it.messageId }.toSet()

                // Keep local sending/failed messages if not yet synced to firestore
                val pendingLocal = currentLocalMessages.filter { it.messageId !in firestoreIds && (it.sendStatus == "SENDING" || it.sendStatus == "FAILED") }
                val merged = (firestoreMessages + pendingLocal).sortedBy { it.timestamp }

                _chatMessages.value = _chatMessages.value + (threadId to merged)

                val lastMsg = merged.lastOrNull()
                if (lastMsg != null) {
                    val existingIndex = _chatThreads.value.indexOfFirst { it.threadId == threadId }
                    if (existingIndex >= 0) {
                        _chatThreads.value = _chatThreads.value.map {
                            if (it.threadId == threadId) {
                                it.copy(
                                    lastMessageText = lastMsg.text.ifBlank { if (lastMsg.isVoiceMessage) "🎤 Voice message" else "📷 Media" },
                                    lastMessageTimestamp = lastMsg.timestamp
                                )
                            } else it
                        }
                    }
                }
            }
        }
    }

    fun stopListeningToChatThread(threadId: String) {
        firestoreRepository.stopListeningToThread(threadId)
        firestoreRepository.stopListeningToTyping(threadId)
        _typingStatus.value = _typingStatus.value - threadId
    }

    fun setTyping(threadId: String, isTyping: Boolean) {
        val currentUserId = _currentUser.value.userId
        firestoreRepository.setTypingStatus(threadId, currentUserId, isTyping)
    }

    fun sendChatMessage(threadId: String, text: String, recipientUsername: String = "", recipientAvatar: String = "", mediaUrl: String = "", isMessageRequest: Boolean = false) {
        if (text.isBlank() && mediaUrl.isBlank()) return
        val author = _currentUser.value
        setTyping(threadId, false)

        val message = ChatMessageEntity(
            messageId = "m_${UUID.randomUUID()}",
            threadId = threadId,
            senderId = author.userId,
            senderUsername = author.username,
            text = text,
            mediaUrl = mediaUrl,
            isMedia = mediaUrl.isNotBlank(),
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            isRead = true,
            sendStatus = "SENDING"
        )

        val currentList = _chatMessages.value[threadId]?.toMutableList() ?: mutableListOf()
        currentList.add(message)
        _chatMessages.value = _chatMessages.value + (threadId to currentList)

        // Update or create thread
        val existingIndex = _chatThreads.value.indexOfFirst { it.threadId == threadId }
        if (existingIndex >= 0) {
            _chatThreads.value = _chatThreads.value.map {
                if (it.threadId == threadId) {
                    it.copy(
                        lastMessageText = if (text.isNotBlank()) text else "📷 Photo message",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                } else it
            }
        } else {
            val newThread = ChatThreadEntity(
                threadId = threadId,
                recipientId = threadId,
                recipientUsername = recipientUsername.ifBlank { "User" },
                recipientAvatarUrl = recipientAvatar,
                lastMessageText = if (text.isNotBlank()) text else "📷 Photo message",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                isOnline = false,
                isMessageRequest = isMessageRequest
            )
            _chatThreads.value = listOf(newThread) + _chatThreads.value
        }

        // Send push notification to recipient
        val recipientId = if (threadId.contains("_")) {
            val parts = threadId.split("_")
            parts.firstOrNull { it != author.userId } ?: threadId
        } else {
            threadId
        }
        if (recipientId.isNotBlank() && recipientId != author.userId) {
            sendNotification(
                recipientUserId = recipientId,
                type = NotificationType.MESSAGE,
                title = "New Message 💬",
                message = if (text.isNotBlank()) text else "Sent a photo message 📷",
                relatedId = threadId
            )
        }

        scope.launch {
            val isSuccess = firestoreRepository.sendChatMessage(message, author.userId)
            val finalStatus = if (isSuccess) "SENT" else "FAILED"
            val list = _chatMessages.value[threadId]?.map {
                if (it.messageId == message.messageId) it.copy(sendStatus = finalStatus) else it
            } ?: emptyList()
            _chatMessages.value = _chatMessages.value + (threadId to list)

            // If recipient is demo / simulated creator contact, simulate active typing dots & reply
            if (recipientId.startsWith("user_") || recipientId.startsWith("demo_") || threadId.startsWith("thread_") || threadId.startsWith("demo_")) {
                kotlinx.coroutines.delay(700)
                _typingStatus.value = _typingStatus.value + (threadId to true)
                kotlinx.coroutines.delay(1800)
                _typingStatus.value = _typingStatus.value - threadId

                val replies = listOf(
                    "Hey! Got your message 😊",
                    "Awesome! Thanks for messaging ❤️",
                    "Hey @${author.username}, how are you doing today?",
                    "Super cool! Talk to you soon ✨",
                    "Got it! Let's connect on a video call later 📞",
                    "Nice! Just checked out your latest post 🌟"
                )
                val replyText = replies.random()
                val replyMsg = ChatMessageEntity(
                    messageId = "m_${UUID.randomUUID()}",
                    threadId = threadId,
                    senderId = recipientId,
                    senderUsername = recipientUsername.ifBlank { "User" },
                    text = replyText,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = false,
                    isRead = false,
                    sendStatus = "SENT"
                )
                val currentUpdated = _chatMessages.value[threadId]?.toMutableList() ?: mutableListOf()
                currentUpdated.add(replyMsg)
                _chatMessages.value = _chatMessages.value + (threadId to currentUpdated)

                // Update thread
                _chatThreads.value = _chatThreads.value.map {
                    if (it.threadId == threadId) it.copy(
                        lastMessageText = replyText,
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = it.unreadCount + 1
                    ) else it
                }

                // Trigger Push Notification with alert sound effect
                NotificationHelper.showPushNotification(
                    context = context,
                    title = recipientUsername.ifBlank { "New Message" },
                    message = replyText,
                    type = NotificationType.MESSAGE,
                    relatedId = threadId,
                    fromUsername = recipientUsername
                )
            }
        }
    }

    fun retrySendMessage(threadId: String, messageId: String) {
        val targetMessage = _chatMessages.value[threadId]?.find { it.messageId == messageId } ?: return
        val author = _currentUser.value

        val list = _chatMessages.value[threadId]?.map {
            if (it.messageId == messageId) it.copy(sendStatus = "SENDING") else it
        } ?: emptyList()
        _chatMessages.value = _chatMessages.value + (threadId to list)

        scope.launch {
            val isSuccess = firestoreRepository.sendChatMessage(targetMessage, author.userId)
            val finalStatus = if (isSuccess) "SENT" else "FAILED"
            val updatedList = _chatMessages.value[threadId]?.map {
                if (it.messageId == messageId) it.copy(sendStatus = finalStatus) else it
            } ?: emptyList()
            _chatMessages.value = _chatMessages.value + (threadId to updatedList)
        }
    }

    fun sendVoiceMessage(threadId: String, voicePath: String, durationSeconds: Int, recipientUsername: String = "", recipientAvatar: String = "", isMessageRequest: Boolean = false) {
        if (voicePath.isBlank()) return
        val author = _currentUser.value
        val message = ChatMessageEntity(
            messageId = "m_${UUID.randomUUID()}",
            threadId = threadId,
            senderId = author.userId,
            senderUsername = author.username,
            text = "🎤 Voice Message (${durationSeconds}s)",
            mediaUrl = voicePath,
            isMedia = true,
            isVoiceMessage = true,
            voiceDurationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            isRead = true,
            sendStatus = "SENDING"
        )
        setTyping(threadId, false)

        val currentList = _chatMessages.value[threadId]?.toMutableList() ?: mutableListOf()
        currentList.add(message)
        _chatMessages.value = _chatMessages.value + (threadId to currentList)

        val existingIndex = _chatThreads.value.indexOfFirst { it.threadId == threadId }
        if (existingIndex >= 0) {
            _chatThreads.value = _chatThreads.value.map {
                if (it.threadId == threadId) {
                    it.copy(
                        lastMessageText = "🎤 Voice message (${durationSeconds}s)",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                } else it
            }
        } else {
            val newThread = ChatThreadEntity(
                threadId = threadId,
                recipientId = threadId,
                recipientUsername = recipientUsername.ifBlank { "User" },
                recipientAvatarUrl = recipientAvatar,
                lastMessageText = "🎤 Voice message (${durationSeconds}s)",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                isOnline = false,
                isMessageRequest = isMessageRequest
            )
            _chatThreads.value = listOf(newThread) + _chatThreads.value
        }

        val recipientId = if (threadId.contains("_")) {
            val parts = threadId.split("_")
            parts.firstOrNull { it != author.userId } ?: threadId
        } else {
            threadId
        }
        if (recipientId.isNotBlank() && recipientId != author.userId) {
            sendNotification(
                recipientUserId = recipientId,
                type = NotificationType.MESSAGE,
                title = "New Voice Message 🎤",
                message = "Sent a voice message (${durationSeconds}s)",
                relatedId = threadId
            )
        }

        scope.launch {
            val isSuccess = firestoreRepository.sendChatMessage(message, author.userId)
            val finalStatus = if (isSuccess) "SENT" else "FAILED"
            val list = _chatMessages.value[threadId]?.map {
                if (it.messageId == message.messageId) it.copy(sendStatus = finalStatus) else it
            } ?: emptyList()
            _chatMessages.value = _chatMessages.value + (threadId to list)
        }
    }

    fun acceptMessageRequest(threadId: String) {
        _chatThreads.value = _chatThreads.value.map {
            if (it.threadId == threadId) it.copy(isMessageRequest = false) else it
        }
    }

    fun reactToMessage(threadId: String, messageId: String, emoji: String) {
        val list = _chatMessages.value[threadId]?.map {
            if (it.messageId == messageId) it.copy(reaction = if (it.reaction == emoji) "" else emoji) else it
        } ?: return
        _chatMessages.value = _chatMessages.value + (threadId to list)
    }

    fun updateUserProfile(displayName: String, username: String, bio: String, website: String, isPrivate: Boolean, showOnlineStatus: Boolean = true, avatarUrl: String? = null) {
        val updated = _currentUser.value.copy(
            username = username,
            displayName = displayName,
            bio = bio,
            website = website,
            isPrivate = isPrivate,
            showOnlineStatus = showOnlineStatus,
            avatarUrl = avatarUrl ?: _currentUser.value.avatarUrl
        )
        _currentUser.value = updated

        // Instantly update authorAvatarUrl, authorUsername, authorDisplayName across all local posts and reels
        val updatedPosts = _posts.value.map { post ->
            if (post.authorId == updated.userId || post.authorUsername.equals(updated.username, ignoreCase = true)) {
                post.copy(
                    authorAvatarUrl = updated.avatarUrl,
                    authorUsername = updated.username,
                    authorDisplayName = updated.displayName
                )
            } else post
        }
        _posts.value = updatedPosts

        val updatedReels = _reels.value.map { reel ->
            if (reel.authorId == updated.userId || reel.authorUsername.equals(updated.username, ignoreCase = true)) {
                reel.copy(
                    authorAvatarUrl = updated.avatarUrl,
                    authorUsername = updated.username,
                    authorDisplayName = updated.displayName
                )
            } else reel
        }
        _reels.value = updatedReels

        // Instantly update stories avatar
        val updatedStories = _stories.value.map { story ->
            if (story.userId == updated.userId || story.username.equals(updated.username, ignoreCase = true)) {
                story.copy(
                    userAvatarUrl = updated.avatarUrl,
                    username = updated.username
                )
            } else story
        }
        _stories.value = updatedStories

        // Instantly update comments avatar
        val newCommentsMap = _comments.value.mapValues { (_, commentList) ->
            commentList.map { comment ->
                if (comment.authorId == updated.userId || comment.authorUsername.equals(updated.username, ignoreCase = true)) {
                    comment.copy(
                        authorAvatarUrl = updated.avatarUrl,
                        authorUsername = updated.username
                    )
                } else comment
            }
        }
        _comments.value = newCommentsMap

        scope.launch {
            try {
                firestoreRepository.syncUserToFirestore(updated)
            } catch (_: Exception) {}
        }
    }

    fun createTextPost(
        text: String,
        bgGradient: String,
        fontStyle: String,
        fontColor: String,
        alignment: String,
        caption: String,
        privacy: PrivacyLevel = PrivacyLevel.PUBLIC
    ) {
        val user = _currentUser.value
        val authorName = user.displayName.ifBlank { user.username }
        val bodyContent = if (caption.isNotBlank()) "$text - $caption" else text
        val roarFormattedCaption = if (bodyContent.contains("__ roar 🦁")) bodyContent else "$authorName __ roar 🦁 $bodyContent"

        val newPost = PostEntity(
            id = "txt_${System.currentTimeMillis()}",
            authorId = user.userId,
            authorUsername = user.username,
            authorDisplayName = user.displayName,
            authorAvatarUrl = user.avatarUrl,
            imageUrl = "",
            caption = roarFormattedCaption,
            privacy = privacy.name,
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            timestamp = System.currentTimeMillis(),
            isTextOnly = true,
            textBgGradient = bgGradient,
            textFontStyle = fontStyle,
            textFontColor = fontColor,
            textAlignment = alignment
        )
        _posts.value = listOf(newPost) + _posts.value
        scope.launch {
            firestoreRepository.publishPost(newPost)
            socialDao.insertPost(newPost)
        }
    }

    suspend fun checkUsernameAvailable(username: String, currentUserId: String): Boolean {
        val clean = username.trim().lowercase()
        if (clean.isBlank()) return false
        return firestoreRepository.isUsernameAvailable(clean, currentUserId)
    }

    suspend fun searchUserInFirestore(query: String): UserData? {
        return firestoreRepository.findUserBySearchQuery(query)
    }

    suspend fun searchUsersInFirestore(query: String): List<UserData> {
        return firestoreRepository.searchUsersByQuery(query)
    }

    suspend fun getRecommendedCreators(): List<UserData> {
        val remote = firestoreRepository.getRecommendedCreators()
        val currentUserId = _currentUser.value.userId
        return remote.filter { it.userId != currentUserId }
    }

    suspend fun syncOnlineData() {
        val blocked = _blockedUserIds.value
        // Sync online posts
        val onlinePosts = onlineSyncService.fetchLiveOnlinePosts().filter { it.authorId !in blocked }
        if (onlinePosts.isNotEmpty()) {
            val existingIds = _posts.value.map { it.id }.toSet()
            val newItems = onlinePosts.filter { it.id !in existingIds }
            _posts.value = (newItems + _posts.value).filter { it.authorId !in blocked }
            try {
                socialDao.insertPosts(onlinePosts)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error caching synced posts to Room: ${e.message}")
            }
        }

        // Sync online reels
        val onlineReels = onlineSyncService.fetchLiveOnlineReels().filter { it.authorId !in blocked }
        if (onlineReels.isNotEmpty()) {
            val existingReelIds = _reels.value.map { it.id }.toSet()
            val newReels = onlineReels.filter { it.id !in existingReelIds }
            _reels.value = (newReels + _reels.value).filter { it.authorId !in blocked }
            try {
                onlineReels.forEach { socialDao.insertReel(it) }
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error caching synced reels to Room: ${e.message}")
            }
        }

        // Sync online stories
        val onlineStories = onlineSyncService.fetchLiveOnlineStories().filter { it.userId !in blocked }
        if (onlineStories.isNotEmpty()) {
            val existingStoryIds = _stories.value.map { it.id }.toSet()
            val newStories = onlineStories.filter { it.id !in existingStoryIds }
            _stories.value = (newStories + _stories.value).filter { it.userId !in blocked }
        }

        updateFollowedUserIds()
    }

    suspend fun syncOnlinePosts() {
        syncOnlineData()
    }

    fun blockUser(userId: String, username: String, displayName: String = "", avatarUrl: String = "") {
        if (userId.isBlank()) return
        val currentBlocked = _blockedUserIds.value.toMutableSet()
        currentBlocked.add(userId)
        _blockedUserIds.value = currentBlocked

        val currentList = _blockedList.value.filter { it.userId != userId }.toMutableList()
        currentList.add(
            BlockedUser(
                userId = userId,
                username = username.ifBlank { "User" },
                displayName = displayName,
                avatarUrl = avatarUrl
            )
        )
        _blockedList.value = currentList

        // Immediately filter out blocked creator's posts, reels, and stories
        _posts.value = _posts.value.filter { it.authorId != userId }
        _reels.value = _reels.value.filter { it.authorId != userId }
        _stories.value = _stories.value.filter { it.userId != userId }
    }

    fun unblockUser(userId: String) {
        val currentBlocked = _blockedUserIds.value.toMutableSet()
        currentBlocked.remove(userId)
        _blockedUserIds.value = currentBlocked
        _blockedList.value = _blockedList.value.filter { it.userId != userId }

        scope.launch {
            syncOnlineData()
        }
    }

    fun reportContent(targetId: String, targetType: String, authorUsername: String, reason: String) {
        val item = ReportedItem(
            id = "rep_${System.currentTimeMillis()}",
            targetId = targetId,
            targetType = targetType,
            authorUsername = authorUsername,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        val list = _reportedList.value.toMutableList()
        list.add(0, item)
        _reportedList.value = list
    }

    fun getAvailableAudioTracks(): List<ComprehensiveAudioTrack> {
        val tracksFromReels = _reels.value.mapNotNull { reel ->
            if (reel.audioTitle.isNotBlank() && (reel.videoUrl.isNotBlank() || reel.thumbnailUri.isNotBlank())) {
                val mediaUrl = reel.videoUrl.ifBlank { reel.thumbnailUri }
                ComprehensiveAudioTrack(
                    id = "reel_audio_${reel.id}",
                    title = reel.audioTitle,
                    artist = reel.audioArtist.ifBlank { reel.authorDisplayName.ifBlank { reel.authorUsername } },
                    durationSeconds = 30,
                    coverUrl = reel.thumbnailUri,
                    rawResId = null,
                    audioUrl = mediaUrl,
                    streamUrl = mediaUrl
                )
            } else null
        }.distinctBy { it.title.trim().lowercase() }

        return tracksFromReels
    }

    fun performAutoCleanupOfExpiredReelsAndPosts() {
        val tenDaysInMs = 10L * 24 * 60 * 60 * 1000 // 10 days in milliseconds
        val now = System.currentTimeMillis()
        val expirationThreshold = now - tenDaysInMs

        scope.launch {
            try {
                // 1. Clean Reels older than 10 days
                val expiredReels = _reels.value.filter { it.timestamp < expirationThreshold }
                expiredReels.forEach { reel ->
                    Log.d("SocialRepository", "Auto-deleting expired Reel (10 Days limit reached): ${reel.id} (Timestamp: ${reel.timestamp})")
                    // Remove from local list
                    _reels.value = _reels.value.filterNot { it.id == reel.id }
                    // Remove from Firestore
                    firestoreRepository.deleteReelFromFirestore(reel.id, reel.authorId)
                }

                // 2. Clean regular Video Posts older than 10 days (including uploaded Reels)
                val expiredVideoPosts = _posts.value.filter { 
                    (it.type == "video" || it.imageUrl.contains("video", ignoreCase = true) || it.mediaUrl.contains("video", ignoreCase = true)) 
                    && it.timestamp < expirationThreshold 
                }
                expiredVideoPosts.forEach { post ->
                    Log.d("SocialRepository", "Auto-deleting expired Video Post (10 Days limit reached): ${post.id} (Timestamp: ${post.timestamp})")
                    // Remove from local list
                    _posts.value = _posts.value.filterNot { it.id == post.id }
                    // Remove from Firestore
                    firestoreRepository.deletePostFromFirestore(post.id, post.authorId)
                }
            } catch (e: Exception) {
                Log.e("SocialRepository", "Failed performing auto-cleanup: ${e.message}", e)
            }
        }
    }
}

