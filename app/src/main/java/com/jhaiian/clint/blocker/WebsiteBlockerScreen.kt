package com.jhaiian.clint.blocker

import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.status.FilterListItemOptionsMenu
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.SortMenu
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import java.util.Date

fun categoryTitleRes(id: String): Int = when (id) {
    WebsiteBlockerCategoryIds.ABUSE -> R.string.website_blocker_category_abuse
    WebsiteBlockerCategoryIds.CRYPTO -> R.string.website_blocker_category_crypto
    WebsiteBlockerCategoryIds.DRUGS -> R.string.website_blocker_category_drugs
    WebsiteBlockerCategoryIds.FRAUD -> R.string.website_blocker_category_fraud
    WebsiteBlockerCategoryIds.GAMBLING -> R.string.website_blocker_category_gambling
    WebsiteBlockerCategoryIds.MALWARE -> R.string.website_blocker_category_malware
    WebsiteBlockerCategoryIds.PHISHING -> R.string.website_blocker_category_phishing
    WebsiteBlockerCategoryIds.PIRACY -> R.string.website_blocker_category_piracy
    WebsiteBlockerCategoryIds.PORN -> R.string.website_blocker_category_porn
    WebsiteBlockerCategoryIds.RANSOMWARE -> R.string.website_blocker_category_ransomware
    WebsiteBlockerCategoryIds.REDIRECT -> R.string.website_blocker_category_redirect
    WebsiteBlockerCategoryIds.SCAM -> R.string.website_blocker_category_scam
    WebsiteBlockerCategoryIds.SOCIAL -> R.string.website_blocker_category_social
    WebsiteBlockerCategoryIds.TORRENT -> R.string.website_blocker_category_torrent
    WebsiteBlockerCategoryIds.TRACKING -> R.string.website_blocker_category_tracking
    else -> R.string.website_blocker_category_social
}

fun categoryDescriptionRes(id: String): Int = when (id) {
    WebsiteBlockerCategoryIds.ABUSE -> R.string.website_blocker_category_abuse_desc
    WebsiteBlockerCategoryIds.CRYPTO -> R.string.website_blocker_category_crypto_desc
    WebsiteBlockerCategoryIds.DRUGS -> R.string.website_blocker_category_drugs_desc
    WebsiteBlockerCategoryIds.FRAUD -> R.string.website_blocker_category_fraud_desc
    WebsiteBlockerCategoryIds.GAMBLING -> R.string.website_blocker_category_gambling_desc
    WebsiteBlockerCategoryIds.MALWARE -> R.string.website_blocker_category_malware_desc
    WebsiteBlockerCategoryIds.PHISHING -> R.string.website_blocker_category_phishing_desc
    WebsiteBlockerCategoryIds.PIRACY -> R.string.website_blocker_category_piracy_desc
    WebsiteBlockerCategoryIds.PORN -> R.string.website_blocker_category_porn_desc
    WebsiteBlockerCategoryIds.RANSOMWARE -> R.string.website_blocker_category_ransomware_desc
    WebsiteBlockerCategoryIds.REDIRECT -> R.string.website_blocker_category_redirect_desc
    WebsiteBlockerCategoryIds.SCAM -> R.string.website_blocker_category_scam_desc
    WebsiteBlockerCategoryIds.SOCIAL -> R.string.website_blocker_category_social_desc
    WebsiteBlockerCategoryIds.TORRENT -> R.string.website_blocker_category_torrent_desc
    WebsiteBlockerCategoryIds.TRACKING -> R.string.website_blocker_category_tracking_desc
    else -> R.string.website_blocker_category_social_desc
}

@Composable
fun WebsiteBlockerScreen(
    state: WebsiteBlockerUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onMasterToggle: (Boolean) -> Unit,
    onAdditionalWebsitesClick: () -> Unit,
    onItemToggle: (WebsiteBlockerCategory, Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onFabPrimaryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit,
    onRecompile: () -> Unit,
    onItemCheckUpdate: (WebsiteBlockerCategory) -> Unit,
    onItemForceUpdate: (WebsiteBlockerCategory) -> Unit,
    onItemRemove: (WebsiteBlockerCategory) -> Unit,
    onItemCopyName: (WebsiteBlockerCategory) -> Unit,
    onItemCopyLink: (WebsiteBlockerCategory) -> Unit,
    onItemShareLink: (WebsiteBlockerCategory) -> Unit,
    onSelectionCheckUpdate: () -> Unit,
    onSelectionForceUpdate: () -> Unit,
    onSelectionRemove: () -> Unit,
    onSelectionCopyName: () -> Unit,
    onSelectionCopyLink: () -> Unit,
    onSelectionShareLink: () -> Unit
) {
    val colors = LocalClintColors.current
    val categoryTitles = state.categories.associate { it.id to stringResource(categoryTitleRes(it.id)) }
    fun titleOf(category: WebsiteBlockerCategory): String = categoryTitles[category.id] ?: category.id
    val displayed = filterAndSortCategories(state.categories, state.searchQuery, state.sortKey, state.sortOrder) { titleOf(it) }
    val listState = rememberLazyListState()
    val locked = state.isCompileRunning || state.isDownloadRunning
    val dirty = state.isConfigurationDirty()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()
    val showPrimaryFab = !state.isInSelectionMode && dirty && !locked

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WebsiteBlockerToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) },
                onRefreshClick = onRefreshClick,
                onCheckUpdateActive = onCheckUpdateActive,
                onCheckUpdateAll = onCheckUpdateAll,
                onForceUpdateActive = onForceUpdateActive,
                onForceUpdateAll = onForceUpdateAll,
                onRecompile = onRecompile,
                onSelectionCheckUpdate = onSelectionCheckUpdate,
                onSelectionForceUpdate = onSelectionForceUpdate,
                onSelectionRemove = onSelectionRemove,
                onSelectionCopyName = onSelectionCopyName,
                onSelectionCopyLink = onSelectionCopyLink,
                onSelectionShareLink = onSelectionShareLink
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            state.bannerText?.let { message ->
                Row(
                    Modifier.fillMaxWidth().background(colors.colorErrorContainer).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Warning, contentDescription = null, tint = colors.colorError, modifier = Modifier.size(20.dp))
                    Text(message, color = colors.colorOnErrorContainer, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }

            MasterSwitchRow(masterEnabled = state.masterEnabled, onToggle = onMasterToggle)

            Text(
                stringResource(R.string.website_blocker_section_categories),
                color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp).alpha(if (state.masterEnabled) 1f else 0.38f)
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 2.dp, bottom = 88.dp)) {
                        item(key = "additional_websites") {
                            AdditionalWebsitesRow(
                                count = state.additionalWebsitesCount,
                                masterEnabled = state.masterEnabled,
                                interactionLocked = locked,
                                onClick = onAdditionalWebsitesClick
                            )
                        }
                        items(displayed, key = { it.id }) { category ->
                            WebsiteBlockerCategoryRow(
                                category = category,
                                title = titleOf(category),
                                isSelected = category.id in state.selectedIds,
                                isDownloading = category.id == state.downloadProgress?.categoryId,
                                masterEnabled = state.masterEnabled,
                                interactionLocked = locked,
                                isInSelectionMode = state.isInSelectionMode,
                                onClick = {
                                    if (state.isInSelectionMode) state.toggleSelection(category.id) else onItemToggle(category, !category.isEnabled)
                                },
                                onLongClick = {
                                    if (!state.isInSelectionMode) {
                                        state.enterSelectionWith(category.id)
                                    } else if (category.id !in state.selectedIds) {
                                        state.selectedIds = state.selectedIds + category.id
                                    }
                                },
                                onCheckUpdate = { onItemCheckUpdate(category) },
                                onForceUpdate = { onItemForceUpdate(category) },
                                onRemove = { onItemRemove(category) },
                                onCopyName = { onItemCopyName(category) },
                                onCopyLink = { onItemCopyLink(category) },
                                onShareLink = { onItemShareLink(category) }
                            )
                        }
                    }
                    ListFastScroller(
                        listState = listState,
                        itemCount = displayed.size + 1,
                        isInteractive = fastScrollerInteractive,
                        sectionLetterAt = { index ->
                            if (index == 0) "#" else sectionLetterForCategory(displayed[index - 1], state.sortKey) { titleOf(it) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WebsiteBlockerFab(
                    enabled = state.masterEnabled && !locked,
                    showDeleteFab = showDeleteFab,
                    showPrimaryFab = showPrimaryFab,
                    onDeleteClick = onDeleteSelected,
                    onPrimaryClick = onFabPrimaryClick
                )
            }
        }
    }
}

@Composable
private fun MasterSwitchRow(masterEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    SettingsRow(
        icon = androidx.compose.material.icons.Icons.Filled.Shield,
        title = stringResource(R.string.website_blocker_master_switch_title),
        summary = stringResource(R.string.website_blocker_settings_summary),
        colors = colors,
        onClick = { onToggle(!masterEnabled) },
        trailing = {
            ClintSwitch(checked = masterEnabled)
        }
    )
}

@Composable
private fun AdditionalWebsitesRow(
    count: Int,
    masterEnabled: Boolean,
    interactionLocked: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .alpha(if (masterEnabled) 1f else 0.38f)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .clickable(enabled = masterEnabled && !interactionLocked, onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Filled.Language, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.additional_websites_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.additional_websites_summary, count),
                color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WebsiteBlockerToolbar(
    state: WebsiteBlockerUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onRefreshClick: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit,
    onRecompile: () -> Unit,
    onSelectionCheckUpdate: () -> Unit,
    onSelectionForceUpdate: () -> Unit,
    onSelectionRemove: () -> Unit,
    onSelectionCopyName: () -> Unit,
    onSelectionCopyLink: () -> Unit,
    onSelectionShareLink: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode
    var selectionItemOptionsMenuOpen by remember { mutableStateOf(false) }

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null, tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = stringResource(R.string.website_blocker_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.history_selected_count, state.selectedIds.size) else stringResource(R.string.website_blocker_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = colors.iconTint)
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Update, contentDescription = null, tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = colors.iconTint)
                    }
                    SortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                    )
                }
                Box {
                    IconButton(onClick = { state.actionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
                    }
                    WebsiteBlockerActionsMenu(
                        expanded = state.actionsMenuOpen,
                        onDismiss = { state.actionsMenuOpen = false },
                        onCheckUpdateActive = onCheckUpdateActive,
                        onCheckUpdateAll = onCheckUpdateAll,
                        onForceUpdateActive = onForceUpdateActive,
                        onForceUpdateAll = onForceUpdateAll,
                        onRecompile = onRecompile
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = null, tint = colors.iconTint)
                    }
                    SelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
                if (state.selectedIds.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { selectionItemOptionsMenuOpen = true }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
                        }
                        FilterListItemOptionsMenu(
                            expanded = selectionItemOptionsMenuOpen,
                            isLocal = false,
                            onDismiss = { selectionItemOptionsMenuOpen = false },
                            onCheckUpdate = onSelectionCheckUpdate,
                            onForceUpdate = onSelectionForceUpdate,
                            onRemove = onSelectionRemove,
                            onCopyName = onSelectionCopyName,
                            onCopyLink = onSelectionCopyLink,
                            onShareLink = onSelectionShareLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsiteBlockerCategoryRow(
    category: WebsiteBlockerCategory,
    title: String,
    isSelected: Boolean,
    isDownloading: Boolean,
    masterEnabled: Boolean,
    interactionLocked: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    onForceUpdate: () -> Unit,
    onRemove: () -> Unit,
    onCopyName: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    var optionsMenuOpen by remember { mutableStateOf(false) }
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground
    val rowAlpha = when {
        !masterEnabled -> 0.38f
        isDownloading -> 0.6f
        else -> 1f
    }
    val statusText = when {
        isDownloading -> stringResource(R.string.filter_list_status_downloading)
        category.isDownloaded -> stringResource(
            R.string.website_blocker_status_downloaded_with_domains,
            category.domainCount.toString(),
            formatFileSize(category.fileSizeBytes),
            DateFormat.getMediumDateFormat(context).format(Date(category.downloadedAt))
        )
        else -> stringResource(R.string.filter_list_status_not_downloaded)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(enabled = masterEnabled && !interactionLocked, onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(categoryDescriptionRes(category.id)),
                color = colors.secondaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(statusText, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (!isInSelectionMode) {
            ClintSwitch(checked = category.isEnabled)
        }
        Box {
            IconButton(onClick = { optionsMenuOpen = true }, enabled = masterEnabled && !interactionLocked) {
                Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
            }
            FilterListItemOptionsMenu(
                expanded = optionsMenuOpen,
                isLocal = false,
                onDismiss = { optionsMenuOpen = false },
                onCheckUpdate = onCheckUpdate,
                onForceUpdate = onForceUpdate,
                onRemove = onRemove,
                onCopyName = onCopyName,
                onCopyLink = onCopyLink,
                onShareLink = onShareLink
            )
        }
    }
}

@Composable
private fun BoxScope.WebsiteBlockerFab(
    enabled: Boolean,
    showDeleteFab: Boolean,
    showPrimaryFab: Boolean,
    onDeleteClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    val colors = LocalClintColors.current
    if (showDeleteFab) {
        FloatingActionButton(
            onClick = onDeleteClick,
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    } else if (showPrimaryFab) {
        FloatingActionButton(
            onClick = onPrimaryClick,
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier
                .align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                .alpha(if (!enabled) 0.38f else 1f)
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.Save, contentDescription = stringResource(R.string.website_blocker_fab_save_desc))
        }
    }
}
