package com.example.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserData
import com.example.ui.MainViewModel
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

@Composable
fun DeleteAccountConfirmationDialog(
    currentUser: UserData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var confirmationChecked by remember { mutableStateOf(false) }
    var reasonText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete / Deactivate Account\n(खाता डिलीट करें)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Warning Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E1719), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚠️ खाता निष्क्रियता व 15 दिन नियम:",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "1. पुष्टि करने पर आपका खाता तुरंत निष्क्रय (Deactivated) हो जाएगा।\n" +
                                    "2. 15 दिनों का Grace Period रहेगा। यदि आप 15 दिनों के भीतर पुनः लॉगिन करते हैं, तो आपका खाता पुनः चालू (Reactivate) हो जाएगा।\n" +
                                    "3. यदि आप 15 दिनों के भीतर लॉगिन नहीं करते हैं, तो आपका खाता एवं संपूर्ण डेटा (पोस्ट, रील, चैट) हमेशा के लिए डिलीट कर दिया जाएगा।\n" +
                                    "4. इसकी पुष्टि आपके ईमेल पर भेजी जा रही है।",
                            color = HundredGramTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                val fbEmail = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email } catch (_: Exception) { null }
                val displayEmail = currentUser.email.ifBlank { fbEmail.orEmpty() }.ifBlank { "ssir6921@gmail.com" }

                // Email Notification Info Box
                OutlinedTextField(
                    value = displayEmail,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("पंजीकृत ईमेल (Notification Email)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextSecondary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("खाता हटाने का कारण (वैकल्पिक)") },
                    placeholder = { Text("उदा. व्यक्तिगत कारण, अन्य खाता आदि") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Confirmation Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = confirmationChecked,
                        onCheckedChange = { confirmationChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFF5252),
                            uncheckedColor = HundredGramTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "मैं समझता/समझती हूँ कि 15 दिनों में लॉगिन न करने पर मेरा खाता permanently delete हो जाएगा।",
                        color = HundredGramTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!confirmationChecked) {
                        Toast.makeText(context, "कृपया पुष्टि करने के लिए चेकबॉक्स पर टिक करें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    val msg = viewModel.deactivateAccount(context, reasonText.trim())
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                enabled = confirmationChecked && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Deactivate Account (खाता हटाएं)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel (रद्द करें)", color = HundredGramTextSecondary, fontSize = 13.sp)
            }
        }
    )
}
