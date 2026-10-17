package com.jhaiian.clint.privacy

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.backup.AuthGateResult
import com.jhaiian.clint.backup.BackupAuthGate
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

class AppLockActivity : ClintActivity() {

    private var authInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"

        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        setContent {
            ClintComposeTheme(theme = theme) {
                AppLockScreen(onUnlock = { startAuthentication() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startAuthentication()
    }

    private fun startAuthentication() {
        if (authInProgress) return
        authInProgress = true
        lifecycleScope.launch {
            val result = BackupAuthGate.authenticate(
                this@AppLockActivity,
                getString(R.string.app_lock_prompt_title, getString(R.string.app_name)),
                getString(R.string.app_lock_prompt_subtitle)
            )
            authInProgress = false
            when (result) {
                is AuthGateResult.Success -> {
                    PrivacyGuard.unlock()
                    finish()
                }
                is AuthGateResult.NotAvailable -> {
                    PreferenceManager.getDefaultSharedPreferences(this@AppLockActivity)
                        .edit().putBoolean(PrivacyPrefs.APP_LOCK, false).apply()
                    PrivacyGuard.unlock()
                    finish()
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun AppLockScreen(onUnlock: () -> Unit) {
    val colors = LocalClintColors.current
    Box(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.app_lock_screen_title),
                color = colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.app_lock_screen_message),
                color = colors.secondaryText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                Text(stringResource(R.string.app_lock_unlock))
            }
        }
    }
}
