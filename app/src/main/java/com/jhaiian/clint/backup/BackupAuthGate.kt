package com.jhaiian.clint.backup

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

sealed class AuthGateResult {
    object Success : AuthGateResult()
    object Failed : AuthGateResult()
    object NotAvailable : AuthGateResult()
    data class Error(val message: String?) : AuthGateResult()
}

object BackupAuthGate {

    fun isAuthenticationAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): AuthGateResult {
        if (!isAuthenticationAvailable(activity)) return AuthGateResult.NotAvailable

        return suspendCancellableCoroutine { continuation ->
            val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(AuthGateResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthGateResult.Failed
                            else -> AuthGateResult.Error(errString.toString())
                        }
                        continuation.resume(result)
                    }
                }

                override fun onAuthenticationFailed() {}
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

            continuation.invokeOnCancellation { runCatching { prompt.cancelAuthentication() } }
            prompt.authenticate(promptInfo)
        }
    }
}
