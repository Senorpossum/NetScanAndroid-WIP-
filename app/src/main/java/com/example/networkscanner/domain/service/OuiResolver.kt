package com.example.networkscanner.domain.service

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

class OuiResolver(private val context: Context) {

    // LRU Cache for instant O(1) lookups of frequently seen vendors across scans
    private val memoryCache = LruCache<String, String>(100)
    
    // In-memory map simulating the pre-populated Room DB asset for scaffolding
    private var offlineOuiDatabase = mapOf<String, String>()

    init {
        // Scaffold: Hardcoded top prefixes for fallback execution without downloading gigabytes
        offlineOuiDatabase = mapOf(
            "00:1A:11" to "Google, Inc.",
            "00:1E:C2" to "Apple, Inc.",
            "28:CF:E9" to "Apple, Inc.",
            "CC:50:E3" to "Espressif Inc.",
            "B8:27:EB" to "Raspberry Pi Foundation",
            "DC:A6:32" to "Raspberry Pi Trading Ltd",
            "00:23:14" to "Intel Corporate",
            "E8:AB:FA" to "Hikvision Digital Technology",
            "00:24:BE" to "Sony Corporation"
        )
    }

    // This asset loader demonstrates how to pull a lightweight JSON/CSV mapping into memory/Room
    suspend fun loadDatabaseFromAssets() = withContext(Dispatchers.IO) {
        try {
            context.assets.open("oui_database.json").use { inputStream ->
                val jsonText = InputStreamReader(inputStream).readText()
                val jsonObject = JSONObject(jsonText)
                val newMap = mutableMapOf<String, String>()
                jsonObject.keys().forEach { key ->
                    newMap[key.uppercase()] = jsonObject.getString(key)
                }
                offlineOuiDatabase = newMap
            }
        } catch (e: Exception) {
            // Asset not found, rely on fallback map gracefully
        }
    }

    fun resolveVendor(macAddress: String?): String? {
        if (macAddress.isNullOrBlank()) return null

        // 1. Gracefully handle randomized MAC addresses using strict bitwise logic
        // The second character of the first octet indicates U/L bit (Locally Administered)
        val cleanedMac = macAddress.replace(":", "").replace("-", "").uppercase()
        if (cleanedMac.length >= 2) {
            val firstOctetStr = cleanedMac.substring(0, 2)
            try {
                val firstOctet = firstOctetStr.toInt(16)
                if ((firstOctet and 0b00000010) != 0) {
                    return "Randomized MAC (iOS/Android/Windows Privacy)"
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        if (cleanedMac.length < 6) return null
        val ouiPrefix = macAddress.substring(0, 8).uppercase() // XX:XX:XX

        // 2. Check LRU Cache to avoid DB queries
        memoryCache.get(ouiPrefix)?.let { return it }

        // 3. Query Offline Database
        val vendor = offlineOuiDatabase[ouiPrefix] ?: offlineOuiDatabase[cleanedMac.substring(0, 6)]
        
        if (vendor != null) {
            memoryCache.put(ouiPrefix, vendor)
            return vendor
        }

        return "Unknown Vendor"
    }
}
