package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class CallSoundManager(private val context: Context) {
    private val TAG = "CallSoundManager"

    private var toneGenerator: ToneGenerator? = null
    private var incomingRingtone: Ringtone? = null
    private var outgoingMediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isPlaying = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var outgoingRingRunnable: Runnable? = null

    init {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator init failed: ${e.message}")
        }
    }

    /**
     * Outgoing Call: Plays the realistic cellular/VoIP ringback tone (घंटी बजने की आवाज़).
     * Repeats ringback tone every 3.5 seconds until answered or ended.
     */
    fun startOutgoingRingtone() {
        stopAll()
        if (!SoundSettingsManager.isCallRingtoneAllowed(context)) {
            return
        }
        isPlaying = true
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            
            outgoingRingRunnable = object : Runnable {
                override fun run() {
                    if (!isPlaying) return
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                    } catch (e: Exception) {
                        Log.w(TAG, "ToneGenerator play failed: ${e.message}")
                    }
                    mainHandler.postDelayed(this, 3500)
                }
            }
            outgoingRingRunnable?.let { mainHandler.post(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start outgoing ringtone: ${e.message}")
        }
    }

    /**
     * Incoming Call: Plays the loud phone ringtone and vibrates (इनकमिंग कॉल रिंगटोन).
     */
    fun startIncomingRingtone() {
        stopAll()
        val isSoundOn = SoundSettingsManager.isCallRingtoneAllowed(context)
        val isVibrationOn = SoundSettingsManager.isVibrationAllowed(context)

        if (!isSoundOn && !isVibrationOn) {
            return
        }
        isPlaying = true

        if (isSoundOn) {
            try {
                val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                incomingRingtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    incomingRingtone?.isLooping = true
                }
                incomingRingtone?.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play incoming ringtone: ${e.message}")
            }
        }

        if (isVibrationOn) {
            try {
                // Vibrate pattern: 0ms delay, vibrate 1000ms, sleep 1000ms
                val pattern = longArrayOf(0, 1000, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to vibrate: ${e.message}")
            }
        }
    }

    /**
     * When call is answered / connected: Plays connection confirmation beep.
     */
    fun playConnectedTone() {
        stopAll()
        if (!SoundSettingsManager.isSoundAllowed(context)) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            mainHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {}
            }, 600)
        } catch (_: Exception) {}
    }

    /**
     * When call ends / hangs up: Plays call disconnect tone.
     */
    fun playEndCallTone() {
        stopAll()
        if (!SoundSettingsManager.isSoundAllowed(context)) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
            mainHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {}
            }, 600)
        } catch (_: Exception) {}
    }

    /**
     * Stops all ringing, audio tones, and vibrations cleanly.
     */
    fun stopAll() {
        isPlaying = false
        outgoingRingRunnable?.let { mainHandler.removeCallbacks(it) }
        outgoingRingRunnable = null

        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        try {
            if (incomingRingtone?.isPlaying == true) {
                incomingRingtone?.stop()
            }
            incomingRingtone = null
        } catch (_: Exception) {}

        try {
            outgoingMediaPlayer?.stop()
            outgoingMediaPlayer?.release()
            outgoingMediaPlayer = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
