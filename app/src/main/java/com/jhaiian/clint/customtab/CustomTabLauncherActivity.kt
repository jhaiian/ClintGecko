package com.jhaiian.clint.customtab

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.jhaiian.clint.browser.MainActivity

class CustomTabLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent
        val data = source.data
        val scheme = data?.scheme?.lowercase()
        val isCustomTab = source.action == Intent.ACTION_VIEW &&
            source.hasExtra(EXTRA_CUSTOM_TAB_SESSION) &&
            (scheme == "http" || scheme == "https")
        val target = if (isCustomTab) {
            Intent(Intent.ACTION_VIEW, data).setClass(this, CustomTabActivity::class.java)
        } else {
            Intent(source)
                .setComponent(ComponentName(this, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(target) }
        finish()
    }

    private companion object {
        const val EXTRA_CUSTOM_TAB_SESSION = "android.support.customtabs.extra.SESSION"
    }
}
