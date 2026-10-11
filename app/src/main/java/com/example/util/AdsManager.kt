package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Unified AdMob Manager for HundredGram
 * Exclusively uses verified Real Ad IDs:
 * - App ID: ca-app-pub-6058721301027431~6693520916
 * - Post Ad: ca-app-pub-6058721301027431/8718806610 (Shown after every 2 posts)
 * - Reel Ad: ca-app-pub-6058721301027431/1195539816 (Shown after every 2 reels)
 */
object AdsManager {
    private const val TAG = "AdsManager"

    // Real AdMob IDs
    const val APP_ID = "ca-app-pub-6058721301027431~6693520916"
    const val FEED_NATIVE_AD_ID = "ca-app-pub-6058721301027431/8718806610"
    const val REEL_AD_ID = "ca-app-pub-6058721301027431/1195539816"

    private var isInitialized = false
    private var cachedReelInterstitial: InterstitialAd? = null

    /**
     * Initializes AdMob MobileAds SDK safely
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Disable heavy mediation adapter scanning to prevent service binding errors
            MobileAds.disableMediationAdapterInitialization(context)

            Thread {
                try {
                    MobileAds.initialize(context) { status ->
                        Log.d(TAG, "AdMob MobileAds initialized successfully: $status")
                        isInitialized = true
                        // Preload interstitial video ad for reels using real Reel Ad ID
                        preloadReelVideoAd(context.applicationContext)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MobileAds initialize background exception: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            Log.w(TAG, "MobileAds initialization skipped: ${e.message}")
        }
    }

    /**
     * Loads a Native Ad for Post Feed (shown after every 2 posts)
     * Exclusively uses real FEED_NATIVE_AD_ID.
     */
    fun loadNativeFeedAd(
        context: Context,
        adUnitId: String = FEED_NATIVE_AD_ID,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: (LoadAdError) -> Unit
    ) {
        try {
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "Native feed ad loaded successfully from unit $adUnitId.")
                    onAdLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native feed ad unit $adUnitId failed to load: ${error.message}")
                        onAdFailed(error)
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.w(TAG, "Exception loading native feed ad: ${e.message}")
        }
    }

    /**
     * Preloads Interstitial Video Ad for Reels (shown after every 2 reels)
     * Exclusively uses real REEL_AD_ID.
     */
    fun preloadReelVideoAd(context: Context, adUnitId: String = REEL_AD_ID) {
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Reel Interstitial Video Ad loaded ($adUnitId).")
                        cachedReelInterstitial = interstitialAd
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Reel Video Ad failed to load ($adUnitId): ${loadAdError.message}")
                        cachedReelInterstitial = null
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Exception preloading reel video ad: ${e.message}")
        }
    }

    /**
     * Shows Real Video Ad after every 2 Reels
     */
    fun showReelVideoAd(
        activity: Activity,
        onDismissed: () -> Unit
    ) {
        val ad = cachedReelInterstitial
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Reel ad dismissed by user.")
                    cachedReelInterstitial = null
                    preloadReelVideoAd(activity.applicationContext)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Reel ad failed to show: ${adError.message}")
                    cachedReelInterstitial = null
                    preloadReelVideoAd(activity.applicationContext)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            // Not ready yet: smoothly continue without interruption and retry preloading
            preloadReelVideoAd(activity.applicationContext)
            onDismissed()
        }
    }
}
