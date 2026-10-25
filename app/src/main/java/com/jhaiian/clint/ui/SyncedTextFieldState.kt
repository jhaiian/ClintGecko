package com.jhaiian.clint.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
fun rememberSyncedTextFieldState(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val state = remember { TextFieldState(value) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var synced by remember { mutableStateOf(value) }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != synced) {
                synced = text
                currentOnValueChange(text)
            }
        }
    }
    LaunchedEffect(value, synced) {
        if (value != synced && state.text.toString() == synced) {
            synced = value
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    return state
}
