package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NotificationType
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_NOTIFICATIONS = "hundredgram_alerts_v2"
    const val CHANNEL_CALLS = "hundredgram_calls_v2"

    private val notificationIdCounter = AtomicInteger(1001)

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            // 1. General Social & Chat Alerts Channel
            val socialChannel = NotificationChannel(
                CHANNEL_NOTIFICATIONS,
                "Social & Messages (सौशल व संदेश)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifications for likes, comments, shares, follows, and direct messages."
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(socialChannel)

            // 2. Incoming Calls Channel
            val callSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val callAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Voice & Video Calls (कॉल रिंगटोन)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts."
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
                setSound(callSoundUri, callAudioAttributes)
            }
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    /**
     * Plays an audible notification alert sound effect immediately.
     */
    fun playAlertSound(context: Context) {
        if (!SoundSettingsManager.isNotificationSoundAllowed(context)) {
            return
        }
        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play alert sound: ${e.message}")
        }

        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 90)
            tg.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 250)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { tg.release() } catch (_: Exception) {}
            }, 500)
        } catch (_: Exception) {}
    }

    /**
     * Posts a system push notification with alert sound effect, vibration and heads-up banner.
     */
    fun showPushNotification(
        context: Context,
        title: String,
        message: String,
        type: NotificationType = NotificationType.SYSTEM_ALERT,
        relatedId: String = "",
        fromUsername: String = ""
    ) {
        try {
            initNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NOTIFICATION_TYPE", type.name)
                putExtra("RELATED_ID", relatedId)
                putExtra("FROM_USERNAME", fromUsername)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationIdCounter.incrementAndGet(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isSoundOn = SoundSettingsManager.isNotificationSoundAllowed(context)
            val isVibrationOn = SoundSettingsManager.isVibrationAllowed(context)
            val defaultSound = if (isSoundOn) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null

            val builder = NotificationCompat.Builder(context, CHANNEL_NOTIFICATIONS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (isSoundOn && defaultSound != null) {
                builder.setSound(defaultSound)
            } else {
                builder.setSilent(true)
            }

            if (isVibrationOn) {
                builder.setVibrate(longArrayOf(0, 250, 150, 250))
            } else {
                builder.setVibrate(longArrayOf(0))
            }

            val notificationManager = NotificationManagerCompat.from(context)
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                val notifId = notificationIdCounter.incrementAndGet()
                notificationManager.notify(notifId, builder.build())
            }

            if (isSoundOn) {
                playAlertSound(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification: ${e.message}", e)
        }
    }
}
