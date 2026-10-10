package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.UserData
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.InstagramStoryGradient
import com.example.ui.theme.HundredGramOrange
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramPurple
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import com.example.util.QrCodeDecoder
import com.example.util.QrCodeGenerator
import com.example.util.ShareManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileShareAndQrSheet(
    user: UserData,
    viewModel: MainViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    // Generates profile payload format: hundredgram://profile/<userId>
    val profileQrUrl = remember(user.userId, user.username) {
        "hundredgram://profile/${user.userId}"
    }

    val qrBitmap = remember(profileQrUrl) {
        QrCodeGenerator.generateQrCode(profileQrUrl, size = 512)
    }

    // Helper to parse scanned QR content and navigate to matching profile
    fun handleScannedContent(rawContent: String) {
        val trimmed = rawContent.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, "No QR Code detected (कोई कोड नहीं मिला)", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse patterns:
        // 1. hundredgram://profile/<id_or_username>
        // 2. https://hundredgram.app/u/<username>
        // 3. https://hundredgram.app/profile/<id>
        // 4. raw userId or @username
        val targetIdOrUsername = when {
            trimmed.startsWith("hundredgram://profile/", ignoreCase = true) -> {
                trimmed.substringAfter("hundredgram://profile/").trim()
            }
            trimmed.startsWith("https://hundredgram.app/u/", ignoreCase = true) -> {
                trimmed.substringAfter("https://hundredgram.app/u/").substringBefore("/").substringBefore("?").trim()
            }
            trimmed.startsWith("https://hundredgram.app/profile/", ignoreCase = true) -> {
                trimmed.substringAfter("https://hundredgram.app/profile/").substringBefore("/").substringBefore("?").trim()
            }
            trimmed.startsWith("hundredgram://", ignoreCase = true) -> {
                trimmed.substringAfter("hundredgram://").substringBefore("/").trim()
            }
            trimmed.startsWith("@") -> {
                trimmed.removePrefix("@").trim()
            }
            else -> trimmed
        }

        if (targetIdOrUsername.isNotBlank()) {
            Toast.makeText(context, "Profile found: $targetIdOrUsername! Opening...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
                viewModel.navigateTo(ScreenDestination.CreatorProfile(targetIdOrUsername))
            }
        } else {
            Toast.makeText(context, "Invalid QR code format: $trimmed", Toast.LENGTH_LONG).show()
        }
    }

    // ZXing Fullscreen Scan Launcher
    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val scanned = result.contents
        if (scanned != null) {
            handleScannedContent(scanned)
        } else {
            Toast.makeText(context, "Scan cancelled (स्कैन रद्द किया)", Toast.LENGTH_SHORT).show()
        }
    }

    fun startQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a 100Gram Profile QR Code\n(प्रोफाइल QR कोड स्कैन करें)")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(com.example.util.PortraitCaptureActivity::class.java)
        }
        qrScanLauncher.launch(options)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrScanner()
        } else {
            Toast.makeText(
                context,
                "Camera permission required to scan QR code (कैमरा अनुमति आवश्यक है)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestCameraAndScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isDecodingFromGallery by remember { mutableStateOf(false) }

    val galleryQrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingFromGallery = true
            coroutineScope.launch(Dispatchers.IO) {
                val decodedText = QrCodeDecoder.decodeQrFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    isDecodingFromGallery = false
                    if (!decodedText.isNullOrBlank()) {
                        Toast.makeText(context, "QR Code scanned from gallery!", Toast.LENGTH_SHORT).show()
                        handleScannedContent(decodedText)
                    } else {
                        Toast.makeText(
                            context,
                            "No QR code found in selected image (चुनी गई इमेज में QR कोड नहीं मिला)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun shareQrCodeImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(context, "QR code not ready", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "profile_qr_${user.username}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Connect with @${user.username} on 100Gram!\nhttps://hundredgram.app/u/${user.username}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code via"))
        } catch (e: Exception) {
            ShareManager.shareProfile(context, user)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Share & QR Code (शेयर व QR)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HundredGramTextPrimary
                )
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = HundredGramTextSecondary
                    )
                }
            }

            // Tabs: 0: My QR Code, 1: Scan QR Code, 2: Share Links
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = HundredGramPink,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = HundredGramPink,
                            height = 3.dp
                        )
                    }
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("My QR", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = HundredGramPink,
                    unselectedContentColor = HundredGramTextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan QR", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = HundredGramPink,
                    unselectedContentColor = HundredGramTextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Link", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = HundredGramPink,
                    unselectedContentColor = HundredGramTextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: My QR Code Generator View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Instagram-style QR Card with Brand Gradient Border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            HundredGramPurple.copy(alpha = 0.25f),
                                            HundredGramPink.copy(alpha = 0.25f),
                                            HundredGramOrange.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(HundredGramPurple, HundredGramPink, HundredGramOrange)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // User Avatar and Names
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (user.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, HundredGramPink, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(InstagramStoryGradient),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.username.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        val displayName = user.displayName.ifBlank { user.fullName.ifBlank { user.username } }
                                        Text(
                                            text = displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = HundredGramTextPrimary
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 13.sp,
                                            color = HundredGramTextSecondary
                                        )
                                    }
                                }

                                // High-contrast White Card holding the sharp QR Code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White)
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "User QR Code",
                                            modifier = Modifier.size(200.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Failed to generate QR Code",
                                            color = Color.Red,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Scan this QR code to visit @${user.username}'s profile",
                                    fontSize = 12.sp,
                                    color = HundredGramTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Action Buttons: Share QR Image, Copy Link, Open Camera Scanner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { shareQrCodeImage(qrBitmap) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    ShareManager.copyToClipboard(
                                        context,
                                        "https://hundredgram.app/u/${user.username}",
                                        "Profile Link",
                                        "Profile link copied! (लिंक कॉपी हुआ)"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HundredGramCardElevated),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = HundredGramTextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Link", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = HundredGramPink, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Scan Someone Else's QR Code (दूसरों का QR स्कैन करें)",
                                color = HundredGramPink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: Scan Someone Else's QR Code
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(HundredGramCardElevated)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(HundredGramPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = HundredGramPink,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Text(
                                    text = "Scan Any Profile QR Code\n(किसी दूसरे का QR कोड स्कैन करें)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HundredGramTextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Point your camera at a friend's 100Gram profile QR code to instantly open and follow their profile.",
                                    fontSize = 13.sp,
                                    color = HundredGramTextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { requestCameraAndScan() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Open Camera Scanner (कैमरा खोलें)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryQrPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isDecodingFromGallery,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = HundredGramTextPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isDecodingFromGallery) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = HundredGramPink,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Scanning Image... (इमेज पढ़ रहे हैं...)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = HundredGramTextPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = HundredGramPink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Upload from Gallery (गैलरी से QR चुनें)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = HundredGramTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Supports 100Gram QR codes, usernames, and direct profile links.",
                            fontSize = 12.sp,
                            color = HundredGramTextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                2 -> {
                    // TAB 2: Quick Share Profile Links & Apps
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val profileLink = "https://hundredgram.app/u/${user.username}"

                        // Profile Link Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HundredGramCardElevated)
                                .border(1.dp, HundredGramBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Profile URL", fontSize = 11.sp, color = HundredGramTextSecondary)
                                    Text(
                                        profileLink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HundredGramTextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        ShareManager.copyToClipboard(
                                            context,
                                            profileLink,
                                            "Profile Link",
                                            "Profile link copied! (लिंक कॉपी हुआ)"
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = HundredGramPink
                                    )
                                }
                            }
                        }

                        // Full System Share Button
                        Button(
                            onClick = {
                                ShareManager.shareProfile(context, user)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Profile via Apps (ऐप्स में शेयर करें)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Share QR Image Option
                        Button(
                            onClick = { shareQrCodeImage(qrBitmap) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramCardElevated),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = HundredGramTextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share QR Code Image (QR फोटो शेयर करें)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
