package com.chat.lightweight.data.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.Emoji
import com.chat.lightweight.data.model.EmojiData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

// DataStore扩展属性
private val Context.emojiCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "emoji_cache")

/**
 * 表情缓存管理器
 *
 * 管理最近使用的表情，最多保存20个
 */
class EmojiCacheManager(
    private val context: Context
) {

    companion object {
        private const val MAX_RECENT_EMOJIS = 20
        private val KEY_RECENT_EMOJIS = stringPreferencesKey("recent_emojis")

        @Volatile
        private var instance: EmojiCacheManager? = null

        fun getInstance(context: Context): EmojiCacheManager {
            return instance ?: synchronized(this) {
                instance ?: EmojiCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 获取最近使用的表情
     */
    suspend fun getRecentEmojis(): List<Emoji> {
        val recentJson = context.emojiCacheDataStore.data.map { preferences ->
            preferences[KEY_RECENT_EMOJIS] ?: ""
        }.first()

        return if (recentJson.isEmpty()) {
            emptyList()
        } else {
            try {
                val jsonArray = JSONArray(recentJson)
                val emojiStrings = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    emojiStrings.add(jsonArray.getString(i))
                }
                emojiStrings.mapNotNull { emojiStr ->
                    EmojiData.commonEmojis.find { it.emoji == emojiStr }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 添加最近使用的表情
     */
    suspend fun addRecentEmoji(emoji: Emoji) {
        val recentEmojis = getRecentEmojis().toMutableList()

        // 移除已存在的表情
        recentEmojis.removeAll { it.emoji == emoji.emoji }

        // 添加到开头
        recentEmojis.add(0, emoji)

        // 限制数量
        if (recentEmojis.size > MAX_RECENT_EMOJIS) {
            recentEmojis.removeAt(recentEmojis.size - 1)
        }

        // 保存
        saveRecentEmojis(recentEmojis)
    }

    /**
     * 清除最近使用的表情
     */
    suspend fun clearRecentEmojis() {
        context.emojiCacheDataStore.edit { preferences ->
            preferences.remove(KEY_RECENT_EMOJIS)
        }
    }

    /**
     * 保存最近使用的表情
     */
    private suspend fun saveRecentEmojis(emojis: List<Emoji>) {
        val jsonArray = JSONArray()
        emojis.forEach { jsonArray.put(it.emoji) }
        context.emojiCacheDataStore.edit { preferences ->
            preferences[KEY_RECENT_EMOJIS] = jsonArray.toString()
        }
    }

    /**
     * 获取最近使用的表情Flow
     */
    fun getRecentEmojisFlow(): Flow<List<Emoji>> {
        return context.emojiCacheDataStore.data.map { preferences ->
            val recentJson = preferences[KEY_RECENT_EMOJIS] ?: ""
            if (recentJson.isEmpty()) {
                emptyList()
            } else {
                try {
                    val jsonArray = JSONArray(recentJson)
                    val emojiStrings = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        emojiStrings.add(jsonArray.getString(i))
                    }
                    emojiStrings.mapNotNull { emojiStr ->
                        EmojiData.commonEmojis.find { it.emoji == emojiStr }
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }
}
