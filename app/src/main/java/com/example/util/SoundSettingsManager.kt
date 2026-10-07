package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SoundSettingsManager {
    private const val PREFS_NAME = "hundredgram_sound_settings"
    private const val KEY_MASTER_SOUND = "key_master_sound"
    private const val KEY_NOTIFICATION_SOUND = "key_notification_sound"
    private const val KEY_CALL_RINGTONE = "key_call_ringtone"
    private const val KEY_VIBRATION = "key_vibration"

    private val _isMasterSoundEnabled = MutableStateFlow(true)
    val isMasterSoundEnabled: StateFlow<Boolean> = _isMasterSoundEnabled.asStateFlow()

    private val _isNotificationSoundEnabled = MutableStateFlow(true)
    val isNotificationSoundEnabled: StateFlow<Boolean> = _isNotificationSoundEnabled.asStateFlow()

    private val _isCallRingtoneEnabled = MutableStateFlow(true)
    val isCallRingtoneEnabled: StateFlow<Boolean> = _isCallRingtoneEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        _isMasterSoundEnabled.value = prefs.getBoolean(KEY_MASTER_SOUND, true)
        _isNotificationSoundEnabled.value = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
        _isCallRingtoneEnabled.value = prefs.getBoolean(KEY_CALL_RINGTONE, true)
        _isVibrationEnabled.value = prefs.getBoolean(KEY_VIBRATION, true)
        isInitialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setMasterSound(context: Context, enabled: Boolean) {
        _isMasterSoundEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_MASTER_SOUND, enabled).apply()
    }

    fun setNotificationSound(context: Context, enabled: Boolean) {
        _isNotificationSoundEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply()
    }

    fun setCallRingtone(context: Context, enabled: Boolean) {
        _isCallRingtoneEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_CALL_RINGTONE, enabled).apply()
    }

    fun setVibration(context: Context, enabled: Boolean) {
        _isVibrationEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isSoundAllowed(context: Context): Boolean {
        init(context)
        return _isMasterSoundEnabled.value
    }

    fun isNotificationSoundAllowed(context: Context): Boolean {
        init(context)
        return _isMasterSoundEnabled.value && _isNotificationSoundEnabled.value
    }

    fun isCallRingtoneAllowed(context: Context): Boolean {
        init(context)
        return _isMasterSoundEnabled.value && _isCallRingtoneEnabled.value
    }

    fun isVibrationAllowed(context: Context): Boolean {
        init(context)
        return _isVibrationEnabled.value
    }
}
