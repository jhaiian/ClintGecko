package com.jhaiian.clint.browser.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.theme.LocalClintColors

internal val LongPressCardCorner = 16.dp

internal val LongPressContentMaxWidth = 480.dp

@Composable
internal fun LongPressActionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
        }
        Text(text, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
internal fun LongPressSheetDivider() {
    HorizontalDivider(color = LocalClintColors.current.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}
