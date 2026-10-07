package com.example.data.crashlytics

import android.util.Log

object AppCrashlytics {
    private const val TAG = "HundredGram_Crashlytics"

    fun log(message: String) {
        Log.i(TAG, message)
    }

    fun logException(throwable: Throwable) {
        Log.e(TAG, "Caught Exception: ${throwable.localizedMessage}", throwable)
    }

    fun setUserId(userId: String) {
        Log.d(TAG, "User tracking set to: $userId")
    }

    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Custom attribute: $key = $value")
    }
}
