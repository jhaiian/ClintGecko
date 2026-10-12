package com.jhaiian.clint.setup
import androidx.compose.material.icons.filled.Check

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    cardBackground: Color,
    primary: Color,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    bottomSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomSpacing)
            .alpha(if (selected) 1.0f else 0.45f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = if (selected) BorderStroke(3.dp, primary) else null
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun RowScope.CheckSlot(visible: Boolean, tint: Color) {
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        if (visible) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                contentDescription = null,
                tint = tint
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, primary: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = primary,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
fun SetupPrimaryButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1.0f else 0.5f),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
fun DefaultChip(text: String, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, color, RoundedCornerShape(12.dp))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DrawableImage(drawableRes: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx -> android.view.View(ctx) },
        update = { view -> view.background = ContextCompat.getDrawable(view.context, drawableRes) },
        modifier = modifier
    )
}

@Composable
fun SetupPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colors = LocalClintColors.current
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.onSurface,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun SetupSectionHeading(
    icon: ImageVector,
    text: String,
    primary: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(15.dp))
        Text(
            text = text,
            color = primary,
            fontSize = 11.sp,
            letterSpacing = 0.1.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun SetupCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalClintColors.current.secondaryText,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
fun SetupStepDots(
    total: Int,
    current: Int,
    primary: Color,
    track: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val active = index == current
            val passed = index < current
            Box(
                Modifier
                    .height(5.dp)
                    .width(if (active) 22.dp else 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active || passed) primary else track)
            )
        }
    }
}

@Composable
fun RowScope.OptionTile(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    cardBackground: Color,
    primary: Color,
    onPrimary: Color,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit
) {
    Box(modifier.weight(1f)) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().alpha(if (selected) 1f else 0.55f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = if (selected) BorderStroke(2.dp, primary) else null
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                preview()
                Text(
                    text = label,
                    color = if (selected) primary else LocalClintColors.current.onSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    tint = onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
