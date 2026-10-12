package com.jhaiian.clint.browser.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun ActionSheetRow(iconRes: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(iconRes, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        Text(text, color = colors.onSurface, fontSize = 15.sp, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
internal fun ActionSheetDivider() {
    HorizontalDivider(color = LocalClintColors.current.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}
