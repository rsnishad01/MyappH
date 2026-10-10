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
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Unified AdMob Manager for HundredGram
 * Handles Native Feed Ads, Video/Interstitial Reel Ads, and safe fallbacks.
 */
object AdsManager {
    private const val TAG = "AdsManager"

    // Ad IDs configured per user AdMob specification
    const val APP_ID = "ca-app-pub-6058721301027431~6693520916"
    const val FEED_NATIVE_AD_ID = "ca-app-pub-6058721301027431/8718806610"
    const val REEL_AD_ID = "ca-app-pub-6058721301027431/1195539816"

    // Official Google AdMob sample test ad units for sandbox/emulator validation
    private const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REEL_AD_ID = "ca-app-pub-3940256099942544/8691691433"

    private var isInitialized = false
    private var cachedReelInterstitial: InterstitialAd? = null

    /**
     * Initializes AdMob SDK safely
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Disable heavy mediation adapter scanning to prevent service binding errors
            MobileAds.disableMediationAdapterInitialization(context)

            // Configure test device for emulator environments
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            Thread {
                try {
                    MobileAds.initialize(context) { status ->
                        Log.d(TAG, "AdMob MobileAds initialized successfully: $status")
                        isInitialized = true
                        // Preload an interstitial video ad for reels
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
     * Loads a Native Ad for Post Feed (Every 4 posts)
     * First attempts with user's live AdMob ID, and falls back to Google test ad unit if live unit has no fill yet.
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
                        if (adUnitId != TEST_NATIVE_AD_ID) {
                            Log.d(TAG, "Attempting fallback to test native ad unit...")
                            loadNativeFeedAd(context, TEST_NATIVE_AD_ID, onAdLoaded, onAdFailed)
                        } else {
                            onAdFailed(error)
                        }
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
     * Preloads Interstitial Video Ad for Reels
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
                        Log.w(TAG, "Reel Video Ad failed to preload ($adUnitId): ${loadAdError.message}")
                        if (adUnitId != TEST_REEL_AD_ID) {
                            Log.d(TAG, "Attempting fallback preload with test reel ad unit...")
                            preloadReelVideoAd(context, TEST_REEL_AD_ID)
                        } else {
                            cachedReelInterstitial = null
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Exception preloading reel video ad: ${e.message}")
        }
    }

    /**
     * Shows Video Ad after every 3 Reels
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
            // Not ready yet or failed to load: continue reel playback smoothly without interruption
            preloadReelVideoAd(activity.applicationContext)
            onDismissed()
        }
    }
}
