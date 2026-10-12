package com.jhaiian.clint.backup

import com.jhaiian.clint.BuildConfig

enum class BackupCategory(val id: String) {
    SETTINGS("settings"),
    TABS("tabs"),
    DOWNLOADS("downloads"),
    WEBSITE_BLOCKER("website_blocker"),
    COOKIES("cookies"),
    PROFILES("profiles"),
    BOOKMARKS("bookmarks"),
    SEARCH_HISTORY("search_history"),
    SITE_PERMISSIONS("site_permissions"),
    SHORTCUTS("shortcuts"),
    EXTENSIONS("extensions"),
    UPDATE_SETTINGS("update_settings");

    companion object {
        fun fromId(id: String): BackupCategory? = entries.firstOrNull { it.id == id }

        /**
         * Categories that make sense to offer for backup on the current build flavor.
         * UPDATE_SETTINGS is excluded on the FOSS flavor, since that flavor has no in-app
         * update checker and therefore no update preferences to save or restore.
         */
        fun available(): List<BackupCategory> =
            entries.filter { it != UPDATE_SETTINGS || !BuildConfig.IS_FOSS }
    }
}
