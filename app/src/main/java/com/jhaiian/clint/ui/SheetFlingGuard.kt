package com.jhaiian.clint.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val SHEET_SETTLE_WAIT_MS = 1000L

internal val ClintSheetWindowInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetFlingConnection(
    sheetState: SheetState,
    consumeFling: Boolean = true
): NestedScrollConnection = remember(sheetState, consumeFling) {
    object : NestedScrollConnection {
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            withTimeoutOrNull(SHEET_SETTLE_WAIT_MS) {
                snapshotFlow {
                    sheetState.currentValue == SheetValue.Hidden && sheetState.targetValue != SheetValue.Hidden
                }.first { opening -> !opening }
            }
            return if (consumeFling) available else Velocity.Zero
        }
    }
}
