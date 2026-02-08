package com.chat.lightweight.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 消息工具类
 * 提供消息相关的格式化功能
 */
object MessageUtils {

    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /**
     * 格式化消息时间
     * 与React WEB端保持一致：今天/昨天/周几/MM-DD
     */
    fun formatMessageTime(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diffDays = ((now.time - date.time) / MILLIS_PER_DAY).toInt()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormat.format(date)

        return when {
            diffDays == 0 -> timeStr  // 今天：只显示时间
            diffDays == 1 -> "昨天 $timeStr"  // 昨天
            diffDays < 7 -> {
                val calendar = Calendar.getInstance()
                calendar.time = date
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                "${weekDays[dayOfWeek - 1]} $timeStr"  // 本周内：显示周几
            }
            else -> {
                val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                "${dateFormat.format(date)} $timeStr"  // 更早：显示日期
            }
        }
    }
}
