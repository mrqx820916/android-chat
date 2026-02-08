package com.chat.lightweight.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.chat.lightweight.data.model.Emoji
import com.chat.lightweight.databinding.ItemEmojiBinding

/**
 * 表情适配器
 *
 * 显示表情网格，支持点击选择
 */
class EmojiAdapter(
    private val onEmojiClick: (Emoji) -> Unit
) : ListAdapter<Emoji, EmojiAdapter.EmojiViewHolder>(EmojiDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemEmojiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmojiViewHolder(binding, onEmojiClick)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 表情ViewHolder
     */
    class EmojiViewHolder(
        private val binding: ItemEmojiBinding,
        private val onEmojiClick: (Emoji) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(emoji: Emoji) {
            binding.tvEmoji.text = emoji.emoji
            binding.tvEmoji.contentDescription = emoji.name

            binding.root.setOnClickListener {
                onEmojiClick(emoji)
            }
        }
    }

    /**
     * DiffUtil回调
     */
    class EmojiDiffCallback : DiffUtil.ItemCallback<Emoji>() {
        override fun areItemsTheSame(oldItem: Emoji, newItem: Emoji): Boolean {
            return oldItem.emoji == newItem.emoji
        }

        override fun areContentsTheSame(oldItem: Emoji, newItem: Emoji): Boolean {
            return oldItem == newItem
        }
    }
}
