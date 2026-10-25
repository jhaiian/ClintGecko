package com.jhaiian.clint.downloads

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.R

@Composable
internal fun autoOrCountLabel(value: Int, pluralRes: Int): String =
    if (DownloadAutoTuner.isAuto(value)) stringResource(R.string.download_setting_auto)
    else pluralStringResource(pluralRes, value, value)

internal fun autoSliderPosition(value: Int, max: Int): Int =
    if (DownloadAutoTuner.isAuto(value)) max + 1 else value.coerceIn(1, max)

internal fun autoSliderValue(position: Int, max: Int): Int =
    if (position > max) DownloadAutoTuner.AUTO else position.coerceAtLeast(1)
