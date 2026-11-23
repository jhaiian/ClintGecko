package com.jhaiian.clint.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.BreakIterator

internal data class PageMetrics(val wide: Boolean, val heroHeight: Dp) {
    val align: TextAlign get() = if (wide) TextAlign.Start else TextAlign.Center
}

internal val LocalSetupEntrance = staticCompositionLocalOf { true }

internal class SetupHaptics(private val feedback: HapticFeedback) {
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    fun click() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)
    fun toggle(on: Boolean) = feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
}

@Composable
internal fun rememberSetupHaptics(): SetupHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { SetupHaptics(feedback) }
}

@Composable
fun SetupScreen(activity: SetupActivity, state: SetupUiState) {
    val colors = LocalClintColors.current
    val haptics = rememberSetupHaptics()
    val scope = rememberCoroutineScope()
    val steps = state.steps
    val step = state.step

    val go: (Int) -> Unit = { target ->
        val next = target.coerceIn(0, steps.lastIndex)
        if (next != state.index) {
            haptics.click()
            state.animateEntrance = true
            state.index = next
        }
    }
    val goNext: () -> Unit = { go(state.index + 1) }
    BackHandler(enabled = state.index > 0) { go(state.index - 1) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        activity.markNotificationsAsked()
        state.notificationsGranted = granted
        if (granted) {
            haptics.confirm()
            scope.launch {
                delay(900)
                if (state.step == SetupStep.NOTIFICATIONS) goNext()
            }
        }
    }

    LaunchedEffect(state.isDefaultBrowser) {
        if (state.isDefaultBrowser && state.defaultBrowserRequested && state.step == SetupStep.DEFAULT_BROWSER) {
            state.defaultBrowserRequested = false
            haptics.confirm()
            delay(900)
            if (state.step == SetupStep.DEFAULT_BROWSER) goNext()
        }
    }

    val googleTitle = stringResource(R.string.google_warning_title)
    val googleMessage = stringResource(R.string.google_warning_message)
    val googlePositive = stringResource(R.string.use_google_anyway)
    val googleNegative = stringResource(R.string.choose_another)

    val nextLabel = when (step) {
        SetupStep.WELCOME -> stringResource(R.string.setup_terms_continue)
        SetupStep.RESTORE -> stringResource(R.string.setup_skip)
        SetupStep.ADDONS -> stringResource(if (state.addonRequests.isEmpty()) R.string.setup_skip else R.string.setup_next)
        SetupStep.NOTIFICATIONS -> stringResource(if (state.notificationsGranted) R.string.setup_next else R.string.setup_not_now)
        SetupStep.DEFAULT_BROWSER -> stringResource(if (state.isDefaultBrowser) R.string.setup_next else R.string.setup_not_now)
        SetupStep.DONE -> stringResource(R.string.setup_start_browsing)
        else -> stringResource(R.string.setup_next)
    }
    val nextPrimary = when (step) {
        SetupStep.WELCOME -> state.consentChecked
        SetupStep.RESTORE -> false
        SetupStep.ADDONS -> state.addonRequests.isNotEmpty()
        SetupStep.NOTIFICATIONS -> state.notificationsGranted
        SetupStep.DEFAULT_BROWSER -> state.isDefaultBrowser
        else -> true
    }
    val onNext: () -> Unit = {
        when (step) {
            SetupStep.WELCOME -> {
                if (state.consentChecked) goNext() else {
                    haptics.reject()
                    state.consentNudge++
                }
            }
            SetupStep.SEARCH -> {
                if (state.engine == "google") {
                    state.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
                        title = googleTitle,
                        message = googleMessage,
                        positiveLabel = googlePositive,
                        onPositive = { goNext() },
                        negativeLabel = googleNegative
                    )
                } else goNext()
            }
            SetupStep.NOTIFICATIONS -> {
                activity.markNotificationsAsked()
                goNext()
            }
            SetupStep.DONE -> {
                haptics.confirm()
                activity.finishSetup()
            }
            else -> goNext()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            val heroHeight = if (maxHeight < 560.dp) 180.dp else (maxHeight * 0.38f).coerceAtMost(380.dp)
            val metrics = PageMetrics(wide, heroHeight)
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state.index,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val sign = if (forward != rtl) 1 else -1
                        val enter = slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 220f)) { full -> sign * full / 3 } +
                            fadeIn(tween(420, delayMillis = 80)) +
                            scaleIn(tween(520, easing = FastOutSlowInEasing), initialScale = 0.94f)
                        val exit = slideOutHorizontally(tween(340)) { full -> -sign * full / 4 } +
                            fadeOut(tween(220)) +
                            scaleOut(tween(340), targetScale = 1.04f)
                        (enter togetherWith exit).using(SizeTransform(clip = false))
                    },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "setup_step"
                ) { target ->
                    val page = steps[target.coerceIn(0, steps.lastIndex)]
                    CompositionLocalProvider(LocalSetupEntrance provides state.animateEntrance) {
                        val scroll = state.scrollState(page)
                        when (page) {
                            SetupStep.WELCOME -> SetupWelcomePage(activity, state, metrics, scroll)
                            SetupStep.RESTORE -> SetupRestorePage(
                                activity = activity,
                                metrics = metrics,
                                scrollState = scroll,
                                hideStatusBar = state.hideStatusBar,
                                hideSystemNavigation = state.hideSystemNavigation,
                                onRestoreComplete = { activity.restartAppAfterRestore() }
                            )
                            SetupStep.LOOK -> SetupLookPage(activity, state, metrics, scroll)
                            SetupStep.LAYOUT -> SetupLayoutPage(state, metrics, scroll)
                            SetupStep.SEARCH -> SetupSearchPage(state, metrics, scroll)
                            SetupStep.PRIVACY -> SetupPrivacyPage(state, metrics, scroll)
                            SetupStep.SECURITY -> SetupSecurityPage(state, metrics, scroll)
                            SetupStep.ADDONS -> SetupAddonsPage(activity, state, metrics, scroll)
                            SetupStep.NOTIFICATIONS -> SetupNotificationsPage(
                                state = state,
                                metrics = metrics,
                                scrollState = scroll,
                                onAllow = {
                                    haptics.click()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        runCatching { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                    }
                                }
                            )
                            SetupStep.DEFAULT_BROWSER -> SetupDefaultBrowserPage(
                                state = state,
                                metrics = metrics,
                                scrollState = scroll,
                                onSetDefault = {
                                    haptics.click()
                                    activity.requestDefaultBrowser()
                                }
                            )
                            SetupStep.DONE -> SetupDonePage(state, metrics, scroll, addonSummaryChips())
                        }
                    }
                }
                SetupBottomBar(
                    step = step,
                    index = state.index,
                    count = steps.size,
                    nextLabel = nextLabel,
                    nextPrimary = nextPrimary,
                    onBack = { go(state.index - 1) },
                    onNext = onNext
                )
            }
        }
    }
}

@Composable
internal fun rememberEntranceProgress(): Animatable<Float, AnimationVector1D> {
    val entrance = LocalSetupEntrance.current
    return remember { Animatable(if (entrance) 0f else 1f) }
}

@Composable
internal fun Modifier.rise(order: Int): Modifier {
    val progress = rememberEntranceProgress()
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(140L + order * 75L)
            progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 140f))
        }
    }
    return this.graphicsLayer {
        val v = progress.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 36.dp.toPx()
    }
}

@Composable
internal fun SetupPage(
    metrics: PageMetrics,
    scrollState: ScrollState,
    hero: @Composable (Modifier) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (metrics.wide) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                hero(Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(24.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 24.dp),
                    content = content
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            hero(
                Modifier
                    .fillMaxWidth()
                    .height(metrics.heroHeight)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun PageHeader(title: String, body: String, metrics: PageMetrics) {
    val colors = LocalClintColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = colors.onSurface,
        textAlign = metrics.align,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .rise(0)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.secondaryText,
        textAlign = metrics.align,
        modifier = Modifier
            .fillMaxWidth()
            .rise(1)
    )
}

@Composable
internal fun SectionTitle(text: String, metrics: PageMetrics, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = colors.primary,
        textAlign = metrics.align,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun SetupCaptionText(text: String, metrics: PageMetrics, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalClintColors.current.secondaryText,
        textAlign = metrics.align,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    )
}

private fun graphemes(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val parts = ArrayList<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        parts.add(text.substring(start, end))
        start = end
        end = iterator.next()
    }
    return parts
}

private fun isRtlText(text: String): Boolean = text.any {
    val d = Character.getDirectionality(it)
    d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RevealTitle(text: String, centered: Boolean) {
    val colors = LocalClintColors.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val size = when {
            maxWidth < 330.dp -> 36.sp
            maxWidth < 420.dp -> 44.sp
            else -> 54.sp
        }
        val style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = size * 1.12f)
        val semanticsModifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = text
                heading()
            }
        if (isRtlText(text)) {
            Text(
                text = text,
                style = style,
                color = colors.onSurface,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = semanticsModifier.rise(1)
            )
        } else {
            val words = remember(text) { text.split(' ').filter { it.isNotEmpty() }.map { graphemes(it) } }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FlowRow(
                    horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
                    modifier = semanticsModifier
                ) {
                    var order = 0
                    words.forEachIndexed { w, glyphs ->
                        Row {
                            glyphs.forEach { glyph ->
                                RevealGlyph(glyph, order++, style)
                            }
                            if (w < words.lastIndex) RevealGlyph(" ", order, style)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevealGlyph(glyph: String, order: Int, style: androidx.compose.ui.text.TextStyle) {
    val colors = LocalClintColors.current
    val progress = rememberEntranceProgress()
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(260L + order * 70L)
            progress.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 170f))
        }
    }
    Text(
        text = glyph,
        style = style,
        color = colors.onSurface,
        modifier = Modifier.graphicsLayer {
            val v = progress.value
            alpha = v.coerceIn(0f, 1f)
            translationY = (1f - v) * 48.dp.toPx()
        }
    )
}

@Composable
internal fun IconTile(icon: ImageVector, selected: Boolean = false, size: Dp = 42.dp) {
    val colors = LocalClintColors.current
    val container by animateColorAsState(if (selected) colors.primary else colors.primary.copy(alpha = 0.16f), label = "icon_tile_bg")
    val tint by animateColorAsState(if (selected) colors.onPrimary else colors.primary, label = "icon_tile_tint")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(container)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
internal fun FeatureRow(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBackground)
            .padding(12.dp)
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }
    }
}

@Composable
internal fun SetupToggleCard(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val container by animateColorAsState(
        if (checked) colors.primary.copy(alpha = 0.14f) else colors.cardBackground,
        label = "toggle_card_bg"
    )
    val stroke by animateColorAsState(
        if (checked) colors.primary else colors.divider,
        label = "toggle_card_stroke"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(if (checked) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 14.dp)
    ) {
        IconTile(icon, selected = checked)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }
        Spacer(Modifier.width(10.dp))
        ClintSwitch(checked = checked)
    }
}

@Composable
internal fun ChoiceCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit
) {
    val colors = LocalClintColors.current
    val container by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.14f) else colors.cardBackground,
        label = "choice_card_bg"
    )
    val stroke by animateColorAsState(
        if (selected) colors.primary else colors.divider,
        label = "choice_card_stroke"
    )
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "choice_card_lift")
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = 1f + 0.03f * lift
                scaleY = 1f + 0.03f * lift
            }
            .clip(RoundedCornerShape(20.dp))
            .border(if (selected) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp)
        ) {
            preview()
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.onSurface else colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(20.dp)
                .graphicsLayer {
                    scaleX = lift
                    scaleY = lift
                    alpha = lift.coerceIn(0f, 1f)
                }
                .clip(CircleShape)
                .background(colors.primary)
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun SetupActionButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    dimmed: Boolean = false,
    iconAtEnd: Boolean = false,
    minHeight: Dp = 48.dp
) {
    val colors = LocalClintColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "button_press")
    val container by animateColorAsState(
        when {
            primary && dimmed -> colors.primary.copy(alpha = 0.38f)
            primary -> colors.primary
            else -> colors.cardBackground
        },
        label = "button_container"
    )
    val content by animateColorAsState(if (primary) colors.onPrimary else colors.onSurface, label = "button_content")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = minHeight)
            .clip(CircleShape)
            .background(container)
            .then(if (primary) Modifier else Modifier.border(1.dp, colors.divider, CircleShape))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (icon != null && !iconAtEnd) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (icon != null && iconAtEnd) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val description = stringResource(R.string.setup_step_counter, current + 1, count)
    val dense = count > 9
    val dot = if (dense) 6.dp else 7.dp
    val active = if (dense) 18.dp else 22.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(
                if (i == current) active else dot,
                spring(dampingRatio = 0.6f, stiffness = 320f),
                label = "dot_width"
            )
            val tint by animateColorAsState(
                when {
                    i == current -> colors.primary
                    i < current -> colors.primary.copy(alpha = 0.45f)
                    else -> colors.secondaryText.copy(alpha = 0.3f)
                },
                label = "dot_color"
            )
            Box(
                Modifier
                    .height(dot)
                    .width(width)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun BackIconButton(onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val label = stringResource(R.string.back)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.cardBackground)
            .border(1.dp, colors.divider, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SetupBottomBar(
    step: SetupStep,
    index: Int,
    count: Int,
    nextLabel: String,
    nextPrimary: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val stacked = step == SetupStep.WELCOME || step == SetupStep.DONE
    val nextIcon = if (step == SetupStep.DONE) Icons.Outlined.Explore else Icons.AutoMirrored.Outlined.ArrowForward
    AnimatedContent(
        targetState = stacked,
        transitionSpec = { fadeIn(tween(300, delayMillis = 120)) togetherWith fadeOut(tween(160)) },
        modifier = Modifier.fillMaxWidth(),
        label = "setup_bar"
    ) { isStacked ->
        if (isStacked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                StepDots(count, index)
                Spacer(Modifier.height(16.dp))
                SetupActionButton(
                    label = nextLabel,
                    icon = nextIcon,
                    iconAtEnd = step != SetupStep.DONE,
                    dimmed = !nextPrimary,
                    minHeight = 56.dp,
                    onClick = onNext,
                    modifier = Modifier
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                )
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 420.dp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Box(if (compact) Modifier else Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (compact) {
                            BackIconButton(onBack)
                        } else {
                            SetupActionButton(
                                label = stringResource(R.string.back),
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                primary = false,
                                onClick = onBack
                            )
                        }
                    }
                    Box(if (compact) Modifier.weight(1f) else Modifier, contentAlignment = Alignment.Center) {
                        StepDots(count, index, Modifier.padding(horizontal = 10.dp))
                    }
                    Box(if (compact) Modifier else Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        AnimatedContent(
                            targetState = nextLabel to nextPrimary,
                            transitionSpec = {
                                (fadeIn(tween(220)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 320f), initialScale = 0.88f)) togetherWith fadeOut(tween(120))
                            },
                            contentAlignment = Alignment.CenterEnd,
                            label = "setup_next"
                        ) { (label, primary) ->
                            SetupActionButton(
                                label = label,
                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                iconAtEnd = true,
                                primary = primary,
                                onClick = onNext
                            )
                        }
                    }
                }
            }
        }
    }
}
