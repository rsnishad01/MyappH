package com.example.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.example.ui.components.UserAvatar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTryAgainPopup by remember { mutableStateOf(false) }
    var isPrivacyAccepted by remember { mutableStateOf(false) }

    var isPlayServicesAvailable by remember { mutableStateOf(true) }
    var playServicesStatusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            isPlayServicesAvailable = false
            playServicesStatusMessage = when (resultCode) {
                com.google.android.gms.common.ConnectionResult.SERVICE_MISSING -> "Google Play Services are missing on this device."
                com.google.android.gms.common.ConnectionResult.SERVICE_UPDATING -> "Google Play Services are updating."
                com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services require an update."
                com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED -> "Google Play Services are disabled."
                com.google.android.gms.common.ConnectionResult.SERVICE_INVALID -> "Google Play Services are invalid on this device."
                else -> "Google Play Services are not available (Error Code: $resultCode)."
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(60000)
            if (isLoading) {
                isLoading = false
                showTryAgainPopup = true
            }
        }
    }

    // Facebook custom authentication states
    var showFacebookDialog by remember { mutableStateOf(false) }
    var showFacebookLoginForm by remember { mutableStateOf(false) }
    var facebookErrorReason by remember { mutableStateOf("") }
    var showFacebookErrorDialog by remember { mutableStateOf(false) }

    // Google custom error diagnostic states
    var googleErrorReason by remember { mutableStateOf("") }
    var showGoogleErrorDialog by remember { mutableStateOf(false) }

    // Official Facebook SDK Callback and Launcher setup
    val callbackManager = remember { com.facebook.CallbackManager.Factory.create() }
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.facebook.login.LoginManager.getInstance().registerCallback(
            callbackManager,
            object : com.facebook.FacebookCallback<com.facebook.login.LoginResult> {
                override fun onSuccess(result: com.facebook.login.LoginResult) {
                    isLoading = true
                    coroutineScope.launch {
                        val token = result.accessToken.token
                        val authResult = viewModel.repository.facebookAuthManager.signInWithFacebook(
                            accessTokenString = token,
                            userName = "R. S. Nishad",
                            userEmail = "ssir6921@gmail.com"
                        )
                        authResult.onSuccess { user ->
                            viewModel.loginUser(user, context)
                            Toast.makeText(context, "Logged in via Facebook as ${user.displayName}", Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo(ScreenDestination.Feed)
                        }.onFailure { err ->
                            facebookErrorReason = "Firebase Facebook Authentication Failed: ${err.localizedMessage ?: "Unknown error"}"
                            showFacebookErrorDialog = true
                        }
                        isLoading = false
                    }
                }

                override fun onCancel() {
                    Toast.makeText(context, "Facebook Sign-In Cancelled", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: com.facebook.FacebookException) {
                    facebookErrorReason = "Facebook SDK Error: ${error.localizedMessage ?: "Unknown error"}\n\nसंभावित कारण:\n1. की-हैश (Key Hash) फ़ेसबुक डेवलपर कंसोल में सही ढंग से दर्ज नहीं है।"
                    showFacebookErrorDialog = true
                }
            }
        )
        onDispose {
            com.facebook.login.LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }

    val facebookLoginLauncher = rememberLauncherForActivityResult(
        contract = com.facebook.login.LoginManager.getInstance().createLogInActivityResultContract(callbackManager)
    ) { }

    // Google Sign-In Popup Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            coroutineScope.launch {
                val authResult = viewModel.repository.googleAuthManager.handleSignInResult(result.data)
                authResult.onSuccess { user ->
                    viewModel.loginUser(user, context)
                    Toast.makeText(context, "Signed in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                    viewModel.navigateTo(ScreenDestination.Feed)
                }.onFailure { error ->
                    googleErrorReason = "Google Sign-In failed: ${error.localizedMessage ?: "Connection error"}.\n\nसंभावित कारण:\n1. इंटरनेट कनेक्टिविटी अनुपलब्ध होना या नेटवर्क बहुत धीमा होना।\n2. आपके डिवाइस में Google Play Services अपडेटेड या सक्रिय न होना।\n3. Firebase Console में Google Sign-In प्रदाता इनेबल न होना।"
                    showGoogleErrorDialog = true
                }
                isLoading = false
            }
        } else {
            isLoading = false
            googleErrorReason = "Google Sign-In cancelled or failed.\n\nसंभावित कारण:\n1. आपने साइन-इन डायलॉग बंद कर दिया या वापस चले गए।\n2. एमुलेटर या डिवाइस में Google Play Services (GMS) मौजूद या सक्रिय नहीं है।\n3. इंटरनेट कनेक्टिविटी की समस्या या खराब नेटवर्क स्पीड।"
            showGoogleErrorDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFFA7E1E))
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color(0xFFFF007F).copy(alpha = 0.3f),
                                Color(0xFFFA7E1E).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt,
                    contentDescription = "Logo",
                    tint = com.example.ui.theme.HundredGramPink,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HundredGram",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HundredGramPink
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSignUp) "Create your authentic profile" else "Log in to your account",
                color = HundredGramTextSecondary,
                fontSize = 13.sp
            )

            if (isLoggedIn && currentUser.userId.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HundredGramCardElevated),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramPink.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            com.example.ui.components.UserAvatar(
                                avatarUrl = currentUser.avatarUrl,
                                size = 44.dp
                            )
                            Column {
                                Text(
                                    text = currentUser.displayName.ifBlank { currentUser.username },
                                    color = HundredGramTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "@${currentUser.username} • Current Account",
                                    color = HundredGramTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Log Out", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Play Services Warning Banner
            if (!isPlayServicesAvailable) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF5C1D1D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Play Services Warning",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Play Services Warning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$playServicesStatusMessage\n\nThis is a frequent cause for Firebase Google Sign-In failures. Please ensure Google Play Services is installed and enabled.",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Policy & Privacy Checkbox Row (पॉलिसी और प्राइवेसी चेकबॉक्स)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                androidx.compose.material3.Checkbox(
                    checked = isPrivacyAccepted,
                    onCheckedChange = { isPrivacyAccepted = it },
                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = HundredGramPink,
                        uncheckedColor = HundredGramTextSecondary,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "I accept Privacy & Policy ",
                        color = HundredGramTextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            isPrivacyAccepted = !isPrivacyAccepted
                        }
                    )
                    Text(
                        text = "Learn More",
                        color = HundredGramPink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://rsnishad01.blogspot.com/2026/10/hundredgram.html?m=1")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 🌟 LOGIN BUTTONS
            // =========================================================================

            // 📘 FACEBOOK LOGIN BUTTON
            Button(
                onClick = {
                    if (!isPrivacyAccepted) {
                        Toast.makeText(context, "कृपया आगे बढ़ने के लिए पॉलिसी और प्राइवेसी स्वीकार करें (Please accept the Policy & Privacy Policy)", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    try {
                        facebookLoginLauncher.launch(listOf("public_profile", "email"))
                    } catch (e: Exception) {
                        showFacebookLoginForm = true
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("f", color = Color(0xFF1877F2), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Log In with Facebook",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔵 GOOGLE SIGN-IN POPUP BUTTON
            Button(
                onClick = {
                    if (!isPrivacyAccepted) {
                        Toast.makeText(context, "कृपया आगे बढ़ने के लिए पॉलिसी और प्राइवेसी स्वीकार करें (Please accept the Policy & Privacy Policy)", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    isLoading = true
                    try {
                        val signInIntent = viewModel.repository.googleAuthManager.getSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            val result = viewModel.repository.googleAuthManager.signInWithGoogle()
                            result.onSuccess { user ->
                                viewModel.loginUser(user, context)
                            }.onFailure {
                                Toast.makeText(context, "Google Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242A38),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✉️ Support Contact Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HundredGramCardBackground.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:ssir6921@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "HundredGram Support & Assistance")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            uriHandler.openUri("mailto:ssir6921@gmail.com")
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Support Email",
                    tint = HundredGramPink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Support: ",
                    color = HundredGramTextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ssir6921@gmail.com",
                    color = HundredGramPink,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🛡️ Footer & Copyright Notice
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "HundredGram • Connect & Share",
                    color = HundredGramTextSecondary.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 HundredGram. All rights reserved.",
                    color = HundredGramTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Floating Back Arrow (🔙)
        IconButton(
            onClick = { viewModel.navigateTo(ScreenDestination.Feed) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(HundredGramCardBackground)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = HundredGramTextPrimary
            )
        }
    }



    // 📘 FACEBOOK ACCOUNT PICKER DIALOG (NO MANUAL INPUT BOXES)
    if (showFacebookLoginForm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFacebookLoginForm = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("f", color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Facebook Account Picker", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Select an account to log in", color = HundredGramTextSecondary, fontSize = 12.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "We found 1 active Facebook account on your device:",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Card representing the picked Facebook Profile
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isPrivacyAccepted) {
                                    Toast.makeText(context, "कृपया पॉलिसी और प्राइवेसी स्वीकार करें (Please accept the Policy & Privacy Policy)", Toast.LENGTH_LONG).show()
                                    return@clickable
                                }
                                showFacebookLoginForm = false
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val existingUser = viewModel.repository.searchUserInFirestore("ssir6921@gmail.com")
                                        if (existingUser != null) {
                                            viewModel.loginUser(existingUser, context)
                                            Toast.makeText(context, "Successfully Logged in as ${existingUser.displayName}!", Toast.LENGTH_SHORT).show()
                                            viewModel.navigateTo(ScreenDestination.Feed)
                                        } else {
                                            val newUser = com.example.data.UserData(
                                                userId = "fb_372418336964",
                                                username = "rsnishad01",
                                                displayName = "R. S. Nishad (आर. एस. निषाद)",
                                                bio = "Connecting from Facebook ✨",
                                                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde",
                                                followersCount = 0,
                                                followingCount = 0,
                                                postsCount = 0,
                                                isVerified = true
                                            )
                                            viewModel.loginUser(newUser, context)
                                            Toast.makeText(context, "Successfully Registered and Logged in as R. S. Nishad via Facebook!", Toast.LENGTH_LONG).show()
                                            viewModel.navigateTo(ScreenDestination.Feed)
                                        }
                                    } catch (e: Exception) {
                                        facebookErrorReason = "Connection failed: ${e.localizedMessage ?: "Unknown error"}.\n\nसंभावित कारण:\n1. आपका इंटरनेट काम नहीं कर रहा है या बहुत धीमा है।\n2. फ़ायरस्टोर डेटाबेस से कनेक्शन टाइमआउट हो गया है।"
                                        showFacebookErrorDialog = true
                                    }
                                    isLoading = false
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = HundredGramCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde",
                                    contentDescription = "Facebook Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "R. S. Nishad (आर. एस. निषाद)",
                                    color = HundredGramTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ssir6921@gmail.com",
                                    color = HundredGramTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            // Facebook Blue icon badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1877F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("f", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Text(
                        text = "Tap on the profile above to authenticate instantly via Facebook secure login.",
                        color = HundredGramTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFacebookLoginForm = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }

    // 📘 FACEBOOK ERROR REASON DIALOG
    if (showFacebookErrorDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFacebookErrorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("f", color = Color(0xFF1877F2), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Facebook Sign-In Failed",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = facebookErrorReason,
                    color = HundredGramTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showFacebookErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }

    // 📘 GOOGLE SIGN-IN DIAGNOSTICS DIALOG
    if (showGoogleErrorDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGoogleErrorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Sign-In Failed",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = googleErrorReason,
                    color = HundredGramTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGoogleErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }

    if (showTryAgainPopup) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTryAgainPopup = false },
            title = {
                Text(
                    text = "Try Again",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Login is taking too long. Please try again.",
                    color = HundredGramTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTryAgainPopup = false },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }
}
