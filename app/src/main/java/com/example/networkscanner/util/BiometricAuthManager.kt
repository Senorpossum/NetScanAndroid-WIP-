package com.example.networkscanner.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val activity: FragmentActivity) {

    /**
     * Triggers the OS Biometric Prompt.
     * Falls back gracefully to Device Credential (PIN/Pattern) if biometrics are unavailable.
     */
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        // Request either strong fingerprint/face auth OR the device lock PIN
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS, 
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showPrompt(onSuccess, onError, authenticators)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError("No secure hardware available on this device.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError("Biometric hardware is currently unavailable.")
            }
            else -> {
                onError("Authentication unavailable. Access Denied.")
            }
        }
    }

    private fun showPrompt(onSuccess: () -> Unit, onError: (String) -> Unit, authenticators: Int) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Security Audit")
            .setSubtitle("Confirm your identity to view sensitive network topologies and reports.")
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess() // Open the secure gates
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Try again.")
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}
