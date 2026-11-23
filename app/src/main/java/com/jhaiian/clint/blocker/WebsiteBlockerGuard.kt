package com.jhaiian.clint.blocker

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.parental.ParentalGateRequest
import com.jhaiian.clint.parental.ParentalControls
import com.jhaiian.clint.parental.ParentalPasswordVerifyDialog
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.status.formatElapsedSeconds
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import java.security.SecureRandom

object WebsiteBlockerGuard {
    const val WAIT_SECONDS = 30
    const val CODE_LENGTH = 6
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun newCode(): String = String(CharArray(CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] })

    fun matches(typed: String, code: String): Boolean = typed.trim().equals(code, ignoreCase = true)
}

@Composable
fun WebsiteBlockerGuardHost(
    request: ParentalGateRequest?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    if (request == null) return
    val context = LocalContext.current
    val passwordProtected = remember(request) { ParentalControls.isProtected(context, ParentalControls.AREA_BLOCKER) }
    if (passwordProtected) {
        ParentalPasswordVerifyDialog(request, hideStatusBar, hideSystemNavigation, onDismiss)
    } else {
        WebsiteBlockerDelayDialog(request, hideStatusBar, hideSystemNavigation, onDismiss)
    }
}

@Composable
private fun WebsiteBlockerDelayDialog(
    request: ParentalGateRequest,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val totalMs = WebsiteBlockerGuard.WAIT_SECONDS * 1000L
    val code = remember(request) { WebsiteBlockerGuard.newCode() }
    var typed by remember(request) { mutableStateOf("") }
    var remainingMs by remember(request) { mutableLongStateOf(totalMs) }
    val waiting = remainingMs > 0L

    LaunchedEffect(request) {
        val endAt = SystemClock.elapsedRealtime() + totalMs
        while (true) {
            val left = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            remainingMs = left
            if (left == 0L) break
            delay(100L)
        }
    }

    ClintDialog(
        title = request.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = request.positiveLabel,
                onPositive = { onDismiss(); request.onConfirm() },
                positiveEnabled = !waiting && WebsiteBlockerGuard.matches(typed, code)
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(request.message, color = colors.onSurface, fontSize = 14.sp)
            if (waiting) {
                Text(
                    stringResource(R.string.website_blocker_guard_wait, formatElapsedSeconds((remainingMs + 999L) / 1000L)),
                    color = colors.secondaryText, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LinearProgressIndicator(
                    progress = { (1f - remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.website_blocker_guard_code_prompt),
                    color = colors.secondaryText, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    code,
                    color = colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
                ClintOutlinedTextField(
                    value = typed,
                    onValueChange = { input -> typed = input.filterNot { it.isWhitespace() }.take(WebsiteBlockerGuard.CODE_LENGTH) },
                    label = { Text(stringResource(R.string.website_blocker_guard_code_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
