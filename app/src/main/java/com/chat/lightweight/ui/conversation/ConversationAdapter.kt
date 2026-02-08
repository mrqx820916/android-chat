package com.chat.lightweight.ui.conversation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.lightweight.R
import com.chat.lightweight.data.model.ConversationItem
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话列表适配器
 * 遵循单一职责原则，仅负责数据绑定和视图更新
 */
class ConversationAdapter(
    private val onConversationClick: (ConversationItem) -> Unit
) : ListAdapter<ConversationItem, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view, onConversationClick)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 对话ViewHolder
     */
    class ConversationViewHolder(
        itemView: View,
        private val onConversationClick: (ConversationItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val avatarImageView: ShapeableImageView = itemView.findViewById(R.id.iv_avatar)
        private val avatarBackground: View = itemView.findViewById(R.id.avatar_background)
        private val avatarText: TextView = itemView.findViewById(R.id.avatar_text)
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_username)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.tv_last_message)
        private val timeTextView: TextView = itemView.findViewById(R.id.tv_time)
        private val unreadBadgeCardView: MaterialCardView = itemView.findViewById(R.id.unread_badge)
        private val unreadCountTextView: TextView = itemView.findViewById(R.id.tv_unread_count)
        private val onlineIndicator: View = itemView.findViewById(R.id.online_indicator)

        fun bind(item: ConversationItem) {
            // 设置点击事件
            itemView.setOnClickListener { onConversationClick(item) }

            // 设置名称
            nameTextView.text = item.username

            // 设置最后消息
            lastMessageTextView.text = if (item.lastMessage.isEmpty()) {
                "暂无消息"
            } else {
                item.lastMessage
            }

            // 设置时间
            timeTextView.text = formatTime(item.lastMessageTime)

            // 设置未读数
            if (item.hasUnreadMessages()) {
                unreadBadgeCardView.visibility = View.VISIBLE
                unreadCountTextView.text = item.getUnreadCountText()
            } else {
                unreadBadgeCardView.visibility = View.GONE
            }

            // 设置在线状态（仅管理员视图显示）
            // onlineIndicator.visibility = if (item.isOnline) View.VISIBLE else View.GONE

            // 设置头像首字母
            val firstChar = item.username.firstOrNull()?.toString()?.uppercase() ?: "?"
            avatarText.text = firstChar

            // 根据首字母选择背景颜色
            val color = when (firstChar.firstOrNull()) {
                in 'A'..'I' -> 0xFF07C160.toInt() // 微信绿
                in 'J'..'R' -> 0xFF10AEFF.toInt() // 蓝色
                in 'S'..'Z' -> 0xFFFFA940.toInt() // 橙色
                else -> 0xFFFF6B6B.toInt() // 红色
            }

            // 创建圆形背景并设置颜色
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
            }
            avatarBackground.background = drawable
        }

        /**
         * 格式化时间
         */
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "刚刚" // 1分钟内
                diff < 3600000 -> "${diff / 60000}分钟前" // 1小时内
                diff < 86400000 -> {
                    // 今天
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
                diff < 172800000 -> {
                    // 昨天
                    val dateFormat = SimpleDateFormat("昨天 HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
                else -> {
                    // 更早
                    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    /**
     * DiffUtil回调
     * 用于高效更新RecyclerView
     */
    private class ConversationDiffCallback : DiffUtil.ItemCallback<ConversationItem>() {
        override fun areItemsTheSame(
            oldItem: ConversationItem,
            newItem: ConversationItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ConversationItem,
            newItem: ConversationItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
