package com.jhaiian.clint.settings.supportclint

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.theme.LocalClintColors
import io.noties.markwon.Markwon
import io.noties.markwon.image.ImagesPlugin

@Composable
fun SupportClintScreen(
    onBack: () -> Unit,
    onViewSupportersClick: () -> Unit
) {
    val colors = LocalClintColors.current
    Column(Modifier.fillMaxSize()) {
        SupportClintToolbar(onBack = onBack)
        Box(Modifier.weight(1f)) {
            SettingsScreenScaffold {
                SettingsSection(colors.cardBackground) {
                    Box(Modifier.padding(16.dp)) {
                        SupportClintDonationContent()
                    }
                }
                SettingsSection(colors.cardBackground) {
                    SettingsRow(
                        icon = Icons.Filled.Groups,
                        title = stringResource(R.string.support_clint_view_supporters_title),
                        summary = stringResource(R.string.support_clint_view_supporters_summary),
                        colors = colors,
                        onClick = onViewSupportersClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportClintToolbar(onBack: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
            }
            Text(
                stringResource(R.string.support_clint_title),
                color = colors.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SupportClintDonationContent() {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val markdown = stringResource(R.string.support_clint_markdown)
    val onSurfaceArgb = colors.onSurface.toArgb()
    val linkArgb = colors.primary.toArgb()
    val markwon = remember(context) {
        Markwon.builder(context).usePlugin(ImagesPlugin.create()).build()
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.3f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { tv ->
            tv.setTextColor(onSurfaceArgb)
            tv.setLinkTextColor(linkArgb)
            markwon.setMarkdown(tv, markdown)
        }
    )
}
