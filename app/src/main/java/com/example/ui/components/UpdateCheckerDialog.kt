package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

@Composable
fun UpdateCheckerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onUpdateConfirm: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00CEC9),
                    modifier = Modifier.size(20.dp)
                )
                Text("System & Update Compatibility", color = HundredGramTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HundredGram v2.4 (Build 240) is optimized for your hardware. Check system compatibility details below:",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SYSTEM COMPATIBILITY & SPECS",
                    color = HundredGramTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Compatibility Badge Row (Architecture, Memory, OS, Hardware acceleration)
                SystemCompatibilityBadgeRow(
                    modifier = Modifier.fillMaxWidth(),
                    scrollable = true,
                    showDetailedRequirements = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HundredGramCardElevated)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "• Architecture: ARM64-v8a / x86_64 64-bit optimized",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "• Memory Rec: 4 GB RAM (Min 2 GB) / 256 MB Heap",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "• Video Transcode: Hardware HEVC/H.264 Accelerated",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink)
            ) {
                Text("Download Latest APK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}

