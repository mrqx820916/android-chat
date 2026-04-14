package com.chat.lightweight.update

object UpdateConfig {
    const val MANIFEST_URL = "https://cn-nb1.rains3.com/xzd/chat/release/latest.json"
    const val CHANNEL = "release"
    const val manifestTimeoutMs = 5_000
    const val downloadTimeoutMs = 120_000
    const val failureCooldownMs = 6 * 60 * 60 * 1000L // 6小时
    const val updatesDirName = "updates"
}
