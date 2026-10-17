package com.jhaiian.clint.privacy

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.preference.PreferenceManager

object IncognitoKeyboard {

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PrivacyPrefs.INCOGNITO_KEYBOARD, PrivacyPrefs.DEFAULT_INCOGNITO_KEYBOARD)

    fun apply(outAttrs: EditorInfo) {
        outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IncognitoKeyboardScope(enabled: Boolean, content: @Composable () -> Unit) {
    val active by rememberUpdatedState(enabled)
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            nextHandler.startInputMethod(
                object : PlatformTextInputMethodRequest {
                    override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                        val connection = request.createInputConnection(outAttributes)
                        if (active) IncognitoKeyboard.apply(outAttributes)
                        return connection
                    }
                }
            )
        },
        content = content
    )
}
