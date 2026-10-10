package com.example.ui.ads

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(nativeAd: NativeAd) {
    AndroidView(
        factory = { context ->
            val nativeAdView = NativeAdView(context).apply {
                // LayoutParams for the NativeAdView
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Create container for ad components
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }
            nativeAdView.addView(container)

            // MediaView
            val mediaView = MediaView(context)
            container.addView(mediaView)
            nativeAdView.mediaView = mediaView

            // Headline
            val headlineView = TextView(context).apply {
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(headlineView)
            nativeAdView.headlineView = headlineView

            // Call to Action
            val callToActionView = android.widget.Button(context)
            container.addView(callToActionView)
            nativeAdView.callToActionView = callToActionView

            nativeAdView
        },
        update = { nativeAdView ->
            (nativeAdView.headlineView as TextView).text = nativeAd.headline
            (nativeAdView.callToActionView as android.widget.Button).text = nativeAd.callToAction
            
            // Link the native ad object to the view
            nativeAdView.setNativeAd(nativeAd)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E2433))
    )
}
