package com.example.networkscanner.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkInfo(
    val ssid: String,
    val gateway: String,
    val localIp: String,
    val externalIp: String,
    val subnetRange: String,
    val isWifiConnected: Boolean
)

@Singleton
class NetworkInfoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var cachedExternalIp: String = "Fetching..."

    fun observeNetworkInfo(): Flow<NetworkInfo> = flow {
        while (true) {
            if (cachedExternalIp == "Fetching..." || cachedExternalIp == "Error") {
                fetchExternalIp()
            }
            val info = fetchCurrentNetworkInfo()
            emit(info)
            delay(10000) // Refresh every 10 seconds
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchExternalIp() {
        cachedExternalIp = withContext(Dispatchers.IO) {
            try {
                // Using a reliable IP check service
                URL("https://api.ipify.org").readText()
            } catch (e: Exception) {
                "Error"
            }
        }
    }

    private fun fetchCurrentNetworkInfo(): NetworkInfo {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        
        val ssid = if (isWifi) {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        } else "Not Connected"

        val linkProperties = connectivityManager.getLinkProperties(network)
        val localIp = linkProperties?.linkAddresses?.find { it.address is Inet4Address }?.address?.hostAddress ?: "Unknown"
        val gateway = linkProperties?.routes?.find { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress ?: "Unknown"
        val prefix = linkProperties?.linkAddresses?.find { it.address is Inet4Address }?.prefixLength ?: 0
        val subnetRange = if (localIp != "Unknown") "$localIp/$prefix" else "Unknown"

        return NetworkInfo(
            ssid = if (ssid == "<unknown ssid>") "Connected" else ssid,
            gateway = gateway,
            localIp = localIp,
            externalIp = cachedExternalIp,
            subnetRange = subnetRange,
            isWifiConnected = isWifi
        )
    }
}
