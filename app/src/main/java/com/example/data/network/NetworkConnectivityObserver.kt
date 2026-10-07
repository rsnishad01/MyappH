package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
    Available,   // Good / Connected (Green Dot)
    Weak,        // Weak / Slow (Yellow Dot)
    Unavailable, // No Connection (Red Dot)
    Losing,      // Losing Connection (Yellow Dot)
    Lost         // Lost Connection (Red Dot)
}

class NetworkConnectivityObserver(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun evaluateCapabilities(capabilities: NetworkCapabilities?): NetworkStatus {
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkStatus.Unavailable
        }
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val bandwidthKbps = capabilities.linkDownstreamBandwidthKbps
        
        // Low bandwidth or unvalidated connection indicates weak/poor connectivity
        val isLowBandwidth = bandwidthKbps in 1..2000
        val isWeakSignal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val signal = capabilities.signalStrength
            signal != Int.MIN_VALUE && signal < -85
        } else false

        return when {
            !isValidated || isLowBandwidth || isWeakSignal -> NetworkStatus.Weak
            else -> NetworkStatus.Available
        }
    }

    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val caps = connectivityManager.getNetworkCapabilities(network)
                trySend(evaluateCapabilities(caps))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                trySend(evaluateCapabilities(networkCapabilities))
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(NetworkStatus.Losing)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(NetworkStatus.Lost)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(NetworkStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}

        val activeNetwork = connectivityManager.activeNetwork
        val currentCaps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        trySend(evaluateCapabilities(currentCaps))

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
