package com.example.networkscanner.di

import android.content.Context
import com.example.networkscanner.data.repository.LanScannerRepository
import com.example.networkscanner.data.repository.WifiAuditRepository
import com.example.networkscanner.domain.service.AuditPdfExporter
import com.example.networkscanner.domain.service.LatencyProfiler
import com.example.networkscanner.domain.service.OuiResolver
import com.example.networkscanner.domain.service.PortScannerService
import com.example.networkscanner.domain.service.VulnerabilityEngine
import com.example.networkscanner.domain.service.ArpSpoofDetector
import com.example.networkscanner.util.SentinelNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLanScannerRepository(@ApplicationContext context: Context): LanScannerRepository {
        return LanScannerRepository(context)
    }

    @Provides
    @Singleton
    fun provideWifiAuditRepository(@ApplicationContext context: Context): WifiAuditRepository {
        return WifiAuditRepository(context)
    }

    @Provides
    @Singleton
    fun providePortScannerService(): PortScannerService {
        return PortScannerService()
    }

    @Provides
    @Singleton
    fun provideOuiResolver(@ApplicationContext context: Context): OuiResolver {
        return OuiResolver(context)
    }

    @Provides
    @Singleton
    fun provideAuditPdfExporter(@ApplicationContext context: Context): AuditPdfExporter {
        return AuditPdfExporter(context)
    }

    @Provides
    @Singleton
    fun provideLatencyProfiler(): LatencyProfiler {
        return LatencyProfiler()
    }

    @Provides
    @Singleton
    fun provideBufferbloatAuditor(profiler: LatencyProfiler): com.example.networkscanner.domain.service.BufferbloatAuditor {
        return com.example.networkscanner.domain.service.BufferbloatAuditor(profiler)
    }

    @Provides
    @Singleton
    fun provideVulnerabilityEngine(): VulnerabilityEngine {
        return VulnerabilityEngine()
    }

    @Provides
    @Singleton
    fun provideArpSpoofDetector(): ArpSpoofDetector {
        return ArpSpoofDetector()
    }

    @Provides
    @Singleton
    fun provideSentinelNotificationManager(@ApplicationContext context: Context): SentinelNotificationManager {
        return SentinelNotificationManager(context)
    }
}
