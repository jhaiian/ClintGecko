package com.jhaiian.clint.blocker.engine

import android.content.Context
import java.io.File

object WebsiteBlockerPaths {
    private fun rootDir(context: Context): File =
        File(context.filesDir, "websiteblocker").apply { mkdirs() }

    fun categoriesDir(context: Context): File =
        File(rootDir(context), "categories").apply { mkdirs() }

    fun categoryFile(context: Context, categoryId: String): File =
        File(categoriesDir(context), "$categoryId.hosts")

    fun additionalWebsitesFile(context: Context): File =
        File(rootDir(context), "additional_websites.txt")

    fun engineFile(context: Context): File =
        File(rootDir(context), "engine.dat")

    fun engineTempFile(context: Context): File =
        File(rootDir(context), "engine.tmp")

    fun manifestFile(context: Context): File =
        File(rootDir(context), "manifest.json")
}
