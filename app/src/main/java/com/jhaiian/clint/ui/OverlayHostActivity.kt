package com.jhaiian.clint.ui

import androidx.compose.runtime.Composable

interface OverlayHostActivity {
    var overlayContent: (@Composable () -> Unit)?
}
