package com.jhaiian.clint.blocker.additional

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Date

@Composable
fun AdditionalWebsitesScreen(
    state: AdditionalWebsitesUiState,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onAddHosts: (List<String>) -> Unit,
    onDeleteSelected: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.rules, state.searchQuery) { filterRules(state.rules, state.searchQuery) }
    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()
    val showAddFab = !state.isInSelectionMode
    val invalidHostMessage = stringResource(R.string.additional_websites_invalid_host)

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AdditionalWebsitesToolbar(state = state, displayed = displayed, onBack = ::handleBack)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                Modifier.fillMaxWidth().clickable { onToggleEnabled(!state.isEnabled) }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.additional_websites_master_switch_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.additional_websites_master_switch_summary), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                ClintSwitch(checked = state.isEnabled)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (displayed.isEmpty()) {
                    Text(
                        stringResource(R.string.additional_websites_empty),
                        color = colors.secondaryText, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                        ) {
                            items(displayed, key = { it.id }) { rule ->
                                AdditionalWebsiteRuleRow(
                                    rule = rule,
                                    masterEnabled = state.isEnabled,
                                    isSelected = rule.id in state.selectedIds,
                                    isInSelectionMode = state.isInSelectionMode,
                                    onClick = {
                                        if (state.isInSelectionMode) state.toggleSelection(rule.id)
                                    },
                                    onLongClick = {
                                        if (!state.isInSelectionMode) {
                                            state.isInSelectionMode = true
                                            state.selectedIds = state.selectedIds + rule.id
                                        } else if (rule.id !in state.selectedIds) {
                                            state.selectedIds = state.selectedIds + rule.id
                                        }
                                    },
                                    onDeleteClick = {
                                        state.isInSelectionMode = true
                                        state.selectedIds = setOf(rule.id)
                                        onDeleteSelected()
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteSelected,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
                if (showAddFab) {
                    FloatingActionButton(
                        onClick = { state.isAddDialogOpen = true; state.addDialogText = ""; state.addDialogError = null },
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = stringResource(R.string.additional_websites_add_fab_desc))
                    }
                }
            }
        }
    }

    if (state.isAddDialogOpen) {
        AdditionalWebsiteAddDialog(
            state = state,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            invalidHostMessage = invalidHostMessage,
            onConfirm = onAddHosts
        )
    }
}

@Composable
private fun AdditionalWebsitesToolbar(
    state: AdditionalWebsitesUiState,
    displayed: List<AdditionalWebsiteRule>,
    onBack: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(if (state.isInSelectionMode) R.string.history_cancel_selection_desc else R.string.back),
                    tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = stringResource(R.string.additional_websites_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.history_selected_count, state.selectedIds.size) else stringResource(R.string.additional_websites_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.history_search), tint = colors.iconTint)
                }
            }
            if (state.isInSelectionMode) {
                IconButton(onClick = { state.selectedIds = displayed.map { it.id }.toSet() }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun AdditionalWebsiteRuleRow(
    rule: AdditionalWebsiteRule,
    masterEnabled: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .alpha(if (masterEnabled) 1f else 0.6f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Filled.Language,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(rule.host, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                DateFormat.getMediumDateFormat(androidx.compose.ui.platform.LocalContext.current).format(Date(rule.createdAt)),
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (isInSelectionMode) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null,
                        tint = colors.primary, modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            IconButton(onClick = onDeleteClick) {
                Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = colors.iconTint)
            }
        }
    }
}

@Composable
private fun AdditionalWebsiteAddDialog(
    state: AdditionalWebsitesUiState,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    invalidHostMessage: String,
    onConfirm: (List<String>) -> Unit
) {
    val colors = LocalClintColors.current
    val validHost = remember { Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$") }

    fun submit() {
        val lines = state.addDialogText.split("\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || lines.any { !validHost.matches(it) }) {
            state.addDialogError = invalidHostMessage
        } else {
            state.isAddDialogOpen = false
            onConfirm(lines.distinct())
        }
    }

    ClintDialog(
        title = stringResource(R.string.additional_websites_add_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = { state.isAddDialogOpen = false },
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { state.isAddDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { submit() }, enabled = state.addDialogText.isNotBlank()) {
                    Text(stringResource(R.string.additional_websites_add_action), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.additional_websites_add_dialog_description),
            color = colors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
        )
        ClintOutlinedTextField(
            value = state.addDialogText,
            onValueChange = { state.addDialogText = it; state.addDialogError = null },
            label = { Text(stringResource(R.string.additional_websites_add_hint)) },
            singleLine = false,
            isError = state.addDialogError != null,
            supportingText = state.addDialogError?.let { error -> { Text(error, color = colors.colorError) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
