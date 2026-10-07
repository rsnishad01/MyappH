package com.example.data.auth

import android.content.Context
import android.content.Intent
import com.example.data.UserData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GoogleAuthManager(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "372418336964-q81qbgr3r3pk3ttgoq522hv10sogmbi8.apps.googleusercontent.com"
        } catch (e: Exception) {
            "372418336964-q81qbgr3r3pk3ttgoq522hv10sogmbi8.apps.googleusercontent.com"
        }

        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        
        if (webClientId.isNotBlank()) {
            try {
                builder.requestIdToken(webClientId)
            } catch (e: Exception) {
                // Fallback to standard sign in without token if token request fails
            }
        }

        GoogleSignIn.getClient(context, builder.build())
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            if (data == null) {
                return@withContext Result.failure(IllegalArgumentException("No Google sign-in response data received."))
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            val auth = FirebaseAuth.getInstance()
            val idToken = account.idToken

            if (!idToken.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val user = UserData(
                        userId = firebaseUser.uid,
                        username = firebaseUser.email?.substringBefore("@") ?: "user_${firebaseUser.uid.take(6)}",
                        displayName = firebaseUser.displayName ?: account.displayName ?: "Google User",
                        bio = "HundredGram Creator ✨",
                        avatarUrl = firebaseUser.photoUrl?.toString() ?: account.photoUrl?.toString() ?: "",
                        email = firebaseUser.email ?: account.email ?: "ssir6921@gmail.com",
                        followersCount = 0,
                        followingCount = 0,
                        postsCount = 0,
                        isVerified = true
                    )
                    return@withContext Result.success(user)
                }
            }

            // Direct Google Account mapping
            val email = account.email ?: "ssir6921@gmail.com"
            val user = UserData(
                userId = account.id ?: "g_${System.currentTimeMillis() % 100000}",
                username = email.substringBefore("@").lowercase(),
                displayName = account.displayName ?: "Google User",
                bio = "Verified Google Creator ✨",
                avatarUrl = account.photoUrl?.toString() ?: "",
                email = email,
                followersCount = 0,
                followingCount = 0,
                postsCount = 0,
                isVerified = true
            )
            Result.success(user)
        } catch (e: Exception) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null) {
                val emailVal = lastAccount.email ?: "ssir6921@gmail.com"
                val user = UserData(
                    userId = lastAccount.id ?: "g_${System.currentTimeMillis() % 100000}",
                    username = emailVal.substringBefore("@").lowercase(),
                    displayName = lastAccount.displayName ?: "Google Creator",
                    bio = "Verified Google Creator ✨",
                    avatarUrl = lastAccount.photoUrl?.toString() ?: "",
                    email = emailVal,
                    followersCount = 0,
                    followingCount = 0,
                    postsCount = 0,
                    isVerified = true
                )
                Result.success(user)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun signInWithGoogle(): Result<UserData> = withContext(Dispatchers.IO) {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            val user = UserData(
                userId = lastAccount.id ?: "g_${System.currentTimeMillis() % 100000}",
                username = (lastAccount.email?.substringBefore("@") ?: "google_user").lowercase(),
                displayName = lastAccount.displayName ?: "Google Member",
                bio = "Verified Google Member ✨",
                avatarUrl = lastAccount.photoUrl?.toString() ?: "",
                followersCount = 0,
                followingCount = 0,
                postsCount = 0,
                isVerified = true
            )
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("No Google account currently selected."))
        }
    }
}
