package com.jhaiian.clint.shortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.jhaiian.clint.browser.MainActivity

class ShortcutLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shortcutId = intent?.getStringExtra(MainActivity.EXTRA_SHORTCUT_ID)
        if (shortcutId != null) {
            val forward = Intent(Intent.ACTION_VIEW, intent.data).apply {
                setClass(this@ShortcutLaunchActivity, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_SHORTCUT_ID, shortcutId)
            }
            runCatching { startActivity(forward) }
        }
        finish()
    }
}
