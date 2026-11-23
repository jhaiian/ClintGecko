package com.jhaiian.clint.downloads.mega

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.MainUiState
import com.jhaiian.clint.browser.delegates.openMegaOffer
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun MegaDownloadFab(activity: MainActivity, state: MainUiState, visible: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val offer = state.activeTabId?.let { state.megaOffers[it] }
    val findBottom = if (state.find.open && !state.find.barAtTop) state.find.barHeightPx else 0
    val bottomPx = maxOf(state.contentPaddingBottomPx, state.imeInsetPx) + findBottom
    AnimatedVisibility(
        visible = visible && offer != null,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
        modifier = modifier.padding(end = 16.dp, bottom = with(density) { bottomPx.toDp() } + 16.dp)
    ) {
        FloatingActionButton(
            onClick = { state.activeTabId?.let { state.megaOffers[it] }?.let { activity.openMegaOffer(it) } },
            containerColor = colors.buttonBackground,
            contentColor = colors.buttonIconTint
        ) {
            Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.mega_fab_download))
        }
    }
}
