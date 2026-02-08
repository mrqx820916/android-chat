package com.chat.lightweight.data.model

/**
 * 表情数据模型
 */
data class Emoji(
    val emoji: String,
    val name: String,
    val category: EmojiCategory,
    val keywords: List<String>
)

/**
 * 表情分类
 */
enum class EmojiCategory(val icon: String, val displayName: String) {
    SMILEYS("😀", "表情"),      // 表情
    PEOPLE("👤", "人物"),       // 人物
    ANIMALS("🐶", "动物"),      // 动物
    FOOD("🍔", "食物"),         // 食物
    ACTIVITIES("⚽", "活动"),   // 活动
    TRAVEL("✈️", "旅行"),       // 旅行
    OBJECTS("📦", "物品"),      // 物品
    SYMBOLS("💬", "符号"),      // 符号
    RECENT("🕒", "最近");       // 最近使用

    companion object {
        /**
         * 获取所有分类（不包括最近使用）
         */
        fun getAllCategories(): List<EmojiCategory> {
            return entries.filter { it != RECENT }
        }
    }
}

/**
 * 表情数据管理
 */
object EmojiData {

    // 常用表情（简化版，避免编码问题）
    val commonEmojis = listOf(
        // 表情
        Emoji("😀", "笑脸", EmojiCategory.SMILEYS, listOf("笑脸", "happy")),
        Emoji("😂", "笑哭", EmojiCategory.SMILEYS, listOf("笑哭", "joy")),
        Emoji("🥰", "爱心眼", EmojiCategory.SMILEYS, listOf("爱心眼", "love")),
        Emoji("😍", "爱心", EmojiCategory.SMILEYS, listOf("爱心", "heart")),
        Emoji("🤔", "思考", EmojiCategory.SMILEYS, listOf("思考", "thinking")),
        Emoji("😎�", "庆祝", EmojiCategory.SMILEYS, listOf("庆祝", "celebration")),

        // 人物
        Emoji("👍", "赞", EmojiCategory.PEOPLE, listOf("赞", "thumbsup")),
        Emoji("👎�", "礼物", EmojiCategory.PEOPLE, listOf("礼物", "gift")),
        Emoji("👋", "挥手", EmojiCategory.PEOPLE, listOf("挥手", "wave")),
        Emoji("🙏", "祈祷", EmojiCategory.PEOPLE, listOf("祈祷", "pray")),

        // 动物
        Emoji("🐶", "猫", EmojiCategory.ANIMALS, listOf("猫", "cat")),
        Emoji("🐕", "狗", EmojiCategory.ANIMALS, listOf("狗", "dog")),
        Emoji("🦊", "狐狸", EmojiCategory.ANIMALS, listOf("狐狸", "fox")),

        // 食物
        Emoji("🍎�", "水果", EmojiCategory.FOOD, listOf("水果", "fruit")),
        Emoji("🍕", "披萨", EmojiCategory.FOOD, listOf("披萨", "pizza")),
        Emoji("☕", "咖啡", EmojiCategory.FOOD, listOf("咖啡", "coffee")),

        // 活动
        Emoji("⚽", "运动", EmojiCategory.ACTIVITIES, listOf("运动", "sports")),
        Emoji("🎮", "游戏", EmojiCategory.ACTIVITIES, listOf("游戏", "game")),
        Emoji("🎵", "音乐", EmojiCategory.ACTIVITIES, listOf("音乐", "music")),

        // 符号
        Emoji("❤️", "红心", EmojiCategory.SYMBOLS, listOf("红心", "heart")),
        Emoji("⭐", "星星", EmojiCategory.SYMBOLS, listOf("星星", "star")),
        Emoji("🔥", "火焰", EmojiCategory.SYMBOLS, listOf("火焰", "fire"))
    )

    /**
     * 根据分类获取表情
     */
    fun getEmojisByCategory(category: EmojiCategory): List<Emoji> {
        return if (category == EmojiCategory.RECENT) {
            commonEmojis.filter { it.isRecent }
        } else {
            commonEmojis.filter { it.category == category }
        }
    }

    /**
     * 搜索表情
     */
    fun searchEmojis(query: String): List<Emoji> {
        val lowerQuery = query.lowercase()
        return commonEmojis.filter { emoji ->
            emoji.name.contains(lowerQuery) ||
            emoji.keywords.any { it.contains(lowerQuery) }
        }
    }

    /**
     * 标记为最近使用
     */
    fun markAsRecent(emoji: Emoji) {
        // 在实际实现中，这会更新缓存
    }
}

/**
 * Emoji扩展属性
 */
val Emoji.isRecent: Boolean
    get() = false // 实际实现应从缓存读取
