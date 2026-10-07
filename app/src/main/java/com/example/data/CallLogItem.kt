package com.example.data

import java.io.Serializable

enum class CallType {
    VOICE, VIDEO
}

enum class CallDirection {
    INCOMING, OUTGOING, MISSED
}

data class CallLogItem(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val type: CallType = CallType.VIDEO,
    val direction: CallDirection = CallDirection.OUTGOING,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
) : Serializable
