package com.chat.lightweight.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chat.lightweight.data.model.EmojiCategory
import com.chat.lightweight.databinding.ItemEmojiCategoryBinding

/**
 * 表情分类适配器
 *
 * 显示表情分类Tab，支持点击选择
 */
class EmojiCategoryAdapter(
    private val onCategoryClick: (EmojiCategory) -> Unit
) : ListAdapter<EmojiCategory, EmojiCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategory: EmojiCategory? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemEmojiCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), selectedCategory)
    }

    /**
     * 设置选中的分类
     */
    fun setSelectedCategory(category: EmojiCategory?) {
        val previous = selectedCategory
        selectedCategory = category

        // 通知变化
        if (previous != null) {
            val previousPosition = currentList.indexOf(previous)
            if (previousPosition >= 0) {
                notifyItemChanged(previousPosition)
            }
        }

        if (category != null) {
            val currentPosition = currentList.indexOf(category)
            if (currentPosition >= 0) {
                notifyItemChanged(currentPosition)
            }
        }
    }

    /**
     * 分类ViewHolder
     */
    class CategoryViewHolder(
        private val binding: ItemEmojiCategoryBinding,
        private val onCategoryClick: (EmojiCategory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: EmojiCategory, selectedCategory: EmojiCategory?) {
            binding.apply {
                tvCategoryIcon.text = category.icon
                tvCategoryName.text = category.displayName

                // 设置选中状态
                val isSelected = category == selectedCategory
                root.setCardBackgroundColor(
                    if (isSelected) {
                        root.context.getColor(com.chat.lightweight.R.color.brand_primary)
                    } else {
                        root.context.getColor(com.chat.lightweight.R.color.md_theme_light_surfaceVariant)
                    }
                )

                tvCategoryName.setTextColor(
                    if (isSelected) {
                        root.context.getColor(com.chat.lightweight.R.color.md_theme_light_onPrimary)
                    } else {
                        root.context.getColor(com.chat.lightweight.R.color.text_primary)
                    }
                )

                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    /**
     * DiffUtil回调
     */
    class CategoryDiffCallback : DiffUtil.ItemCallback<EmojiCategory>() {
        override fun areItemsTheSame(oldItem: EmojiCategory, newItem: EmojiCategory): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: EmojiCategory, newItem: EmojiCategory): Boolean {
            return oldItem == newItem
        }
    }
}
