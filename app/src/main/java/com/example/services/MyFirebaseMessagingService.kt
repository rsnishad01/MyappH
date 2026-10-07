package com.example.services

import com.example.data.NotificationType
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "HundredGram"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "You have a new update"

        val typeStr = remoteMessage.data["type"] ?: "SYSTEM_ALERT"
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (_: Exception) {
            NotificationType.SYSTEM_ALERT
        }
        val relatedId = remoteMessage.data["relatedId"] ?: ""
        val fromUser = remoteMessage.data["fromUser"] ?: ""

        NotificationHelper.showPushNotification(
            context = applicationContext,
            title = title,
            message = body,
            type = type,
            relatedId = relatedId,
            fromUsername = fromUser
        )
    }
}
