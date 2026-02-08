package com.chat.lightweight.data.model

/**
 * 缓存策略配置
 */
data class CachePolicy(
    val conversationsCacheDuration: Long = 5 * 60 * 1000,  // 5分钟
    val messagesCacheDuration: Long = 10 * 60 * 1000,     // 10分钟
    val maxCacheSize: Int = 100,                           // 最大缓存条目数
    val enableConversationCache: Boolean = true,
    val enableMessageCache: Boolean = true
) {
    companion object {
        /**
         * 默认缓存策略
         */
        fun default() = CachePolicy()

        /**
         * 短期缓存策略 (1分钟)
         */
        fun shortTerm() = CachePolicy(
            conversationsCacheDuration = 60 * 1000,
            messagesCacheDuration = 60 * 1000
        )

        /**
         * 长期缓存策略 (30分钟)
         */
        fun longTerm() = CachePolicy(
            conversationsCacheDuration = 30 * 60 * 1000,
            messagesCacheDuration = 30 * 60 * 1000
        )
    }
}
