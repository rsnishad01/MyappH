package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogItem
import com.example.data.ChatMessageEntity
import com.example.data.ChatThreadEntity
import com.example.data.CommentEntity
import com.example.data.PostEntity
import com.example.data.PrivacyLevel
import com.example.data.ReelVideo
import com.example.data.SocialRepository
import com.example.data.StoryEntity
import com.example.data.UserData
import com.example.data.CallEntity
import com.example.data.CallStatus
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.data.nearby.NearbyEvent
import com.example.data.nearby.NearbyUser
import com.example.data.network.NetworkStatus
import com.example.data.storage.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

sealed class ScreenDestination {
    object Auth : ScreenDestination()
    object Feed : ScreenDestination()
    object Explore : ScreenDestination()
    object Reels : ScreenDestination()
    object DirectMessages : ScreenDestination()
    data class ChatDetail(val threadId: String, val recipientUsername: String, val recipientAvatar: String) : ScreenDestination()
    data class StoryViewer(val storyIndex: Int = 0, val userIdFilter: String? = null) : ScreenDestination()
    object Camera : ScreenDestination()
    object FilterStudio : ScreenDestination()
    object CreateReel : ScreenDestination()
    object Profile : ScreenDestination()
    data class Call(
        val callName: String,
        val isVideo: Boolean,
        val callId: String = "",
        val isIncoming: Boolean = false,
        val callerAvatar: String = ""
    ) : ScreenDestination()
    object LiveRoom : ScreenDestination()
    object Notifications : ScreenDestination()
    data class CreatorProfile(val userId: String) : ScreenDestination()
    data class SongDetail(
        val audioTitle: String,
        val audioArtist: String,
        val audioUrl: String = "",
        val sourceReelId: String = ""
    ) : ScreenDestination()
    object Drafts : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SocialRepository(application)
    val draftRepository = repository.draftRepository

    val allDrafts: StateFlow<List<com.example.data.DraftEntity>> = repository.draftRepository.allDrafts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val draftsCount: StateFlow<Int> = repository.draftRepository.draftsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentEditingDraftId = MutableStateFlow<String?>(null)
    val draftInitialCaption = MutableStateFlow("")
    val draftInitialLocation = MutableStateFlow("")
    val draftInitialFilterId = MutableStateFlow("normal")

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val currentUser: StateFlow<UserData> = repository.currentUser
    val posts: StateFlow<List<PostEntity>> = repository.posts
    val stories: StateFlow<List<StoryEntity>> = repository.stories
    val reels: StateFlow<List<ReelVideo>> = repository.reels
    val chatThreads: StateFlow<List<ChatThreadEntity>> = repository.chatThreads
    val chatMessages: StateFlow<Map<String, List<ChatMessageEntity>>> = repository.chatMessages
    val comments: StateFlow<Map<String, List<CommentEntity>>> = repository.comments
    val callLogs: StateFlow<List<CallLogItem>> = repository.callLogs
    val audioPlaybackInfo = repository.reelAudioService.playbackInfo
    val voiceRecordingState = repository.voiceMessageManager.recordingState
    val voicePlaybackState = repository.voiceMessageManager.playbackState

    val networkStatus: StateFlow<NetworkStatus> = repository.connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStatus.Available)

    val followedUserIds: StateFlow<Set<String>> = repository.followedUserIds

    private val _networkSpeed = MutableStateFlow("0.0 KB/s")
    val networkSpeed: StateFlow<String> = _networkSpeed.asStateFlow()

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Feed)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _uploadMessage = MutableStateFlow("Uploading...")
    val uploadMessage: StateFlow<String> = _uploadMessage.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    private val _loginPromptReason = MutableStateFlow("इस एक्शन के लिए पहले लॉगिन करें")
    val loginPromptReason: StateFlow<String> = _loginPromptReason.asStateFlow()

    private val _nearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())
    val nearbyUsers: StateFlow<List<NearbyUser>> = _nearbyUsers.asStateFlow()

    private val _nearbyEvents = MutableStateFlow<List<NearbyEvent>>(emptyList())
    val nearbyEvents: StateFlow<List<NearbyEvent>> = _nearbyEvents.asStateFlow()

    private val _searchedUsers = MutableStateFlow<List<com.example.data.UserData>>(emptyList())
    val searchedUsers: StateFlow<List<com.example.data.UserData>> = _searchedUsers.asStateFlow()

    private val _isSearchingUsers = MutableStateFlow(false)
    val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers.asStateFlow()

    private val _recommendedCreators = MutableStateFlow<List<com.example.data.UserData>>(emptyList())
    val recommendedCreators: StateFlow<List<com.example.data.UserData>> = _recommendedCreators.asStateFlow()

    private val _selectedFilterImageUri = MutableStateFlow<Uri?>(null)
    val selectedFilterImageUri: StateFlow<Uri?> = _selectedFilterImageUri.asStateFlow()

    val autoStartReelRecording = MutableStateFlow(false)
    val selectedReelUri = MutableStateFlow<Uri?>(null)
    val selectedReelThumbnailUri = MutableStateFlow<Uri?>(null)
    val selectedReelAudio = MutableStateFlow<String?>(null)
    val selectedReelAudioTrack = MutableStateFlow<ComprehensiveAudioTrack?>(null)
    val selectedReelAudioUrl = MutableStateFlow<String?>(null)
    val selectedReelAudioArtist = MutableStateFlow<String?>(null)
    val selectedReelForPlayer = MutableStateFlow<String?>(null)

    fun extractAndUseReelAudio(reel: ReelVideo) {
        try {
            val title = reel.audioTitle.ifBlank { "${reel.authorDisplayName.ifBlank { reel.authorUsername }}'s Sound" }
            val artist = reel.audioArtist.ifBlank { reel.authorDisplayName.ifBlank { reel.authorUsername } }
            val mediaUrl = reel.videoUrl.ifBlank { reel.thumbnailUri }
            val track = ComprehensiveAudioTrack(
                id = "extracted_${reel.id}_${System.currentTimeMillis()}",
                title = title,
                artist = artist,
                durationSeconds = 30,
                coverUrl = reel.thumbnailUri,
                rawResId = null,
                audioUrl = mediaUrl,
                streamUrl = mediaUrl
            )
            selectedReelAudio.value = title
            selectedReelAudioArtist.value = artist
            selectedReelAudioUrl.value = mediaUrl
            selectedReelAudioTrack.value = track

            if (selectedReelUri.value != null) {
                navigateTo(ScreenDestination.CreateReel)
            } else {
                navigateTo(ScreenDestination.Camera)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error extracting reel audio: ${e.message}")
        }
    }

    fun extractAndUseAudioFromDetails(title: String, artist: String, audioUrl: String) {
        try {
            val effectiveTitle = title.ifBlank { "Original Audio" }
            val effectiveArtist = artist.ifBlank { "Creator" }
            val track = ComprehensiveAudioTrack(
                id = "extracted_${System.currentTimeMillis()}",
                title = effectiveTitle,
                artist = effectiveArtist,
                durationSeconds = 30,
                coverUrl = "",
                rawResId = null,
                audioUrl = audioUrl,
                streamUrl = audioUrl
            )
            selectedReelAudio.value = effectiveTitle
            selectedReelAudioArtist.value = effectiveArtist
            selectedReelAudioUrl.value = audioUrl
            selectedReelAudioTrack.value = track

            if (selectedReelUri.value != null) {
                navigateTo(ScreenDestination.CreateReel)
            } else {
                navigateTo(ScreenDestination.Camera)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error using audio details: ${e.message}")
        }
    }

    fun openReelInPlayer(reelId: String) {
        selectedReelForPlayer.value = reelId
        recordReelView(reelId)
        navigateTo(ScreenDestination.Reels)
    }

    init {
        loadNearbyData()
        loadRecommendedCreators()
        


        // Real-time network speed monitor (KB/MB) using standard Android TrafficStats
        viewModelScope.launch {
            var lastRxBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
            var lastTxBytes = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
            if (lastRxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) lastRxBytes = 0
            if (lastTxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) lastTxBytes = 0
            
            while (true) {
                delay(1000)
                var currentRxBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
                var currentTxBytes = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
                if (currentRxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) currentRxBytes = 0
                if (currentTxBytes == android.net.TrafficStats.UNSUPPORTED.toLong()) currentTxBytes = 0
                
                val rxSpeed = currentRxBytes - lastRxBytes
                val txSpeed = currentTxBytes - lastTxBytes
                val totalSpeedBytes = (if (rxSpeed > 0) rxSpeed else 0) + (if (txSpeed > 0) txSpeed else 0)
                
                _networkSpeed.value = when {
                    totalSpeedBytes < 1024 -> "$totalSpeedBytes B/s"
                    totalSpeedBytes < 1024 * 1024 -> String.format("%.1f KB/s", totalSpeedBytes / 1024.0)
                    else -> String.format("%.1f MB/s", totalSpeedBytes / (1024.0 * 1024.0))
                }
                
                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
            }
        }
    }

    fun requireAuth(reason: String = "इस फीचर के लिए पहले लॉगिन करें", onAuthorized: () -> Unit) {
        if (isLoggedIn.value) {
            onAuthorized()
        } else {
            _loginPromptReason.value = reason
            _showLoginPrompt.value = true
        }
    }

    fun dismissLoginPrompt() {
        _showLoginPrompt.value = false
    }

    fun openLoginScreen() {
        _showLoginPrompt.value = false
        navigateTo(ScreenDestination.Auth)
    }

    fun logout() {
        repository.logoutUser()
        navigateTo(ScreenDestination.Auth)
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    private val backStack = mutableListOf<ScreenDestination>()

    fun navigateTo(destination: ScreenDestination) {
        if (_currentScreen.value != destination) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = destination
        }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
        } else {
            _currentScreen.value = ScreenDestination.Feed
        }
    }

    fun onLikePost(postId: String) {
        if (postId.startsWith("r_")) {
            onLikeReel(postId)
            return
        }
        requireAuth("पोस्ट को लाइक / रिएक्ट करने के लिए पहले लॉगिन करें") {
            val post = posts.value.find { it.id == postId }
            repository.toggleLikePost(postId)
            if (post != null) {
                val isCurrentlyLiked = post.isLiked
                val newStatus = !isCurrentlyLiked
                val currentUserId = currentUser.value.userId
                val currentUsername = currentUser.value.username
                if (newStatus && post.authorId != currentUserId && !post.authorUsername.equals(currentUsername, ignoreCase = true)) {
                    repository.sendNotification(
                        recipientUserId = post.authorId,
                        type = com.example.data.NotificationType.LIKE,
                        title = "New Like ❤️",
                        message = "liked your post.",
                        relatedId = post.id,
                        relatedThumbnail = post.imageUrl
                    )
                }
            }
        }
    }

    fun onBookmarkPost(postId: String) {
        if (postId.startsWith("r_")) {
            onBookmarkReel(postId)
            return
        }
        requireAuth("पोस्ट को सेव करने के लिए पहले लॉगिन करें") {
            repository.toggleBookmarkPost(postId)
        }
    }

    fun onAddComment(postId: String, text: String) {
        requireAuth("कमेंट करने के लिए पहले लॉगिन करें") {
            repository.addComment(postId, text)
            val post = posts.value.find { it.id == postId }
            val reel = reels.value.find { it.id == postId }
            val currentUserId = currentUser.value.userId
            val currentUsername = currentUser.value.username
            if (post != null && post.authorId != currentUserId && !post.authorUsername.equals(currentUsername, ignoreCase = true)) {
                repository.sendNotification(
                    recipientUserId = post.authorId,
                    type = com.example.data.NotificationType.COMMENT,
                    title = "New Comment 💬",
                    message = "commented: \"$text\"",
                    relatedId = post.id,
                    relatedThumbnail = post.imageUrl
                )
            } else if (reel != null && reel.authorId != currentUserId && !reel.authorUsername.equals(currentUsername, ignoreCase = true)) {
                repository.sendNotification(
                    recipientUserId = reel.authorId,
                    type = com.example.data.NotificationType.COMMENT,
                    title = "New Reel Comment 💬",
                    message = "commented on your reel: \"$text\"",
                    relatedId = reel.id,
                    relatedThumbnail = reel.thumbnailUri
                )
            }
        }
    }

    fun onLikeReel(reelId: String) {
        requireAuth("रील को लाइक / रिएक्ट करने के लिए पहले लॉगिन करें") {
            val reel = reels.value.find { it.id == reelId }
            repository.toggleLikeReel(reelId)
            if (reel != null) {
                val isCurrentlyLiked = reel.isLiked
                val newStatus = !isCurrentlyLiked
                val currentUserId = currentUser.value.userId
                val currentUsername = currentUser.value.username
                if (newStatus && reel.authorId != currentUserId && !reel.authorUsername.equals(currentUsername, ignoreCase = true)) {
                    repository.sendNotification(
                        recipientUserId = reel.authorId,
                        type = com.example.data.NotificationType.LIKE,
                        title = "New Reel Like ❤️",
                        message = "liked your reel.",
                        relatedId = reel.id,
                        relatedThumbnail = reel.thumbnailUri
                    )
                }
            }
        }
    }

    fun onBookmarkReel(reelId: String) {
        requireAuth("रील को सेव करने के लिए पहले लॉगिन करें") {
            repository.toggleBookmarkReel(reelId)
        }
    }

    fun onSendChatMessage(threadId: String, text: String, mediaUrl: String = "", recipientUsername: String = "", recipientAvatar: String = "", isMessageRequest: Boolean = false) {
        requireAuth("मैसेज भेजने के लिए पहले लॉगिन करें") {
            repository.sendChatMessage(threadId, text, recipientUsername, recipientAvatar, mediaUrl, isMessageRequest)
        }
    }

    fun onRetrySendMessage(threadId: String, messageId: String) {
        repository.retrySendMessage(threadId, messageId)
    }

    fun startListeningToChatThread(threadId: String) {
        repository.startListeningToChatThread(threadId)
    }

    fun stopListeningToChatThread(threadId: String) {
        repository.stopListeningToChatThread(threadId)
    }

    fun startVoiceRecording(): Boolean {
        return repository.voiceMessageManager.startRecording()
    }

    fun stopVoiceRecordingAndSend(threadId: String, recipientUsername: String = "", recipientAvatar: String = "", isMessageRequest: Boolean = false) {
        val duration = repository.voiceMessageManager.recordingState.value.durationSeconds
        val filePath = repository.voiceMessageManager.stopRecording(save = true)
        if (!filePath.isNullOrBlank()) {
            requireAuth("वॉयस मैसेज भेजने के लिए पहले लॉगिन करें") {
                repository.sendVoiceMessage(threadId, filePath, duration.coerceAtLeast(1), recipientUsername, recipientAvatar, isMessageRequest)
            }
        }
    }

    fun acceptMessageRequest(threadId: String) {
        repository.acceptMessageRequest(threadId)
    }

    fun cancelVoiceRecording() {
        repository.voiceMessageManager.stopRecording(save = false)
    }

    fun playVoiceMessage(messageId: String, audioUrl: String) {
        repository.voiceMessageManager.playVoiceMessage(messageId, audioUrl)
    }

    fun stopVoicePlayback() {
        repository.voiceMessageManager.stopPlayback()
    }

    fun onReactToMessage(threadId: String, messageId: String, emoji: String) {
        requireAuth("रिएक्शन देने के लिए पहले लॉगिन करें") {
            repository.reactToMessage(threadId, messageId, emoji)
        }
    }

    fun onDeletePost(postId: String) {
        repository.deletePost(postId)
    }

    fun onEditPost(postId: String, newCaption: String, newLocation: String, newPrivacy: com.example.data.PrivacyLevel = com.example.data.PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        repository.editPost(postId, newCaption, newLocation, newPrivacy, tags)
    }

    fun onEditReel(reelId: String, newCaption: String, newAudioTitle: String = "", newAudioArtist: String = "", privacy: com.example.data.PrivacyLevel = com.example.data.PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        repository.editReel(reelId, newCaption, newAudioTitle, newAudioArtist, privacy, tags)
    }

    fun onDeleteReel(reelId: String) {
        repository.deleteReel(reelId)
    }

    fun recordReelView(reelId: String) {
        repository.recordReelView(reelId)
    }

    fun onEndCall(
        callerName: String,
        callerAvatar: String = "",
        isVideo: Boolean,
        durationSeconds: Int,
        isOutgoing: Boolean = true,
        isMissed: Boolean = false
    ) {
        repository.addCallLog(
            callerName = callerName,
            callerAvatar = callerAvatar,
            isVideo = isVideo,
            durationSeconds = durationSeconds,
            isOutgoing = isOutgoing,
            isMissed = isMissed
        )
        navigateBack()
    }

    val typingStatus: StateFlow<Map<String, Boolean>> = repository.typingStatus
    val incomingCall: StateFlow<com.example.data.CallEntity?> = repository.incomingCall

    fun setTypingStatus(threadId: String, isTyping: Boolean) {
        repository.setTyping(threadId, isTyping)
    }

    fun initiateCall(name: String, receiverId: String, isVideo: Boolean) {
        val callId = "call_${java.util.UUID.randomUUID()}"
        val caller = currentUser.value
        val call = CallEntity(
            callId = callId,
            callerId = caller.userId,
            callerName = caller.displayName.ifBlank { caller.username },
            callerAvatar = caller.avatarUrl,
            receiverId = receiverId,
            isVideo = isVideo,
            status = CallStatus.RINGING
        )

        // Instantly navigate to CallScreen with 0 delay!
        navigateTo(
            ScreenDestination.Call(
                callName = name,
                isVideo = isVideo,
                callId = callId,
                isIncoming = false,
                callerAvatar = caller.avatarUrl
            )
        )

        viewModelScope.launch {
            try {
                repository.firestoreRepository.initiateCall(call)
            } catch (_: Exception) {}

            if (receiverId.isNotBlank() && receiverId != caller.userId) {
                repository.sendNotification(
                    recipientUserId = receiverId,
                    type = com.example.data.NotificationType.SYSTEM_ALERT,
                    title = "Incoming ${if (isVideo) "Video" else "Voice"} Call 📞",
                    message = "${caller.displayName.ifBlank { caller.username }} is calling you...",
                    relatedId = callId
                )
            }
        }
    }

    fun answerIncomingCall(call: com.example.data.CallEntity) {
        viewModelScope.launch {
            repository.firestoreRepository.answerCall(call.callId)
            navigateTo(
                ScreenDestination.Call(
                    callName = call.callerName,
                    isVideo = call.isVideo,
                    callId = call.callId,
                    isIncoming = true,
                    callerAvatar = call.callerAvatar
                )
            )
        }
    }

    fun declineIncomingCall(call: com.example.data.CallEntity) {
        viewModelScope.launch {
            repository.firestoreRepository.endCall(call.callId, CallStatus.ENDED)
        }
    }

    fun onSharePost(post: PostEntity) {
        val currentUserId = currentUser.value.userId
        val currentUsername = currentUser.value.username
        if (post.authorId != currentUserId && !post.authorUsername.equals(currentUsername, ignoreCase = true)) {
            repository.sendNotification(
                recipientUserId = post.authorId,
                type = com.example.data.NotificationType.SHARE,
                title = "Post Shared ↗️",
                message = "shared your post.",
                relatedId = post.id,
                relatedThumbnail = post.imageUrl
            )
        }
    }

    fun onShareReel(reel: com.example.data.ReelVideo) {
        val currentUserId = currentUser.value.userId
        val currentUsername = currentUser.value.username
        if (reel.authorId != currentUserId && !reel.authorUsername.equals(currentUsername, ignoreCase = true)) {
            repository.sendNotification(
                recipientUserId = reel.authorId,
                type = com.example.data.NotificationType.SHARE,
                title = "Reel Shared ↗️",
                message = "shared your reel.",
                relatedId = reel.id,
                relatedThumbnail = reel.thumbnailUri
            )
        }
    }

    fun clearCallHistory() {
        repository.clearCallLogs()
    }

    suspend fun getPopularLocations(): List<String> {
        return repository.locationManager.getPopularLocations()
    }

    suspend fun getCurrentLocationName(): String {
        return repository.locationManager.getCurrentLocationName()
    }

    fun onUpdateProfile(displayName: String, username: String, bio: String, website: String, isPrivate: Boolean, showOnlineStatus: Boolean = true, avatarUrl: String? = null) {
        repository.updateUserProfile(displayName, username, bio, website, isPrivate, showOnlineStatus, avatarUrl)
    }

    fun onCreateTextPost(
        text: String,
        bgGradient: String,
        fontStyle: String,
        fontColor: String,
        alignment: String,
        caption: String,
        privacy: PrivacyLevel = PrivacyLevel.PUBLIC
    ) {
        repository.createTextPost(text, bgGradient, fontStyle, fontColor, alignment, caption, privacy)
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        return repository.checkUsernameAvailable(username, currentUser.value.userId)
    }

    fun onUpdateAvatar(uri: Uri) {
        viewModelScope.launch {
            _uploadError.value = null
            _isUploading.value = true
            _uploadProgress.value = 0.05f
            _uploadMessage.value = "Updating profile picture..."
            val currentUserVal = repository.currentUser.value
            repository.storageManager.uploadMediaFlow(
                uri = uri,
                folder = "profile"
            ).collect { status ->
                when (status) {
                    is UploadStatus.Progress -> {
                        _isUploading.value = true
                        _uploadProgress.value = status.progressPercent
                        _uploadMessage.value = status.statusMessage
                    }
                    is UploadStatus.Error -> {
                        _isUploading.value = false
                        _uploadError.value = status.errorMessage
                        _uploadMessage.value = status.errorMessage
                    }
                    is UploadStatus.Success -> {
                        _uploadProgress.value = 1f
                        _uploadMessage.value = "Profile picture updated!"
                        repository.updateUserProfile(
                            displayName = currentUserVal.displayName,
                            username = currentUserVal.username,
                            bio = currentUserVal.bio,
                            website = currentUserVal.website,
                            isPrivate = currentUserVal.isPrivate,
                            avatarUrl = status.downloadUrl
                        )
                        _isUploading.value = false
                    }
                    is UploadStatus.Idle -> {}
                }
            }
        }
    }

    fun deactivateAccount(context: Context, reason: String = ""): String {
        val result = repository.deactivateAccount(context, reason)
        navigateTo(ScreenDestination.Auth)
        return result
    }

    fun loginUser(user: UserData, context: Context? = null) {
        if (context != null && user.isDeactivated) {
            viewModelScope.launch {
                val (isDeleted, message) = repository.checkAndProcessAccountDeactivationOnLogin(context, user)
                if (isDeleted) {
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    navigateTo(ScreenDestination.Auth)
                } else {
                    repository.loginUser(user.copy(isDeactivated = false, deactivationTimestamp = 0L))
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    navigateTo(ScreenDestination.Feed)
                }
            }
        } else {
            repository.loginUser(user)
            navigateTo(ScreenDestination.Feed)
        }
    }

    fun completeProfileSetup(updatedUser: UserData) {
        repository.updateUserProfile(
            displayName = updatedUser.displayName,
            username = updatedUser.username,
            bio = updatedUser.bio,
            website = updatedUser.website,
            isPrivate = updatedUser.isPrivate,
            avatarUrl = updatedUser.avatarUrl
        )
        // Note: For full persistence of new fields, SocialRepository needs an update, 
        // but for now, we will handle the state in the repository/Room.
        repository.loginUser(updatedUser.copy(isTermsAccepted = true))
    }

    fun onRefreshFeed() {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Syncing live feed..."
            _uploadProgress.value = 0.5f
            repository.syncOnlinePosts()
            _uploadProgress.value = 1f
            _isUploading.value = false
        }
    }

    fun createPost(type: String, caption: String, location: String, uri: Uri? = null, text: String = "", privacy: PrivacyLevel = PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        requireAuth("पोस्ट अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                
                if (type == "text") {
                    repository.publishPostAfterUpload(
                        mediaUrl = "", 
                        caption = caption, 
                        location = location,
                        type = type,
                        text = text,
                        privacy = privacy,
                        tags = tags
                    )
                    _uploadProgress.value = 1f
                    _uploadMessage.value = "Published to HundredGram!"
                    _isUploading.value = false
                    navigateTo(ScreenDestination.Feed)
                    return@launch
                }

                // For image/video
                if (uri == null) {
                    _uploadError.value = "Media required"
                    _isUploading.value = false
                    return@launch
                }

                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = if (type == "video") "reels" else "posts"
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            Log.d("UPLOADING", "${(status.progressPercent * 100).toInt()}%")
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            Log.e("UPLOADING", "Error: ${status.errorMessage}")
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            Log.d("UPLOADING", "Success: ${status.downloadUrl}")
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Published to HundredGram!"
                            repository.publishPostAfterUpload(
                                mediaUrl = status.downloadUrl, 
                                caption = caption, 
                                location = location,
                                type = type,
                                text = text,
                                publicId = status.publicId,
                                resourceType = status.resourceType,
                                format = status.format,
                                bytes = status.bytes,
                                width = status.width,
                                height = status.height,
                                folder = status.folder,
                                privacy = privacy,
                                tags = tags
                            )
                            _isUploading.value = false
                            // ... handle drafts ...
                            navigateTo(ScreenDestination.Feed)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun createStory(uri: Uri, caption: String, audioTrack: ComprehensiveAudioTrack? = null) {
        requireAuth("स्टोरी अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = "stories"
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Story added!"
                            repository.publishStoryAfterUpload(
                                mediaUrl = status.downloadUrl,
                                caption = caption,
                                audioTitle = audioTrack?.title ?: "",
                                audioArtist = audioTrack?.artist ?: "",
                                audioUrl = audioTrack?.audioUrl ?: "",
                                publicId = status.publicId,
                                resourceType = status.resourceType,
                                format = status.format,
                                bytes = status.bytes,
                                width = status.width,
                                height = status.height,
                                folder = status.folder
                            )
                            _isUploading.value = false
                            navigateTo(ScreenDestination.Feed)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun onDeleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
        }
    }

    fun createReel(uri: Uri, caption: String, audioTitle: String, audioArtist: String, privacy: PrivacyLevel = PrivacyLevel.PUBLIC, tags: List<String> = emptyList()) {
        requireAuth("रील अपलोड करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                _uploadError.value = null
                _isUploading.value = true
                _uploadProgress.value = 0.05f
                _uploadMessage.value = "Uploading..."

                val authorId = currentUser.value.userId
                repository.storageManager.uploadMediaFlow(
                    uri = uri,
                    folder = "reels"
                ).collect { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            _isUploading.value = true
                            _uploadProgress.value = status.progressPercent
                            _uploadMessage.value = status.statusMessage
                        }
                        is UploadStatus.Error -> {
                            _isUploading.value = false
                            _uploadError.value = status.errorMessage
                            _uploadMessage.value = status.errorMessage
                        }
                        is UploadStatus.Success -> {
                            _uploadProgress.value = 1f
                            _uploadMessage.value = "Reel published!"
                            val thumbUri = selectedReelThumbnailUri.value?.toString() ?: ""
                            repository.publishReelAfterUpload(
                                mediaUrl = status.downloadUrl,
                                caption = caption,
                                audioTitle = audioTitle,
                                audioArtist = audioArtist,
                                thumbnailUri = thumbUri,
                                publicId = status.publicId,
                                resourceType = status.resourceType,
                                format = status.format,
                                bytes = status.bytes,
                                width = status.width,
                                height = status.height,
                                folder = status.folder,
                                privacy = privacy,
                                tags = tags
                            )
                            _isUploading.value = false
                            val draftId = currentEditingDraftId.value
                            if (!draftId.isNullOrBlank()) {
                                viewModelScope.launch {
                                    repository.draftRepository.deleteDraft(draftId)
                                    currentEditingDraftId.value = null
                                }
                            }
                            navigateTo(ScreenDestination.Reels)
                        }
                        is UploadStatus.Idle -> {}
                    }
                }
            }
        }
    }

    fun savePostDraft(
        mediaUri: Uri,
        caption: String,
        location: String = "",
        filterId: String = "normal",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = currentEditingDraftId.value
            val savedId = repository.draftRepository.savePostDraft(
                id = id,
                mediaUri = mediaUri.toString(),
                caption = caption,
                location = location,
                filterId = filterId
            )
            currentEditingDraftId.value = savedId
            onSaved()
        }
    }

    fun saveReelDraft(
        mediaUri: Uri,
        caption: String,
        audioTitle: String = "",
        audioArtist: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = currentEditingDraftId.value
            val savedId = repository.draftRepository.saveReelDraft(
                id = id,
                mediaUri = mediaUri.toString(),
                caption = caption,
                audioTitle = audioTitle,
                audioArtist = audioArtist
            )
            currentEditingDraftId.value = savedId
            onSaved()
        }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch {
            repository.draftRepository.deleteDraft(id)
            if (currentEditingDraftId.value == id) {
                currentEditingDraftId.value = null
            }
        }
    }

    fun clearAllDrafts() {
        viewModelScope.launch {
            repository.draftRepository.clearAll()
            currentEditingDraftId.value = null
        }
    }

    fun openDraft(draft: com.example.data.DraftEntity) {
        currentEditingDraftId.value = draft.id
        if (draft.type == com.example.data.DraftType.POST.name) {
            _selectedFilterImageUri.value = Uri.parse(draft.mediaUri)
            draftInitialCaption.value = draft.caption
            draftInitialLocation.value = draft.location
            draftInitialFilterId.value = draft.filterId
            navigateTo(ScreenDestination.FilterStudio)
        } else {
            selectedReelUri.value = Uri.parse(draft.mediaUri)
            selectedReelAudio.value = draft.audioTitle
            draftInitialCaption.value = draft.caption
            navigateTo(ScreenDestination.CreateReel)
        }
    }

    fun setSelectedFilterImage(uri: Uri) {
        _selectedFilterImageUri.value = uri
        navigateTo(ScreenDestination.FilterStudio)
    }

    fun playAudioTrack(track: ComprehensiveAudioTrack) {
        repository.reelAudioService.playAudioTrack(track)
    }

    fun playReelAudio(reel: ReelVideo) {
        if (reel.audioTitle.isBlank() && reel.audioArtist.isBlank()) {
            // No custom audio soundtrack attached; video's own audio will play through VideoPlayer
            repository.reelAudioService.stop()
            return
        }
        val availableTracks = repository.getAvailableAudioTracks()
        if (availableTracks.isEmpty()) {
            repository.reelAudioService.stop()
            return
        }
        // Try matching by track title
        val matchedTrack = availableTracks.find {
            it.title.equals(reel.audioTitle, ignoreCase = true) ||
            reel.audioTitle.contains(it.title, ignoreCase = true) ||
            it.title.contains(reel.audioTitle, ignoreCase = true)
        }
        if (matchedTrack != null) {
            repository.reelAudioService.playAudioTrack(matchedTrack)
        } else {
            repository.reelAudioService.stop()
        }
    }

    fun stopAudio() {
        repository.reelAudioService.stop()
    }

    fun toggleMuteAudio() {
        repository.reelAudioService.toggleMute()
    }

    fun togglePlayPauseAudio() {
        repository.reelAudioService.togglePlayPause()
    }

    val notifications: StateFlow<List<com.example.data.NotificationEntity>> = repository.notifications

    fun clearNotifications() {
        repository.clearNotifications()
    }

    private fun loadNearbyData() {
        viewModelScope.launch {
            val result = repository.nearbyService.searchNearby()
            _nearbyUsers.value = result.users
            _nearbyEvents.value = result.events
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                _searchedUsers.value = emptyList()
                return@launch
            }
            _isSearchingUsers.value = true
            try {
                val results = repository.searchUsersInFirestore(trimmed)
                _searchedUsers.value = results
            } catch (e: Exception) {
                _searchedUsers.value = emptyList()
            } finally {
                _isSearchingUsers.value = false
            }
        }
    }

    fun loadRecommendedCreators() {
        viewModelScope.launch {
            try {
                val results = repository.getRecommendedCreators()
                _recommendedCreators.value = results
            } catch (e: Exception) {
                _recommendedCreators.value = emptyList()
            }
        }
    }

    suspend fun getFollowersOrFollowing(targetUserId: String, isFollowingTab: Boolean): List<UserData> {
        return repository.getFollowersOrFollowing(targetUserId, isFollowingTab)
    }

    fun toggleFollowUser(targetUserId: String, onSuccess: (Boolean) -> Unit = {}) {
        requireAuth("क्रिएटर को फ़ॉलो करने के लिए पहले लॉगिन करें") {
            viewModelScope.launch {
                val isFollowingNow = repository.toggleFollowUser(targetUserId)
                _isFollowingMap.value = _isFollowingMap.value + (targetUserId to isFollowingNow)
                onSuccess(isFollowingNow)
                
                // Send notification to the target creator (ONLY if followed and target is not current user)
                val currentUserId = currentUser.value.userId
                if (isFollowingNow && targetUserId.isNotBlank() && targetUserId != currentUserId) {
                    repository.sendNotification(
                        recipientUserId = targetUserId,
                        type = com.example.data.NotificationType.FOLLOW,
                        title = "New Follower 👤",
                        message = "started following you.",
                        relatedId = currentUserId
                    )
                }
            }
        }
    }

    private val _isFollowingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isFollowingMap: StateFlow<Map<String, Boolean>> = _isFollowingMap.asStateFlow()

    fun checkIfFollowing(targetUserId: String) {
        viewModelScope.launch {
            val following = repository.isFollowingUser(targetUserId)
            _isFollowingMap.value = _isFollowingMap.value + (targetUserId to following)
        }
    }

    val blockedUsers: StateFlow<List<com.example.data.BlockedUser>> = repository.blockedList
    val reportedItems: StateFlow<List<com.example.data.ReportedItem>> = repository.reportedList

    fun blockUser(userId: String, username: String, displayName: String = "", avatarUrl: String = "") {
        repository.blockUser(userId, username, displayName, avatarUrl)
    }

    fun unblockUser(userId: String) {
        repository.unblockUser(userId)
    }

    fun reportContent(targetId: String, targetType: String, authorUsername: String, reason: String) {
        repository.reportContent(targetId, targetType, authorUsername, reason)
    }

    suspend fun refreshFeed() {
        try {
            repository.syncOnlineData()
            loadRecommendedCreators()
            loadNearbyData()
        } catch (_: Exception) {}
    }
}
