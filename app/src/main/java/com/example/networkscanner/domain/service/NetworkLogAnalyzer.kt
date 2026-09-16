package com.example.networkscanner.domain.service

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkLogAnalyzer @Inject constructor(
    private val llmEngine: LlmInferenceEngine
) {
    fun analyzeScanOutput(rawScanOrBannerData: String): Flow<String> {
        val systemPrompt = """
            You are a Network Security Expert. Analyze the following network scan logs, open ports, service banners, and TLS details.
            Identify potential vulnerabilities, misconfigurations, and security risks.
            Provide structured findings with severity levels and remediation steps.
            
            SCAN DATA:
            $rawScanOrBannerData
            
            ASSESSMENT:
        """.trimIndent()

        return llmEngine.generateResponseAsync(systemPrompt)
    }
}
