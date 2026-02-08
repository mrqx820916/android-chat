package com.chat.lightweight.presentation.ui.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 基础Adapter类
 * 提供通用的列表适配器实现
 */
abstract class BaseAdapter<T : Any, VH : RecyclerView.ViewHolder> :
    ListAdapter<T, VH>(DiffUtilCallback()) {

    class DiffUtilCallback<T : Any> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<T>?) {
        super.submitList(list?.let { ArrayList(it) })
    }
}
