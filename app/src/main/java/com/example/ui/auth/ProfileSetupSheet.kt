package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserData
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupSheet(
    viewModel: MainViewModel,
    user: UserData,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    // States
    var username by remember { mutableStateOf(user.username.ifBlank { "" }) }
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde") }
    var termsAccepted by remember { mutableStateOf(false) }
    
    // Username uniqueness checking state
    val takenUsernames = listOf("admin", "test", "amit_kum", "priya_sharma", "rohan_verma", "tech_guru")
    val isUsernameTaken = username.lowercase().trim() in takenUsernames
    val isUsernameValid = username.length >= 3 && !isUsernameTaken

    ModalBottomSheet(
        onDismissRequest = {},
        containerColor = HundredGramCardBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = HundredGramDivider) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (step == i) 32.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (step == i) HundredGramPink else HundredGramDivider)
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ProfileSetupSteps"
            ) { targetStep ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (targetStep) {
                        1 -> {
                            Text(
                                text = "Create a Unique Username\n(एक अनोखा यूज़रनेम बनाएं)",
                                color = HundredGramTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This helps people search and connect with you uniquely.\n(यह लोगों को खोजने में मदद करेगा।)",
                                color = HundredGramTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it.replace(" ", "_").lowercase() },
                                label = { Text("Username") },
                                leadingIcon = { Text("@", color = HundredGramPink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HundredGramTextPrimary,
                                    unfocusedTextColor = HundredGramTextPrimary,
                                    focusedBorderColor = if (isUsernameTaken) Color.Red else HundredGramPink,
                                    unfocusedBorderColor = HundredGramDivider
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            if (username.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isUsernameTaken) "❌ This username is already taken. (यह यूज़रनेम पहले से लिया जा चुका है)"
                                           else if (username.length < 3) "⚠️ Username must be at least 3 characters."
                                           else "✅ Username is available! (यूज़रनेम उपलब्ध है)",
                                    color = if (isUsernameValid) Color.Green else Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { step = 2 },
                                enabled = isUsernameValid,
                                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Next (अगला)", fontWeight = FontWeight.Bold)
                            }
                        }
                        2 -> {
                            Text(
                                text = "What is your full name?\n(आपका पूरा नाम क्या है?)",
                                color = HundredGramTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HundredGramTextPrimary,
                                    unfocusedTextColor = HundredGramTextPrimary,
                                    focusedBorderColor = HundredGramPink,
                                    unfocusedBorderColor = HundredGramDivider
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { step = 1 },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HundredGramTextPrimary)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = { step = 3 },
                                    enabled = fullName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                        3 -> {
                            Text(
                                text = "Gender & Date of Birth\n(जेंडर और जन्म तिथि)",
                                color = HundredGramTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Gender Input
                            OutlinedTextField(
                                value = gender,
                                onValueChange = { gender = it },
                                label = { Text("Gender (e.g. Male/Female/Other)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HundredGramTextPrimary,
                                    unfocusedTextColor = HundredGramTextPrimary,
                                    focusedBorderColor = HundredGramPink,
                                    unfocusedBorderColor = HundredGramDivider
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // DOB Input
                            OutlinedTextField(
                                value = dob,
                                onValueChange = { dob = it },
                                label = { Text("Date of Birth (DD/MM/YYYY)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HundredGramTextPrimary,
                                    unfocusedTextColor = HundredGramTextPrimary,
                                    focusedBorderColor = HundredGramPink,
                                    unfocusedBorderColor = HundredGramDivider
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { step = 2 },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HundredGramTextPrimary)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = { step = 4 },
                                    enabled = gender.isNotBlank() && dob.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                        4 -> {
                            Text(
                                text = "Accept Terms & Privacy\n(नीति और गोपनीयता)",
                                color = HundredGramTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Choose an avatar pre-selection
                            Text("Select Avatar (अवतार चुनें)", color = HundredGramTextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                val avatars = listOf(
                                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde",
                                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                                    "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61",
                                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d"
                                )
                                avatars.forEach { url ->
                                    val isSelected = avatarUrl == url
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) HundredGramPink else Color.Transparent)
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .clickable { avatarUrl = url }
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(model = url),
                                            contentDescription = "Avatar Selection",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HundredGramCardElevated)
                                    .padding(12.dp)
                            ) {
                                Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { termsAccepted = it },
                                    colors = CheckboxDefaults.colors(checkedColor = HundredGramPink)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "मैं हमारी नीति और गोपनीयता शर्तों से सहमत हूँ (Agree to policy)",
                                    color = HundredGramTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { step = 3 },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HundredGramTextPrimary)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = {
                                        viewModel.completeProfileSetup(
                                            user.copy(
                                                username = username.trim(),
                                                fullName = fullName.trim(),
                                                gender = gender.trim(),
                                                dob = dob.trim(),
                                                avatarUrl = avatarUrl,
                                                isTermsAccepted = termsAccepted
                                            )
                                        )
                                        onDismiss()
                                    },
                                    enabled = termsAccepted,
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Submit (दर्ज करें)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

