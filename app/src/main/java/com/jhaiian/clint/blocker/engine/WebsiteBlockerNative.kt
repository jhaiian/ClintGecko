package com.jhaiian.clint.blocker.engine

object WebsiteBlockerNative {
    init {
        System.loadLibrary("websiteblocker")
    }

    external fun nativeCreateBuilder(): Long
    external fun nativeAddText(builderPtr: Long, text: String): String
    external fun nativeFinalizeBuilder(builderPtr: Long, outputPath: String): String
    external fun nativeDestroyBuilder(builderPtr: Long)

    external fun nativeLoadDatabase(path: String): Long
    external fun nativeIsBlocked(handle: Long, host: String): Boolean
    external fun nativeDomainCount(handle: Long): Long
    external fun nativeUnloadDatabase(handle: Long)
}
