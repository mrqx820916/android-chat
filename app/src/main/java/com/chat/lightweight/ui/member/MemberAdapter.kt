package com.chat.lightweight.ui.member

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.chat.lightweight.R
import com.chat.lightweight.data.model.MemberItem
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import android.widget.TextView

/**
 * 成员列表适配器
 * 遵循单一职责原则，仅负责数据绑定和视图更新
 */
class MemberAdapter(
    private val onMemberClick: (MemberItem) -> Unit,
    private val onEditNote: (MemberItem) -> Unit,
    private val onDeleteMember: (MemberItem) -> Unit
) : ListAdapter<MemberItem, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view, onMemberClick, onEditNote, onDeleteMember)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 成员ViewHolder
     */
    class MemberViewHolder(
        itemView: View,
        private val onMemberClick: (MemberItem) -> Unit,
        private val onEditNote: (MemberItem) -> Unit,
        private val onDeleteMember: (MemberItem) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val avatarImageView: ShapeableImageView = itemView.findViewById(R.id.avatarImageView)
        private val avatarBackground: View = itemView.findViewById(R.id.avatarBackground)
        private val avatarText: TextView = itemView.findViewById(R.id.avatarText)
        private val displayNameTextView: TextView = itemView.findViewById(R.id.displayNameTextView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val noteTextView: TextView = itemView.findViewById(R.id.noteTextView)
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val onlineDotView: View = itemView.findViewById(R.id.onlineDotView)
        private val onlineStatusTextView: TextView = itemView.findViewById(R.id.onlineStatusTextView)
        private val messageCountTextView: TextView = itemView.findViewById(R.id.messageCountTextView)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
        private val editNoteButton: MaterialButton = itemView.findViewById(R.id.editNoteButton)
        private val deleteMemberButton: MaterialButton = itemView.findViewById(R.id.deleteMemberButton)

        fun bind(member: MemberItem) {
            // 设置点击事件（进入对话）
            cardView.setOnClickListener { onMemberClick(member) }

            // 设置显示名称（备注优先）
            displayNameTextView.text = if (member.note.isNotEmpty()) {
                member.note
            } else {
                member.username
            }

            // 设置用户名
            usernameTextView.text = "@${member.username}"

            // 设置备注显示
            if (member.note.isNotEmpty()) {
                noteTextView.visibility = View.VISIBLE
                noteTextView.text = "备注：${member.note}"
            } else {
                noteTextView.visibility = View.GONE
            }

            // 设置在线状态
            if (member.isOnline) {
                onlineIndicator.visibility = View.VISIBLE
                onlineDotView.setBackgroundResource(R.color.success)
                onlineStatusTextView.text = "在线"
            } else {
                onlineIndicator.visibility = View.GONE
                onlineDotView.setBackgroundResource(R.color.text_hint)
                onlineStatusTextView.text = "离线"
            }

            // 设置消息数
            if (member.messageCount > 0) {
                messageCountTextView.visibility = View.VISIBLE
                messageCountTextView.text = "${member.messageCount} 条消息"
            } else {
                messageCountTextView.visibility = View.GONE
            }

            // 设置最后消息
            if (member.lastMessage.isNotEmpty()) {
                lastMessageTextView.visibility = View.VISIBLE
                lastMessageTextView.text = member.lastMessage
            } else {
                lastMessageTextView.visibility = View.GONE
            }

            // 设置头像 - 使用彩色圆形背景和首字母
            val displayName = if (member.note.isNotEmpty()) member.note else member.username
            val firstChar = displayName.firstOrNull()?.toString()?.uppercase() ?: "?"

            // 设置首字母文本
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

            // 编辑备注按钮
            editNoteButton.setOnClickListener { onEditNote(member) }

            // 删除成员按钮
            deleteMemberButton.setOnClickListener { onDeleteMember(member) }
        }
    }

    /**
     * DiffUtil回调
     */
    private class MemberDiffCallback : DiffUtil.ItemCallback<MemberItem>() {
        override fun areItemsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean {
            return oldItem == newItem
        }
    }
}
