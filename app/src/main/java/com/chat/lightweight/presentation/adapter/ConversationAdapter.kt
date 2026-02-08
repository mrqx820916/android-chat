package com.chat.lightweight.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.lightweight.databinding.ItemConversationBinding
import com.chat.lightweight.domain.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话列表适配器
 */
class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemConversationBinding,
        private val onItemClick: (Conversation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.apply {
                // 设置用户名
                tvUsername.text = conversation.user?.username ?: "未知用户"

                // 设置最后消息
                tvLastMessage.text = conversation.lastMessage?.content ?: "暂无消息"

                // 设置时间
                tvTime.text = formatTime(conversation.lastMessage?.createdAt)

                // 设置未读数
                if (conversation.unreadCount > 0) {
                    // TODO: 设置未读徽章
                }

                // 设置头像
                // TODO: 加载用户头像

                // 点击事件
                root.setOnClickListener {
                    onItemClick(conversation)
                }
            }
        }

        private fun formatTime(timestamp: String?): String {
            if (timestamp == null) return ""

            return try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = Date(timestamp.toLongOrNull() ?: 0)
                sdf.format(date)
            } catch (e: Exception) {
                ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
