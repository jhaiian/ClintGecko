package com.jhaiian.clint.settings.lookandfeel

import androidx.annotation.StringRes
import com.jhaiian.clint.R
import com.jhaiian.clint.util.LocaleHelper

data class LanguageOption(val tag: String, @StringRes val nameRes: Int)

val languageOptions = listOf(
    LanguageOption(LocaleHelper.LANGUAGE_ENGLISH, R.string.language_name_english),
    LanguageOption(LocaleHelper.LANGUAGE_FILIPINO, R.string.language_name_filipino),
    LanguageOption(LocaleHelper.LANGUAGE_RUSSIAN, R.string.language_name_russian),
    LanguageOption(LocaleHelper.LANGUAGE_SPANISH, R.string.language_name_spanish)
)
