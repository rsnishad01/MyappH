package com.example.data.auth

import android.content.Context
import com.example.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebasePhoneAuthManager(
    private val context: Context
) {
    private var verificationId: String? = null

    fun setVerificationId(id: String) {
        this.verificationId = id
    }

    suspend fun requestOtp(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) = withContext(Dispatchers.IO) {
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneNumberAndSignIn(phoneNumber: String, otpCode: String): Result<UserData> =
        withContext(Dispatchers.IO) {
            try {
                val cleanOtp = otpCode.trim()
                if (cleanOtp.length != 6) {
                    return@withContext Result.failure(IllegalArgumentException("Please enter a 6-digit code."))
                }

                val currentVerId = verificationId
                if (currentVerId.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalStateException("Verification ID missing. Request OTP again."))
                }

                val auth = FirebaseAuth.getInstance()
                val credential = PhoneAuthProvider.getCredential(currentVerId, cleanOtp)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val cleanPhone = phoneNumber.takeLast(4)
                    val user = UserData(
                        userId = firebaseUser.uid,
                        username = "user_${cleanPhone.ifBlank { firebaseUser.uid.take(4) }}",
                        displayName = firebaseUser.displayName ?: "Member $cleanPhone",
                        bio = "HundredGram Member 📱",
                        avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                        followersCount = 0,
                        followingCount = 0,
                        postsCount = 0,
                        isVerified = true
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

}
