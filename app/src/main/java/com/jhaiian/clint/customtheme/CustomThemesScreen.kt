package com.jhaiian.clint.customtheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.AccentSwatch
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun ThemeTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalClintColors.current
    Surface(
        color = colors.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
            }
            Text(
                title,
                color = colors.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            actions()
        }
    }
    HorizontalDivider(color = colors.divider, thickness = 1.dp)
}

@Composable
fun CustomThemesScreen(
    themes: List<CustomTheme>,
    activeAccent: String,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onCreate: () -> Unit,
    onApply: (CustomTheme) -> Unit,
    onEdit: (CustomTheme) -> Unit,
    onDuplicate: (CustomTheme) -> Unit,
    onDelete: (CustomTheme) -> Unit
) {
    val colors = LocalClintColors.current
    var pendingDelete by remember { mutableStateOf<CustomTheme?>(null) }
    val deleteConfig = pendingDelete?.let { theme ->
        ConfirmDialogConfig(
            title = stringResource(R.string.custom_theme_delete_title),
            message = stringResource(R.string.custom_theme_delete_message, theme.name),
            positiveLabel = stringResource(R.string.action_delete),
            onPositive = { onDelete(theme) },
            negativeLabel = stringResource(R.string.action_cancel)
        )
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ThemeTopBar(title = stringResource(R.string.custom_themes_title), onBack = onExit)
            AdaptiveWidthContainer(maxContentWidth) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    SettingsSection(colors.cardBackground) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.ColorLens,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                stringResource(R.string.custom_themes_intro),
                                color = colors.secondaryText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onCreate,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.custom_theme_create),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    if (themes.isEmpty()) {
                        EmptyState()
                    } else {
                        themes.forEach { theme ->
                            ThemeCard(
                                theme = theme,
                                active = activeAccent == customAccentFor(theme.id),
                                onApply = { onApply(theme) },
                                onEdit = { onEdit(theme) },
                                onDuplicate = { onDuplicate(theme) },
                                onDelete = { pendingDelete = theme }
                            )
                        }
                    }
                }
            }
        }
    }

    ConfirmDialogHost(
        config = deleteConfig,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = { pendingDelete = null }
    )
}

@Composable
private fun EmptyState() {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.ColorLens,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(56.dp)
        )
        Text(
            stringResource(R.string.custom_theme_empty_title),
            color = colors.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(R.string.custom_theme_empty_message),
            color = colors.secondaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ThemeCard(
    theme: CustomTheme,
    active: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val palette = theme.palette(colors.isLight)

    SelectableCard(
        selected = active,
        onClick = onApply,
        cardBackground = colors.surfaceVariant,
        primary = colors.primary
    ) {
        AccentSwatch(
            bg = palette.color(ThemeSlot.BACKGROUND),
            surface = palette.color(ThemeSlot.SURFACE),
            accent = palette.color(ThemeSlot.PRIMARY)
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                theme.name,
                color = colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (theme.description.isNotBlank()) {
                Text(
                    theme.description,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (active || theme.description.isBlank()) {
                Text(
                    stringResource(
                        if (active) R.string.custom_theme_active else R.string.custom_theme_tap_to_apply
                    ),
                    color = if (active) colors.primary else colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.custom_theme_edit),
                tint = colors.iconTint
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.custom_theme_more),
                    tint = colors.iconTint
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.custom_theme_duplicate), checked = false) {
                    menuOpen = false
                    onDuplicate()
                }
                ListMenuItem(Icons.Filled.Delete, stringResource(R.string.custom_theme_delete), checked = false) {
                    menuOpen = false
                    onDelete()
                }
            }
        }
    }
}
