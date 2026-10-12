package com.jhaiian.clint.ui

import android.widget.TextView
import android.util.TypedValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import io.noties.markwon.Markwon

@Composable
private fun DocumentViewerContent(state: DocumentViewerUiState) {
    val colors = LocalClintColors.current
    when {
        state.isLoading -> Box(
            Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary, trackColor = colors.surfaceVariant)
        }
        state.isError -> Text(
            stringResource(R.string.document_viewer_error),
            color = colors.secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
        )
        else -> {
            val onSurfaceArgb = colors.onSurface.toArgb()
            val linkArgb = colors.primary.toArgb()
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(onSurfaceArgb)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setLineSpacing(0f, 1.3f)
                    }
                },
                update = { tv ->
                    tv.setTextColor(onSurfaceArgb)
                    tv.setLinkTextColor(linkArgb)
                    Markwon.create(tv.context).setMarkdown(tv, state.markdown.orEmpty())
                }
            )
        }
    }
}

@Composable
fun DocumentViewerDialog(
    title: String,
    state: DocumentViewerUiState,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        cancelable = false,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.back), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        DocumentViewerContent(state)
    }
}
