package com.example.networkscanner.domain.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentinelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startSentinel() {
        val intent = Intent(context, NetworkSentinelService::class.java)
        context.startForegroundService(intent)
    }

    fun stopSentinel() {
        val intent = Intent(context, NetworkSentinelService::class.java)
        context.stopService(intent)
    }
}
